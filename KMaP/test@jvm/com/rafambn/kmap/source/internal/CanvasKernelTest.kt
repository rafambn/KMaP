package com.rafambn.kmap.source.internal

import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.rafambn.kmap.MapState
import com.rafambn.kmap.components.parameters.CanvasParameters
import com.rafambn.kmap.components.parameters.RasterCanvasParameters
import com.rafambn.kmap.components.parameters.VectorCanvasParameters
import com.rafambn.kmap.geometry.plane.Coordinates
import com.rafambn.kmap.geometry.plane.ProjectedCoordinates
import com.rafambn.kmap.geometry.plane.TilePoint
import com.rafambn.kmap.mapProperties.MapProperties
import com.rafambn.kmap.mapProperties.TileDimension
import com.rafambn.kmap.mapProperties.ZoomLevelRange
import com.rafambn.kmap.mapProperties.border.BoundaryMode
import com.rafambn.kmap.mapProperties.border.MapBoundaryBehavior
import com.rafambn.kmap.mapProperties.border.TileRepeatMode
import com.rafambn.kmap.mapProperties.coordinates.CoordinatesRange
import com.rafambn.kmap.mapProperties.coordinates.Latitude
import com.rafambn.kmap.mapProperties.coordinates.Longitude
import com.rafambn.kmap.source.TileSpecs
import com.rafambn.kmap.source.RasterTile
import com.rafambn.kmap.source.TileResult
import com.rafambn.kmap.style.compiled.CompiledStyle
import de.infix.testBalloon.framework.core.testSuite
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

