package com.rafambn.kmap

import androidx.compose.animation.core.tween
import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composer
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ControlledComposition
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.key
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.rafambn.kmap.camera.CameraState
import com.rafambn.kmap.components.parameters.RasterCanvasParameters
import com.rafambn.kmap.geometry.angle.Degrees
import com.rafambn.kmap.geometry.plane.Coordinates
import com.rafambn.kmap.geometry.plane.DifferentialScreenOffset
import com.rafambn.kmap.geometry.plane.ProjectedCoordinates
import com.rafambn.kmap.geometry.plane.ScreenOffset
import com.rafambn.kmap.geometry.plane.TilePoint
import com.rafambn.kmap.geometry.plane.toScreenOffset
import com.rafambn.kmap.mapProperties.MapProperties
import com.rafambn.kmap.mapProperties.TileDimension
import com.rafambn.kmap.mapProperties.ZoomLevelRange
import com.rafambn.kmap.mapProperties.border.BoundaryMode
import com.rafambn.kmap.mapProperties.border.MapBoundaryBehavior
import com.rafambn.kmap.mapProperties.border.TileRepeatMode
import com.rafambn.kmap.mapProperties.coordinates.CoordinatesRange
import com.rafambn.kmap.mapProperties.coordinates.Latitude
import com.rafambn.kmap.mapProperties.coordinates.Longitude
import com.rafambn.kmap.source.RasterTile
import com.rafambn.kmap.source.TileResult
import com.rafambn.kmap.source.TileSpecs
import com.rafambn.kmap.source.internal.CanvasEngine
import com.rafambn.kmap.source.internal.TileRenderer
import com.rafambn.kmap.source.internal.ActiveTiles
import com.rafambn.kmap.source.internal.awaitActiveTiles
import de.infix.testBalloon.framework.core.testSuite
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import kotlin.time.Duration.Companion.milliseconds
import java.util.concurrent.ConcurrentLinkedQueue

private fun mapState(
    mapProperties: MapProperties = mapProperties(),
    coroutineScope: CoroutineScope = CoroutineScope(EmptyCoroutineContext),
    density: Density = Density(1F),
    zoomLevelPreference: ZoomLevelRange? = null,
    initialCameraState: CameraState? = null,
) = MapState(
    mapProperties = mapProperties,
    coroutineScope = coroutineScope,
    density = density,
    zoomLevelPreference = zoomLevelPreference,
    initialCameraState = initialCameraState,
)

private fun cameraState(
    zoom: Float = 0F,
    angle: Degrees = Degrees.Zero,
    tilePoint: TilePoint = TilePoint(256.0, 256.0),
) = CameraState(
    zoom = zoom,
    angleDegrees = angle,
    tilePoint = tilePoint,
)

private fun mapProperties() = object : MapProperties {
    override val boundaryBehavior = MapBoundaryBehavior(BoundaryMode.CLAMP, BoundaryMode.CLAMP)
    override val tileRepeatMode = TileRepeatMode.NONE
    override val zoomLevels = ZoomLevelRange(0, 30)
    override val coordinatesRange = object : CoordinatesRange {
        override val latitude = Latitude(north = 90.0, south = -90.0)
        override val longitude = Longitude(west = -180.0, east = 180.0)
    }
    override val tileSize = TileDimension(512.dp, 512.dp)

    override fun toProjectedCoordinates(coordinates: Coordinates) =
        ProjectedCoordinates(coordinates.x, coordinates.y)

    override fun toCoordinates(projectedCoordinates: ProjectedCoordinates) =
        Coordinates(projectedCoordinates.x, projectedCoordinates.y)
}

private class NoOpApplier : AbstractApplier<Unit>(Unit) {
    override fun insertTopDown(index: Int, instance: Unit) = Unit
    override fun insertBottomUp(index: Int, instance: Unit) = Unit
    override fun remove(index: Int, count: Int) = Unit
    override fun move(from: Int, to: Int, count: Int) = Unit
    override fun onClear() = Unit
}

private suspend fun compose(content: @Composable () -> Unit) = coroutineScope {
    val recomposer = Recomposer(coroutineContext)
    val composition = Composition(NoOpApplier(), recomposer)
    val recomposerJob = launch { recomposer.runRecomposeAndApplyChanges() }
    try {
        var composed = false
        composition.setContent {
            content()
            composed = true
        }
        withTimeout(5000.milliseconds) {
            while (!composed) yield()
        }
    } finally {
        composition.dispose()
        recomposer.close()
        recomposerJob.cancelAndJoin()
    }
}