val CanvasKernelTest by testSuite {
    for (vector in listOf(false, true)) {
        test("removing a canvas cancels its requests and preserves its sibling, vector=$vector") {
            withContext(Dispatchers.Default) {
                withTimeout(5_000) {
                    val parent = Job()
                    val state = MapState(kernelMapProperties(), coroutineScope = CoroutineScope(parent + Dispatchers.Default))
                    val removedStarted = CompletableDeferred<Unit>()
                    val removedCancelled = CompletableDeferred<Unit>()
                    val retainedStarted = CompletableDeferred<Unit>()
                    val retainedCancelled = CompletableDeferred<Unit>()
                    val removed = waitingCanvas(1, vector, removedStarted, removedCancelled)
                    val retained = waitingCanvas(2, false, retainedStarted, retainedCancelled)
                    val kernel = state.canvasKernel
                    try {
                        state.setViewportSize(IntSize(32, 32))
                        kernel.refreshCanvas(listOf(removed))
                        removedStarted.await()
                        val removedJob = parent.children.single()
                        kernel.refreshCanvas(listOf(removed, retained))
                        retainedStarted.await()
                        val retainedJob = parent.children.single { it !== removedJob }

                        kernel.refreshCanvas(listOf(removed, retained))
                        assertEquals(setOf(removedJob, retainedJob), parent.children.toSet())
                        kernel.refreshCanvas(listOf(retained))

                        assertTrue(removedJob.isCancelled)
                        removedJob.join()
                        removedCancelled.await()
                        assertTrue(removedJob.children.none())
                        assertFailsWith<NoSuchElementException> { kernel.getActiveTiles(1) }
                        assertTrue(kernel.getActiveTiles(2).tiles.isEmpty())
                        assertSame(retainedJob, parent.children.single())
                        assertTrue(retainedJob.isActive)
                        assertFalse(retainedCancelled.isCompleted)
                        assertTrue(parent.isActive)

                        kernel.refreshCanvas(emptyList())
                        retainedCancelled.await()
                        retainedJob.join()
                        assertTrue(parent.children.none())
                        assertTrue(parent.isActive)
                    } finally {
                        parent.cancelAndJoin()
                    }
                }
            }
        }

        test("a removed canvas ID can start fresh requests, vector=$vector") {
            withContext(Dispatchers.Default) {
                withTimeout(5_000) {
                    val parent = Job()
                    val state = MapState(kernelMapProperties(), coroutineScope = CoroutineScope(parent + Dispatchers.Default))
                    val firstStarted = CompletableDeferred<Unit>()
                    val firstCancelled = CompletableDeferred<Unit>()
                    val secondStarted = CompletableDeferred<Unit>()
                    val secondCancelled = CompletableDeferred<Unit>()
                    val kernel = state.canvasKernel
                    try {
                        state.setViewportSize(IntSize(32, 32))
                        kernel.refreshCanvas(listOf(waitingCanvas(1, vector, firstStarted, firstCancelled)))
                        firstStarted.await()
                        val firstJob = parent.children.single()

                        kernel.refreshCanvas(emptyList())
                        assertTrue(firstJob.isCancelled)
                        firstJob.join()
                        firstCancelled.await()
                        kernel.refreshCanvas(listOf(waitingCanvas(1, vector, secondStarted, secondCancelled)))
                        secondStarted.await()

                        assertNotSame(firstJob, parent.children.single())
                        assertFalse(secondCancelled.isCompleted)
                        assertTrue(parent.isActive)
                    } finally {
                        parent.cancelAndJoin()
                    }
                }
            }
        }
    }

    test("cancelling the map scope cancels requests from every canvas") {
        withContext(Dispatchers.Default) {
            withTimeout(5_000) {
                val parent = Job()
                val state = MapState(kernelMapProperties(), coroutineScope = CoroutineScope(parent + Dispatchers.Default))
                val rasterStarted = CompletableDeferred<Unit>()
                val rasterCancelled = CompletableDeferred<Unit>()
                val vectorStarted = CompletableDeferred<Unit>()
                val vectorCancelled = CompletableDeferred<Unit>()
                try {
                    state.setViewportSize(IntSize(32, 32))
                    state.canvasKernel.refreshCanvas(listOf(
                        waitingCanvas(1, false, rasterStarted, rasterCancelled),
                        waitingCanvas(2, true, vectorStarted, vectorCancelled),
                    ))
                    rasterStarted.await()
                    vectorStarted.await()

                    parent.cancelAndJoin()

                    assertTrue(rasterCancelled.isCompleted)
                    assertTrue(vectorCancelled.isCompleted)
                    assertTrue(parent.children.none())
                } finally {
                    parent.cancelAndJoin()
                }
            }
        }
    }

    test("canvases wait for the first selection without a MapState") {
        val parent = Job()
        val kernel = CanvasKernel(CoroutineScope(parent + Dispatchers.Default))
        val requests = Channel<TileSpecs>(Channel.UNLIMITED)
        try {
            kernel.refreshCanvas(listOf(RasterCanvasParameters(1, tileSource = { z, r, c ->
                requests.send(TileSpecs(z, r, c))
                TileResult.Success(RasterTile(z, r, c, null))
            })))
            assertTrue(kernel.getActiveTiles(1).tiles.isEmpty())

            kernel.resolveVisibleTiles(TilePoint.Zero, TilePoint(128.0, 64.0), 1, kernelMapProperties())

            kernel.awaitActiveTiles(1, 1, listOf(TileSpecs(1, 0, 0)))
            assertEquals(TileSpecs(1, 0, 0), requests.tryReceive().getOrThrow())
            assertTrue(requests.tryReceive().isFailure)
        } finally {
            parent.cancelAndJoin()
        }
    }

    test("new canvases receive the latest selection resolved before they existed") {
        val parent = Job()
        val kernel = CanvasKernel(CoroutineScope(parent + Dispatchers.Default))
        val properties = kernelMapProperties()
        val requests = Channel<TileSpecs>(Channel.UNLIMITED)
        try {
            kernel.resolveVisibleTiles(TilePoint.Zero, TilePoint(128.0, 64.0), 1, properties)
            kernel.resolveVisibleTiles(TilePoint(128.0, 64.0), TilePoint(192.0, 96.0), 2, properties)
            kernel.refreshCanvas(listOf(RasterCanvasParameters(1, tileSource = { z, r, c ->
                requests.send(TileSpecs(z, r, c))
                TileResult.Success(RasterTile(z, r, c, null))
            })))

            kernel.awaitActiveTiles(1, 2, listOf(TileSpecs(2, 2, 2)))
            assertEquals(TileSpecs(2, 2, 2), requests.tryReceive().getOrThrow())
            assertTrue(requests.tryReceive().isFailure)
        } finally {
            parent.cancelAndJoin()
        }
    }

    test("an empty selection replaces the previous tiles and retains its zoom for new canvases") {
        val parent = Job()
        val kernel = CanvasKernel(CoroutineScope(parent + Dispatchers.Default))
        val properties = kernelMapProperties()
        val parameters = RasterCanvasParameters(1, tileSource = { z, r, c -> TileResult.Success(RasterTile(z, r, c, null)) })
        try {
            kernel.resolveVisibleTiles(TilePoint.Zero, TilePoint(128.0, 64.0), 1, properties)
            kernel.refreshCanvas(listOf(parameters))
            kernel.awaitActiveTiles(1, 1, listOf(TileSpecs(1, 0, 0)))
            val job = parent.children.single()
            kernel.refreshCanvas(emptyList())
            job.join()

            kernel.resolveVisibleTiles(TilePoint.Zero, TilePoint.Zero, 5, properties)
            kernel.refreshCanvas(listOf(parameters))

            assertEquals(ActiveTiles(currentZoom = 5), kernel.awaitActiveTiles(1, 5, emptyList()))
        } finally {
            parent.cancelAndJoin()
        }
    }

    test("a canvas removed before the first selection releases its job") {
        val parent = Job()
        val kernel = CanvasKernel(CoroutineScope(parent + Dispatchers.Default))
        try {
            kernel.refreshCanvas(listOf(RasterCanvasParameters(1, tileSource = { _, _, _ -> error("No tiles requested") })))
            val job = parent.children.single()

            kernel.refreshCanvas(emptyList())
            job.join()

            assertTrue(job.isCancelled)
            assertTrue(parent.children.none())
            assertTrue(parent.isActive)
            assertFailsWith<NoSuchElementException> { kernel.getActiveTiles(1) }
        } finally {
            parent.cancelAndJoin()
        }
    }

    for (repeatMode in TileRepeatMode.entries) {
        for (zoom in listOf(0, 1, 5, 30)) {
            test("exact tile bounds select only that tile at zoom $zoom with $repeatMode") {
                val count = (1L shl zoom).toDouble()
                assertEquals(
                    listOf(TileSpecs(zoom, 0, 0)),
                    visibleTiles(TilePoint.Zero, TilePoint(256.0 / count, 128.0 / count), zoom, repeatMode),
                )
            }
        }

        test("partial tiles on both sides of an edge remain visible with $repeatMode") {
            assertEquals(
                listOf(TileSpecs(1, 0, 0), TileSpecs(1, 1, 0), TileSpecs(1, 0, 1), TileSpecs(1, 1, 1)),
                visibleTiles(TilePoint(127.5, 63.5), TilePoint(128.5, 64.5), repeatMode = repeatMode),
            )
        }

        for ((name, bounds) in listOf(
            "zero width" to (TilePoint(10.0, 10.0) to TilePoint(10.0, 20.0)),
            "zero height" to (TilePoint(10.0, 10.0) to TilePoint(20.0, 10.0)),
            "point" to (TilePoint(10.0, 10.0) to TilePoint(10.0, 10.0)),
            "reversed width" to (TilePoint(20.0, 10.0) to TilePoint(10.0, 20.0)),
            "reversed height" to (TilePoint(10.0, 20.0) to TilePoint(20.0, 10.0)),
        )) {
            test("$name selects no tiles with $repeatMode") {
                assertTrue(visibleTiles(bounds.first, bounds.second, repeatMode = repeatMode).isEmpty())
            }
        }
    }

    test("negative repeated tile bounds exclude their right and bottom neighbors") {
        assertEquals(
            listOf(TileSpecs(1, -1, -1)),
            visibleTiles(TilePoint(-128.0, -64.0), TilePoint.Zero, repeatMode = TileRepeatMode.REPEAT),
        )
    }

    test("repeated copies retain their unwrapped indices") {
        assertEquals(
            listOf(TileSpecs(1, 2, 2)),
            visibleTiles(TilePoint(256.0, 128.0), TilePoint(384.0, 192.0), repeatMode = TileRepeatMode.REPEAT),
        )
    }

    test("crossing the repeated map origin selects only intersecting tiles") {
        assertEquals(
            listOf(TileSpecs(1, -1, -1), TileSpecs(1, 0, -1), TileSpecs(1, -1, 0), TileSpecs(1, 0, 0)),
            visibleTiles(TilePoint(-128.0, -64.0), TilePoint(128.0, 64.0), repeatMode = TileRepeatMode.REPEAT),
        )
    }

    test("a viewport touching the outside of the map selects no tiles without repetition") {
        for ((topLeft, bottomRight) in listOf(
            TilePoint(-128.0, 0.0) to TilePoint(0.0, 64.0),
            TilePoint(0.0, -64.0) to TilePoint(128.0, 0.0),
            TilePoint(256.0, 0.0) to TilePoint(384.0, 64.0),
            TilePoint(0.0, 128.0) to TilePoint(128.0, 192.0),
        )) {
            assertTrue(visibleTiles(topLeft, bottomRight).isEmpty())
        }
    }

    test("partially outside viewports clip to the map without repetition") {
        assertEquals(
            listOf(TileSpecs(1, 0, 0)),
            visibleTiles(TilePoint(-128.0, -64.0), TilePoint(128.0, 64.0)),
        )
    }

    for (index in listOf(Int.MIN_VALUE, Int.MAX_VALUE)) {
        test("exact repeated tile bounds preserve Int boundary $index at zoom 30") {
            val width = 256.0 / (1L shl 30)
            val height = 128.0 / (1L shl 30)
            assertEquals(
                listOf(TileSpecs(30, index, index)),
                visibleTiles(
                    TilePoint(index * width, index * height),
                    TilePoint((index.toLong() + 1) * width, (index.toLong() + 1) * height),
                    zoom = 30,
                    repeatMode = TileRepeatMode.REPEAT,
                ),
            )
        }
    }
}

private fun waitingCanvas(
    id: Int,
    vector: Boolean,
    started: CompletableDeferred<Unit>,
    cancelled: CompletableDeferred<Unit>,
): CanvasParameters {
    val source: suspend (Int, Int, Int) -> Nothing = { _, _, _ ->
        started.complete(Unit)
        try {
            awaitCancellation()
        } finally {
            cancelled.complete(Unit)
        }
    }
    return if (vector) {
        VectorCanvasParameters(id, tileSource = source, style = CompiledStyle(emptyList()))
    } else {
        RasterCanvasParameters(id, tileSource = source)
    }
}

private fun visibleTiles(
    topLeft: TilePoint,
    bottomRight: TilePoint,
    zoom: Int = 1,
    repeatMode: TileRepeatMode = TileRepeatMode.NONE,
): List<TileSpecs> {
    return getVisibleTilesForLevel(topLeft, bottomRight, zoom, repeatMode, kernelMapProperties().tileSize)
}

private fun kernelMapProperties(repeatMode: TileRepeatMode = TileRepeatMode.NONE) = object : MapProperties {
    override val boundaryBehavior = MapBoundaryBehavior(BoundaryMode.CLAMP, BoundaryMode.CLAMP)
    override val tileRepeatMode = repeatMode
    override val zoomLevels = ZoomLevelRange(0, 30)
    override val tileSize = TileDimension(width = 256.dp, height = 128.dp)
    override val coordinatesRange = object : CoordinatesRange {
        override val latitude = Latitude(north = 0.0, south = 128.0)
        override val longitude = Longitude(west = 0.0, east = 256.0)
    }

    override fun toProjectedCoordinates(coordinates: Coordinates) = ProjectedCoordinates(coordinates.x, coordinates.y)
    override fun toCoordinates(projectedCoordinates: ProjectedCoordinates) = Coordinates(projectedCoordinates.x, projectedCoordinates.y)
}