val MapStateTest by testSuite {
    test("repeated world indices outside the Int range are rejected") {
        val properties = object : MapProperties by mapProperties() {
            override val tileRepeatMode = TileRepeatMode.REPEAT
        }
        val state = mapState(mapProperties = properties)
        val tileSpan = 512.0 / (1L shl 30)
        for (point in listOf(
            TilePoint(1024.0, 0.0), TilePoint(-1025.0, 0.0),
            TilePoint(0.0, 1024.0), TilePoint(0.0, -1025.0)
        )) {
            assertFailsWith<IllegalArgumentException> {
                state.canvasKernel.resolveVisibleTiles(point, point + TilePoint(tileSpan, tileSpan), 30, properties)
            }
        }
        val lastColumn = 1024.0 - tileSpan
        state.canvasKernel.resolveVisibleTiles(
            TilePoint(lastColumn, 0.0), TilePoint(1024.0, tileSpan), 30, properties,
        )
    }

    test("initial zoom 31 is rejected") {
        assertFailsWith<IllegalArgumentException> {
            mapState(initialCameraState = cameraState(zoom = 31F))
        }
        assertFailsWith<IllegalArgumentException> { TileSpecs(31, 0, 0) }
    }

    test("renderer wraps zoom 30 copies before calling the Int tile source") {
        val job = Job()
        val renderer = TileRenderer(
            CoroutineScope(job + Dispatchers.Default),
            { zoom, row, col ->
                TileResult.Success(RasterTile(zoom, row, col, null))
            },
            { it },
        )
        try {
            val tile = withContext(Dispatchers.Default) {
                renderer.tilesToProcessChannel.send(listOf(TileSpecs(30, -1, 1073741824)))
                withTimeout(5000.milliseconds) { renderer.tilesProcessedChannel.receive() }
            }
            assertEquals(30, tile.zoom)
            assertEquals(1073741823, tile.row)
            assertEquals(0, tile.col)
        } finally {
            job.cancel()
        }
    }

    test("rejects unsupported map zoom ranges even with a narrow preference") {
        for (range in listOf(
            ZoomLevelRange(-1, 30),
            ZoomLevelRange(0, 31),
            ZoomLevelRange(0, 32),
            ZoomLevelRange(8, 3)
        )) {
            val properties = object : MapProperties by mapProperties() {
                override val zoomLevels = range
            }
            assertFailsWith<IllegalArgumentException> {
                mapState(mapProperties = properties, zoomLevelPreference = ZoomLevelRange(3, 3))
            }
        }
    }

    test("rejects a map zoom range above the supported maximum") {
        val properties = object : MapProperties by mapProperties() {
            override val zoomLevels = ZoomLevelRange(31, 31)
        }

        assertFailsWith<IllegalArgumentException> {
            mapState(mapProperties = properties, zoomLevelPreference = ZoomLevelRange(31, 31))
        }
    }

    test("maximum zoom retains viewport precision and clips map edges") {
        withContext(Dispatchers.Default.limitedParallelism(1)) {
            val parent = Job()
            val state = mapState(coroutineScope = CoroutineScope(coroutineContext + parent))
            try {
                state.updateCamera(zoom = 30F)
                state.canvasKernel.refreshCanvas(listOf(RasterCanvasParameters(1, tileSource = { z, r, c ->
                    TileResult.Success(RasterTile(z, r, c, null))
                })))
                state.setViewportSize(IntSize(512, 512))
                val middle = 536870912
                state.canvasKernel.awaitActiveTiles(1, 30, listOf(
                    TileSpecs(30, middle - 1, middle - 1), TileSpecs(30, middle, middle - 1),
                    TileSpecs(30, middle - 1, middle), TileSpecs(30, middle, middle)
                ))
                state.updateCamera(tilePoint = TilePoint(512.0, 512.0))
                state.canvasKernel.awaitActiveTiles(1, 30, listOf(TileSpecs(30, 1073741823, 1073741823)))
                state.updateCamera(tilePoint = TilePoint.Zero)
                state.canvasKernel.awaitActiveTiles(1, 30, listOf(TileSpecs(30, 0, 0)))
            } finally {
                parent.cancelAndJoin()
            }
        }
    }

    test("maximum zoom keeps repeated world indices within the Int range") {
        withContext(Dispatchers.Default.limitedParallelism(1)) {
            val parent = Job()
            val properties = object : MapProperties by mapProperties() {
                override val tileRepeatMode = TileRepeatMode.REPEAT
            }
            val state = mapState(mapProperties = properties, coroutineScope = CoroutineScope(coroutineContext + parent))
            try {
                state.updateCamera(zoom = 30F, tilePoint = TilePoint(512.0, 512.0))
                state.canvasKernel.refreshCanvas(listOf(RasterCanvasParameters(1, tileSource = { z, r, c ->
                    TileResult.Success(RasterTile(z, r, c, null))
                })))
                state.setViewportSize(IntSize(512, 512))
                val edge = 1073741824
                state.canvasKernel.awaitActiveTiles(1, 30, listOf(
                    TileSpecs(30, edge - 1, edge - 1), TileSpecs(30, edge, edge - 1),
                    TileSpecs(30, edge - 1, edge), TileSpecs(30, edge, edge)
                ))
                state.updateCamera(tilePoint = TilePoint.Zero)
                state.canvasKernel.awaitActiveTiles(1, 30, listOf(
                    TileSpecs(30, -1, -1), TileSpecs(30, 0, -1), TileSpecs(30, -1, 0), TileSpecs(30, 0, 0)
                ))
            } finally {
                parent.cancelAndJoin()
            }
        }
    }

    test("animated pan, zoom, and rotation do not repeat unchanged tile requests") {
        withContext(Dispatchers.Default.limitedParallelism(1)) {
            val parent = Job()
            val mapState = mapState(coroutineScope = CoroutineScope(coroutineContext + parent))
            val requests = ConcurrentLinkedQueue<TileSpecs>()
            try {
                mapState.updateCamera(zoom = 2F)
                mapState.canvasKernel.refreshCanvas(listOf(RasterCanvasParameters(1, tileSource = { z, r, c ->
                    requests.add(TileSpecs(z, r, c))
                    TileResult.Success(RasterTile(z, r, c, null))
                })))
                mapState.setViewportSize(IntSize(64, 64))
                val expected = listOf(TileSpecs(2, 1, 1), TileSpecs(2, 2, 1), TileSpecs(2, 1, 2), TileSpecs(2, 2, 2))
                mapState.canvasKernel.awaitActiveTiles(1, 2, expected)
                var frames = 0
                val clock = object : MonotonicFrameClock {
                    override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R = onFrame(++frames * 16_000_000L)
                }

                withContext(clock) {
                    mapState.motionController.animate {
                        positionTo(TilePoint(258.0, 258.0), tween(160))
                        zoomTo(2.3F, tween(160))
                        rotateTo(Degrees(45.0), tween(160))
                    }
                }

                assertTrue(frames > 20)
                mapState.canvasKernel.awaitActiveTiles(1, 2, expected)
                assertEquals(expected.toSet(), requests.toSet())
                assertEquals(expected.size, requests.size)
            } finally {
                parent.cancelAndJoin()
            }
        }
    }

    test("animated pan zoom and rotation publish tiles for the final camera") {
        withContext(Dispatchers.Default.limitedParallelism(1)) {
            val parent = Job()
            val state = mapState(coroutineScope = CoroutineScope(coroutineContext + parent))
            try {
                state.updateCamera(zoom = 3F)
                state.canvasKernel.refreshCanvas(listOf(RasterCanvasParameters(1, tileSource = { z, r, c ->
                    TileResult.Success(RasterTile(z, r, c, null))
                })))
                state.setViewportSize(IntSize(64, 64))
                var frames = 0
                val clock = object : MonotonicFrameClock {
                    override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R = onFrame(++frames * 16_000_000L)
                }
                withContext(clock) {
                    state.motionController.animate {
                        positionTo(TilePoint(400.0, 400.0), tween(320))
                        zoomTo(4F, tween(160))
                        rotateTo(Degrees(45.0), tween(160))
                    }
                }

                state.canvasKernel.awaitActiveTiles(1, 4, listOf(TileSpecs(4, 12, 12)))
            } finally {
                parent.cancelAndJoin()
            }
        }
    }

    test("engine keeps only the latest pending tile request") {
        val scope = CoroutineScope(Job().apply { cancel() })
        val renderer = TileRenderer<RasterTile, RasterTile>(scope, { _, _, _ -> error("Consumer is paused") }, { it })
        val engine = object : CanvasEngine<RasterTile>(coroutineScope = scope, tileRenderer = renderer) {}
        engine.renderTiles(listOf(TileSpecs(3, 3, 3)), 3)
        engine.renderTiles(listOf(TileSpecs(3, 4, 4)), 3)
        val latest = listOf(TileSpecs(4, 12, 12))
        engine.renderTiles(latest, 4)

        assertEquals(latest, renderer.tilesToProcessChannel.tryReceive().getOrThrow())
        assertTrue(renderer.tilesToProcessChannel.tryReceive().isFailure)
    }

    test("empty requests replace obsolete work and publish zoom changes") {
        val scope = CoroutineScope(Job().apply { cancel() })
        val renderer = TileRenderer<RasterTile, RasterTile>(scope, { _, _, _ -> error("Consumer is paused") }, { it })
        val engine = object : CanvasEngine<RasterTile>(coroutineScope = scope, tileRenderer = renderer) {}
        engine.renderTiles(listOf(TileSpecs(2, 1, 1)), 2)
        engine.renderTiles(emptyList(), 2)
        assertEquals(emptyList(), renderer.tilesToProcessChannel.tryReceive().getOrThrow())

        engine.renderTiles(emptyList(), 3)
        assertEquals(3, engine.activeTiles.value.currentZoom)
        assertEquals(emptyList(), renderer.tilesToProcessChannel.tryReceive().getOrThrow())
        engine.renderTiles(emptyList(), 3)
        assertTrue(renderer.tilesToProcessChannel.tryReceive().isFailure)
    }

    test("new canvas receives current tiles when the viewport has not changed") {
        withContext(Dispatchers.Default.limitedParallelism(1)) {
            val parent = Job()
            val mapState = mapState(coroutineScope = CoroutineScope(coroutineContext + parent))
            try {
                mapState.setViewportSize(IntSize(64, 64))
                val first = RasterCanvasParameters(id = 1, tileSource = { z, r, c -> TileResult.Success(RasterTile(z, r, c, null)) })
                val second = RasterCanvasParameters(id = 2, tileSource = first.tileSource)
                mapState.canvasKernel.refreshCanvas(listOf(first))
                val initialTiles = mapState.canvasKernel.awaitActiveTiles(1, 0, listOf(TileSpecs(0, 0, 0)))

                mapState.canvasKernel.refreshCanvas(listOf(first, second))

                assertEquals(initialTiles, mapState.canvasKernel.awaitActiveTiles(2, 0, listOf(TileSpecs(0, 0, 0))))
            } finally {
                parent.cancelAndJoin()
            }
        }
    }

    for (emptySize in listOf(IntSize(0, 64), IntSize(64, 0))) {
        test("new canvases do not replay stale tiles while the viewport is $emptySize") {
            withContext(Dispatchers.Default.limitedParallelism(1)) {
                val parent = Job()
                val state = mapState(coroutineScope = CoroutineScope(coroutineContext + parent))
                val first = RasterCanvasParameters(1, tileSource = { z, r, c -> TileResult.Success(RasterTile(z, r, c, null)) })
                val second = RasterCanvasParameters(2, tileSource = first.tileSource)
                try {
                    state.updateCamera(zoom = 3F)
                    state.setViewportSize(IntSize(64, 64))
                    state.canvasKernel.refreshCanvas(listOf(first))
                    val expected = listOf(TileSpecs(3, 3, 3), TileSpecs(3, 4, 3), TileSpecs(3, 3, 4), TileSpecs(3, 4, 4))
                    state.canvasKernel.awaitActiveTiles(1, 3, expected)

                    state.setViewportSize(emptySize)
                    state.canvasKernel.refreshCanvas(listOf(first, second))

                    assertEquals(ActiveTiles(currentZoom = 3), state.canvasKernel.getActiveTiles(1))
                    assertEquals(ActiveTiles(currentZoom = 3), state.canvasKernel.getActiveTiles(2))
                    state.setViewportSize(IntSize(64, 64))
                    state.canvasKernel.awaitActiveTiles(1, 3, expected)
                    state.canvasKernel.awaitActiveTiles(2, 3, expected)
                } finally {
                    parent.cancelAndJoin()
                }
            }
        }
    }

    test("initial zoom uses the preference minimum") {
        val mapState = mapState(
            zoomLevelPreference = ZoomLevelRange(3, 8),
        )

        assertEquals(3F, mapState.cameraState.zoom)
    }

    test("remember map state uses default and provided arguments") {
        var defaultState: MapState? = null
        compose {
            CompositionLocalProvider(LocalDensity provides Density(1.5F)) {
                defaultState = rememberMapState(mapProperties())
            }
        }

        val state = assertNotNull(defaultState)
        assertEquals(0F, state.cameraState.zoom)
        assertEquals(Density(1.5F), state.currentDensity)

        var configuredState: MapState? = null
        compose {
            configuredState = rememberMapState(
                mapProperties = mapProperties(),
                zoomLevelPreference = ZoomLevelRange(3, 8),
                density = Density(2F),
                coroutineScope = CoroutineScope(EmptyCoroutineContext),
                initialCameraState = cameraState(zoom = 4F),
            )
        }

        val configured = assertNotNull(configuredState)
        assertEquals(4F, configured.cameraState.zoom)
        assertEquals(ZoomLevelRange(3, 8), configured.zoomLevelPreference)
    }

    test("remember map state restores from the saveable state registry") {
        val properties = mapProperties()
        var registry = SaveableStateRegistry(null) { true }
        var state: MapState? = null
        val content: @Composable () -> Unit = {
            CompositionLocalProvider(
                LocalDensity provides Density(1F),
                LocalSaveableStateRegistry provides registry,
            ) {
                state = rememberMapState(properties)
            }
        }

        val originalRecomposer = Recomposer(EmptyCoroutineContext)
        val originalComposition = ControlledComposition(NoOpApplier(), originalRecomposer)
        val originalState: MapState
        val savedValues: Map<String, List<Any?>>
        try {
            originalComposition.composeContent(content)
            originalComposition.applyChanges()
            originalComposition.applyLateChanges()
            originalComposition.changesApplied()

            originalState = assertNotNull(state)
            originalState.updateCamera(zoom = 4F, tilePoint = TilePoint(128.0, 384.0))
            savedValues = registry.performSave()
            assertTrue(savedValues.isNotEmpty())
        } finally {
            originalComposition.dispose()
            originalRecomposer.close()
        }

        registry = SaveableStateRegistry(savedValues) { true }
        state = null
        val restoredRecomposer = Recomposer(EmptyCoroutineContext)
        val restoredComposition = ControlledComposition(NoOpApplier(), restoredRecomposer)
        try {
            restoredComposition.composeContent(content)
            restoredComposition.applyChanges()
            restoredComposition.applyLateChanges()
            restoredComposition.changesApplied()

            val restoredState = assertNotNull(state)
            assertEquals(4F, restoredState.cameraState.zoom)
            assertEquals(TilePoint(128.0, 384.0), restoredState.cameraState.tilePoint)
        } finally {
            restoredComposition.dispose()
            restoredRecomposer.close()
        }
    }

    test("remember map state reuses state across recompositions") {
        val properties = mapProperties()
        val updatedProperties = object : MapProperties by properties {}
        val coroutineScope = CoroutineScope(EmptyCoroutineContext)
        val updatedCoroutineScope = CoroutineScope(EmptyCoroutineContext)
        var updateMapProperties = false
        var updateZoomLevelPreference = false
        var updateDensity = false
        var updateCoroutineScope = false
        var updateInitialCameraState = false
        var compositionCount = 0
        var state: MapState? = null
        val recomposer = Recomposer(EmptyCoroutineContext)
        val composition = ControlledComposition(NoOpApplier(), recomposer)

        val content: @Composable () -> Unit = {
            compositionCount++
            state = rememberMapState(
                mapProperties = if (updateMapProperties) updatedProperties else properties,
                zoomLevelPreference = if (updateZoomLevelPreference) ZoomLevelRange(0, 4) else null,
                density = if (updateDensity) Density(2F) else Density(1.5F),
                coroutineScope = if (updateCoroutineScope) updatedCoroutineScope else coroutineScope,
                initialCameraState = if (updateInitialCameraState) cameraState(zoom = 2F) else null,
            )
        }

        fun recompose() {
            composition.invalidateAll()
            assertTrue(composition.recompose())
            composition.applyChanges()
            composition.applyLateChanges()
            composition.changesApplied()
        }

        try {
            composition.composeContent(content)
            composition.applyChanges()
            composition.applyLateChanges()
            composition.changesApplied()
            val initialState = assertNotNull(state)

            recompose()
            assertSame(initialState, state)

            updateMapProperties = true
            recompose()
            updateMapProperties = false
            recompose()

            updateZoomLevelPreference = true
            recompose()
            updateZoomLevelPreference = false
            recompose()

            updateDensity = true
            recompose()
            updateDensity = false
            recompose()

            updateCoroutineScope = true
            recompose()
            updateCoroutineScope = false
            recompose()

            updateInitialCameraState = true
            recompose()
            updateInitialCameraState = false
            recompose()

            assertEquals(12, compositionCount)
            assertSame(initialState, state)

            val defaultRecomposer = Recomposer(EmptyCoroutineContext)
            val defaultComposition = ControlledComposition(NoOpApplier(), defaultRecomposer)
            var defaultCompositionCount = 0
            var defaultState: MapState? = null
            val defaultContent: @Composable () -> Unit = {
                defaultCompositionCount++
                CompositionLocalProvider(LocalDensity provides Density(1.5F)) {
                    defaultState = rememberMapState(properties)
                }
            }

            try {
                defaultComposition.composeContent(defaultContent)
                defaultComposition.applyChanges()
                defaultComposition.applyLateChanges()
                defaultComposition.changesApplied()
                val initialDefaultState = assertNotNull(defaultState)

                defaultComposition.invalidateAll()
                assertTrue(defaultComposition.recompose())
                defaultComposition.applyChanges()
                defaultComposition.applyLateChanges()
                defaultComposition.changesApplied()

                assertEquals(2, defaultCompositionCount)
                assertSame(initialDefaultState, defaultState)
            } finally {
                defaultComposition.dispose()
                defaultRecomposer.close()
            }
        } finally {
            composition.dispose()
            recomposer.close()
        }
    }

    test("remember map state handles Compose parameter change masks") {
        val properties = mapProperties()
        val zoomLevelPreference = ZoomLevelRange(0, 8)
        val density = Density(1F)
        val coroutineScope = CoroutineScope(EmptyCoroutineContext)
        val initialCameraState = cameraState(zoom = 2F)
        // Invoke the compiler-generated JVM entry point to cover every change-mask status.
        // Compose stores each parameter status in three bits: unknown, same, changed, or static.
        val parameterOffsets = listOf(1, 4, 7, 10, 13)
        val changedMasks = buildList {
            add(0)
            for (status in listOf(2, 4, 6)) {
                add(parameterOffsets.fold(0) { mask, offset -> mask or (status shl offset) })
            }
            for (offset in parameterOffsets) {
                for (status in listOf(2, 4, 6)) {
                    add(status shl offset)
                }
            }
        }
        val method = Class.forName("com.rafambn.kmap.MapStateKt").getDeclaredMethod(
            "rememberMapState",
            MapProperties::class.java,
            ZoomLevelRange::class.java,
            Density::class.java,
            CoroutineScope::class.java,
            CameraState::class.java,
            Composer::class.java,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
        ).apply { isAccessible = true }
        val recomposer = Recomposer(EmptyCoroutineContext)
        val composition = ControlledComposition(NoOpApplier(), recomposer)
        val composerField = composition.javaClass.getDeclaredField("composer")
            .apply { isAccessible = true }
        val states = mutableListOf<MapState>()

        try {
            composition.composeContent {
                val composer = composerField.get(composition) as Composer
                for (changedMask in changedMasks) {
                    key(changedMask) {
                        states += method.invoke(
                            null,
                            properties,
                            zoomLevelPreference,
                            density,
                            coroutineScope,
                            initialCameraState,
                            composer,
                            changedMask,
                            0,
                        ) as MapState
                    }
                }
            }
            composition.applyChanges()
            composition.applyLateChanges()
            composition.changesApplied()

            assertEquals(19, states.size)
            assertTrue(states.all { it.cameraState.zoom == 2F })
        } finally {
            composition.dispose()
            recomposer.close()
        }
    }

    test("constructor uses the default density") {
        val state = MapState(
            mapProperties = mapProperties(),
            coroutineScope = CoroutineScope(EmptyCoroutineContext),
        )

        assertEquals(Density(1F, 1F), state.currentDensity)
    }

    test("constructor rejects an inverted zoom preference") {
        assertFailsWith<IllegalArgumentException> {
            mapState(zoomLevelPreference = ZoomLevelRange(8, 3))
        }
    }

    test("constructor rejects a zoom preference outside the map range") {
        assertFailsWith<IllegalArgumentException> {
            mapState(zoomLevelPreference = ZoomLevelRange(-1, 8))
        }
        assertFailsWith<IllegalArgumentException> {
            mapState(zoomLevelPreference = ZoomLevelRange(3, 32))
        }
    }

    test("constructor rejects an initial zoom outside the preference") {
        assertFailsWith<IllegalArgumentException> {
            mapState(
                zoomLevelPreference = ZoomLevelRange(3, 8),
                initialCameraState = cameraState(zoom = 2F),
            )
        }
    }

    test("changing the zoom preference coerces the current zoom") {
        val zoomAboveMaximum = mapState(initialCameraState = cameraState(zoom = 8F))
        val zoomBelowMinimum = mapState(initialCameraState = cameraState(zoom = 2F))

        zoomAboveMaximum.zoomLevelPreference = ZoomLevelRange(2, 5)
        zoomBelowMinimum.zoomLevelPreference = ZoomLevelRange(3, 9)

        assertEquals(5F, zoomAboveMaximum.cameraState.zoom)
        assertEquals(3F, zoomBelowMinimum.cameraState.zoom)
    }

    test("zoom rejects NaN") {
        val mapState = mapState()
        assertFailsWith<IllegalArgumentException> {
            mapState.updateCamera(zoom = Float.NaN)
        }
    }

    test("centered zoom publishes one camera state") {
        val mapState = mapState()
        mapState.setViewportSize(IntSize(800, 600))
        val centerPoint = TilePoint(270.0, 270.0)
        val initialOffset = context(mapState) { centerPoint.toScreenOffset() }
        var stateWrites = 0

        Snapshot.observe(
            readObserver = null,
            writeObserver = { stateWrites++ },
        ) {
            mapState.motionController.zoomToCentered(2F, centerPoint)
        }

        val finalOffset = context(mapState) { centerPoint.toScreenOffset() }
        assertEquals(1, stateWrites)
        assertEquals(initialOffset.x, finalOffset.x, absoluteTolerance = 0.000001)
        assertEquals(initialOffset.y, finalOffset.y, absoluteTolerance = 0.000001)
    }

    test("centered rotation publishes one camera state") {
        val mapState = mapState()
        mapState.setViewportSize(IntSize(800, 600))
        val centerPoint = TilePoint(270.0, 270.0)
        val initialOffset = context(mapState) { centerPoint.toScreenOffset() }
        var stateWrites = 0

        Snapshot.observe(
            readObserver = null,
            writeObserver = { stateWrites++ },
        ) {
            mapState.motionController.rotateToCentered(Degrees(90.0), centerPoint)
        }

        val finalOffset = context(mapState) { centerPoint.toScreenOffset() }
        assertEquals(1, stateWrites)
        assertEquals(initialOffset.x, finalOffset.x, absoluteTolerance = 0.000001)
        assertEquals(initialOffset.y, finalOffset.y, absoluteTolerance = 0.000001)
    }

    test("equal camera updates do not publish state") {
        val mapState = mapState()
        var stateWrites = 0

        Snapshot.observe(
            readObserver = null,
            writeObserver = { stateWrites++ },
        ) {
            mapState.updateCamera()
        }

        assertEquals(0, stateWrites)
    }

    test("combined camera update places the focal point with density in one write") {
        val mapState = mapState(density = Density(2F))
        mapState.setViewportSize(IntSize(800, 600))
        val focalPoint = TilePoint(270.0, 280.0)
        var stateWrites = 0

        Snapshot.observe(readObserver = null, writeObserver = { stateWrites++ }) {
            mapState.updateCamera(
                zoom = 2F,
                angle = Degrees(37.0),
                tilePoint = focalPoint,
                centerOffset = DifferentialScreenOffset(80.0, -40.0),
            )
        }

        val offset = context(mapState) { focalPoint.toScreenOffset() }
        assertEquals(1, stateWrites)
        assertEquals(2F, mapState.cameraState.zoom)
        assertEquals(Degrees(37.0), mapState.cameraState.angleDegrees)
        assertEquals(480.0, offset.x, absoluteTolerance = 0.000001)
        assertEquals(260.0, offset.y, absoluteTolerance = 0.000001)
    }

    test("default center offset places the tile point at the viewport center") {
        val mapState = mapState(density = Density(2F))
        mapState.setViewportSize(IntSize(800, 600))
        val point = TilePoint(128.0, 384.0)

        mapState.updateCamera(zoom = 3F, angle = Degrees(45.0), tilePoint = point)

        assertEquals(point, mapState.cameraState.tilePoint)
        assertEquals(ScreenOffset(400.0, 300.0), context(mapState) { point.toScreenOffset() })
    }

    test("focal placement uses clamped zoom and respects map borders") {
        val mapState = mapState(zoomLevelPreference = ZoomLevelRange(0, 2))
        mapState.setViewportSize(IntSize(800, 600))
        val point = TilePoint(270.0, 280.0)

        mapState.updateCamera(
            zoom = 10F,
            tilePoint = point,
            centerOffset = DifferentialScreenOffset(80.0, -40.0),
        )

        assertEquals(2F, mapState.cameraState.zoom)
        assertEquals(ScreenOffset(480.0, 260.0), context(mapState) { point.toScreenOffset() })

        mapState.updateCamera(tilePoint = TilePoint(-10.0, 600.0))

        assertEquals(TilePoint(0.0, 512.0), mapState.cameraState.tilePoint)
    }

    test("coordinates are projected only on access") {
        var projections = 0
        val properties = mapProperties()
        val mapState = mapState(mapProperties = object : MapProperties by properties {
            override fun toCoordinates(projectedCoordinates: ProjectedCoordinates): Coordinates {
                projections++
                return properties.toCoordinates(projectedCoordinates)
            }
        })
        mapState.setViewportSize(IntSize(800, 600))

        mapState.updateCamera(zoom = 2F, tilePoint = TilePoint(128.0, 384.0))
        assertEquals(TilePoint(128.0, 384.0), mapState.cameraState.tilePoint)
        assertEquals(0, projections)

        assertEquals(Coordinates(-90.0, -45.0), mapState.coordinates)
        assertEquals(1, projections)
    }

    test("density changes preserve coordinates and tile point") {
        val mapState = mapState(density = Density(1F))
        val coordinates = mapState.coordinates

        mapState.updateDensity(Density(2F, 1.5F))

        assertEquals(2F, mapState.currentDensity.density)
        assertEquals(1.5F, mapState.currentDensity.fontScale)
        assertEquals(coordinates, mapState.coordinates)
        assertEquals(TilePoint(256.0, 256.0), mapState.cameraState.tilePoint)
    }

    test("density updates skip unchanged values and font-only changes") {
        val mapState = mapState(density = Density(2F, 1F))
        mapState.setViewportSize(IntSize(64, 64))

        mapState.updateDensity(Density(2F, 1F))
        mapState.updateDensity(Density(2F, 1.5F))

        assertEquals(Density(2F, 1.5F), mapState.currentDensity)
        assertEquals(TilePoint(256.0, 256.0), mapState.cameraState.tilePoint)
    }

    test("visible tile resolution waits for both viewport dimensions") {
        val mapState = mapState()

        mapState.setViewportSize(IntSize(64, 0))

        assertEquals(IntSize(64, 0), mapState.viewportSize)
    }

    test("density is applied when the tile point is converted to screen") {
        val mapState = mapState(density = Density(1F))
        mapState.setViewportSize(IntSize(800, 600))
        val point = TilePoint(266.0, 256.0)

        val initialOffset = context(mapState) { point.toScreenOffset() }
        mapState.updateDensity(Density(2F))
        val scaledOffset = context(mapState) { point.toScreenOffset() }

        assertEquals(ScreenOffset(410.0, 300.0), initialOffset)
        assertEquals(ScreenOffset(420.0, 300.0), scaledOffset)
        assertEquals(TilePoint(256.0, 256.0), mapState.cameraState.tilePoint)
    }

    test("initial tile point defines coordinates") {
        val mapState = MapState(
            mapProperties = mapProperties(),
            initialCameraState = CameraState(
                tilePoint = TilePoint(128.0, 384.0),
            ),
            coroutineScope = CoroutineScope(EmptyCoroutineContext),
            density = Density(2F),
        )

        assertEquals(TilePoint(128.0, 384.0), mapState.cameraState.tilePoint)
        assertEquals(Coordinates(-90.0, -45.0), mapState.coordinates)
    }

    test("saver restores the tile point without density") {
        val mapProperties = mapProperties()
        val coroutineScope = CoroutineScope(EmptyCoroutineContext)
        val original = mapState(
            mapProperties = mapProperties,
            coroutineScope = coroutineScope,
            density = Density(1F),
        )
        original.updateCamera(tilePoint = TilePoint(128.0, 384.0))
        val saver = MapState.saver(
            mapProperties = mapProperties,
            zoomLevelPreference = mapProperties.zoomLevels,
            density = Density(1F),
            coroutineScope = coroutineScope,
        )
        val saved = assertNotNull(with(saver) {
            SaverScope { true }.save(original)
        })

        val restored = assertNotNull(saver.restore(saved))

        assertEquals(original.coordinates, restored.coordinates)
        assertEquals(TilePoint(128.0, 384.0), restored.cameraState.tilePoint)
    }

    test("saver restores with the current configuration and coerces old zoom") {
        val mapProperties = mapProperties()
        val coroutineScope = CoroutineScope(EmptyCoroutineContext)
        val density = Density(2F, 1.5F)
        val currentZoomLevelPreference = ZoomLevelRange(3, 8)
        val original = mapState(mapProperties = mapProperties)
        original.updateCamera(zoom = 20F)
        val saver = MapState.saver(
            mapProperties = mapProperties,
            zoomLevelPreference = currentZoomLevelPreference,
            density = density,
            coroutineScope = coroutineScope,
        )
        val saved = assertNotNull(with(saver) {
            SaverScope { true }.save(original)
        })

        assertFalse("zoomLevelPreference" in (saved as List<*>))

        val restored = assertNotNull(saver.restore(saved))

        assertSame(mapProperties, restored.mapProperties)
        assertSame(coroutineScope, restored.canvasKernel.coroutineScope)
        assertSame(currentZoomLevelPreference, restored.zoomLevelPreference)
        assertEquals(2F, restored.currentDensity.density)
        assertEquals(1.5F, restored.currentDensity.fontScale)
        assertEquals(8F, restored.cameraState.zoom)
    }

    test("saver does not scale the tile point from saved density") {
        val mapProperties = mapProperties()
        val saver = MapState.saver(
            mapProperties = mapProperties,
            zoomLevelPreference = mapProperties.zoomLevels,
            density = Density(1F),
            coroutineScope = CoroutineScope(EmptyCoroutineContext),
        )
        val savedState = listOf(
            "density", 2F,
            "zoom", 0F,
            "angleDegrees", 0.0,
            "tilePoint", Pair(512.0, 512.0),
        )

        val restored = assertNotNull(saver.restore(savedState))

        assertEquals(TilePoint(512.0, 512.0), restored.cameraState.tilePoint)
    }
}
