@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.rafambn.kmap.source.internal

import com.rafambn.kmap.source.RasterTile
import com.rafambn.kmap.source.TileResult
import com.rafambn.kmap.source.TileSpecs
import de.infix.testBalloon.framework.core.testSuite
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.test.assertEquals
import kotlin.test.assertTrue

val TileRendererTest by testSuite {
    test("updates copy the caller's list and retain only the latest pending request") {
        val requested = mutableListOf<TileSpecs>()
        testRenderer(getTile = { zoom, row, col ->
            requested.add(TileSpecs(zoom, row, col))
            TileResult.Success(RasterTile(zoom, row, col, null))
        }) { renderer, scheduler ->
            renderer.updateRequiredTiles(listOf(TileSpecs(2, 0, 0)))
            val latest = listOf(TileSpecs(2, 1, 1), TileSpecs(2, 2, 2))
            val submitted = latest.toMutableList()
            renderer.updateRequiredTiles(submitted)
            submitted.clear()
            submitted.add(TileSpecs(2, 3, 3))
            assertTrue(requested.isEmpty())
            scheduler.runCurrent()

            assertEquals(latest, requested)
            assertEquals(
                latest.map { TileResult.Success(RasterTile(it.zoom, it.row, it.col, null)) },
                List(2) { renderer.results.tryReceive().getOrThrow() },
            )
            assertTrue(renderer.results.tryReceive().isFailure)
        }
    }

    test("an empty update clears pending requests and lets in-flight tiles finish") {
        val requested = mutableListOf<TileSpecs>()
        val finish = CompletableDeferred<Unit>()
        testRenderer(getTile = { zoom, row, col ->
            requested.add(TileSpecs(zoom, row, col))
            finish.await()
            TileResult.Success(RasterTile(zoom, row, col, null))
        }) { renderer, scheduler ->
            val first = TileSpecs(2, 0, 0)
            renderer.updateRequiredTiles(listOf(first))
            scheduler.runCurrent()
            assertEquals(listOf(first), requested)

            renderer.updateRequiredTiles(listOf(TileSpecs(2, 1, 1)))
            renderer.updateRequiredTiles(emptyList())
            scheduler.runCurrent()
            assertEquals(listOf(first), requested)
            assertTrue(renderer.results.tryReceive().isFailure)

            finish.complete(Unit)
            scheduler.runCurrent()
            assertEquals(TileResult.Success(RasterTile(2, 0, 0, null)), renderer.results.tryReceive().getOrThrow())
            assertTrue(renderer.results.tryReceive().isFailure)
        }
    }

    test("every completed success and failure is retained until consumed") {
        testRenderer(getTile = { zoom, row, col ->
            if (col % 2 == 0) TileResult.Success(RasterTile(zoom, row, col, null))
            else TileResult.Failure(TileSpecs(zoom, row, col))
        }) { renderer, scheduler ->
            renderer.updateRequiredTiles((0..3).map { TileSpecs(2, 0, it) })
            scheduler.runCurrent()

            assertEquals(
                listOf(
                    TileResult.Success(RasterTile(2, 0, 0, null)), TileResult.Failure(TileSpecs(2, 0, 1)),
                    TileResult.Success(RasterTile(2, 0, 2, null)), TileResult.Failure(TileSpecs(2, 0, 3)),
                ),
                List(4) { renderer.results.tryReceive().getOrThrow() },
            )
            assertTrue(renderer.results.tryReceive().isFailure)
        }
    }

    for (startRenderer in listOf(false, true)) {
        test("cancellation closes results and ignores future updates, started=$startRenderer") {
            val scheduler = TestCoroutineScheduler()
            val dispatcher = StandardTestDispatcher(scheduler)
            val parent = Job()
            var started = 0
            var cancelled = 0
            val renderer = TileRenderer<RasterTile, RasterTile>(
                coroutineScope = CoroutineScope(parent + dispatcher),
                getTile = { _, _, _ ->
                    started++
                    try {
                        awaitCancellation()
                    } finally {
                        cancelled++
                    }
                },
                processTile = { it },
                dispatcher = dispatcher,
            )
            try {
                renderer.updateRequiredTiles(listOf(TileSpecs(2, 0, 0)))
                if (startRenderer) scheduler.runCurrent()
                parent.cancel()
                scheduler.runCurrent()
                parent.join()

                assertTrue(renderer.results.tryReceive().isClosed)
                renderer.updateRequiredTiles(listOf(TileSpecs(2, 1, 1)))
                scheduler.runCurrent()
                assertEquals(if (startRenderer) 1 else 0, started)
                assertEquals(started, cancelled)
                assertTrue(renderer.results.tryReceive().isClosed)
                assertTrue(parent.children.none())
            } finally {
                parent.cancel()
                scheduler.runCurrent()
                parent.join()
            }
        }
    }

    test("duplicate source coordinates share a worker and can be requested again after success") {
        withContext(Dispatchers.Default) {
            withTimeout(5_000) {
                val parent = Job(coroutineContext[Job])
                val requests = Channel<TileSpecs>(Channel.UNLIMITED)
                val finishTile = CompletableDeferred<Unit>()
                val source = TileSpecs(3, 2, 4)
                val marker = TileSpecs(3, 0, 0)
                val renderer = TileRenderer(
                    coroutineScope = CoroutineScope(parent + Dispatchers.Default),
                    getTile = { zoom, row, col ->
                        val specs = TileSpecs(zoom, row, col)
                        requests.send(specs)
                        if (specs == source) finishTile.await()
                        TileResult.Success(RasterTile(zoom, row, col, null))
                    },
                    processTile = { it },
                )
                try {
                    renderer.updateRequiredTiles(listOf(source, source))
                    assertEquals(source, requests.receive())

                    renderer.updateRequiredTiles(listOf(source, source, marker))
                    assertEquals(marker, requests.receive())
                    assertEquals(TileResult.Success(RasterTile(marker.zoom, marker.row, marker.col, null)), renderer.results.receive())
                    assertTrue(requests.tryReceive().isFailure)

                    finishTile.complete(Unit)
                    assertEquals(TileResult.Success(RasterTile(source.zoom, source.row, source.col, null)), renderer.results.receive())
                    renderer.updateRequiredTiles(listOf(source))
                    assertEquals(source, requests.receive())
                    assertEquals(TileResult.Success(RasterTile(source.zoom, source.row, source.col, null)), renderer.results.receive())
                    assertTrue(requests.tryReceive().isFailure)
                    assertTrue(renderer.results.tryReceive().isFailure)
                } finally {
                    parent.cancelAndJoin()
                }
            }
        }
    }

    for (failure in listOf("source result", "tile failure result", "source exception", "processing exception")) {
        test("source coordinates can be retried after a $failure") {
            withContext(Dispatchers.Default) {
                withTimeout(5_000) {
                    val parent = Job(coroutineContext[Job])
                    val attempts = AtomicInteger()
                    val source = TileSpecs(3, 2, 4)
                    val renderer = TileRenderer(
                        coroutineScope = CoroutineScope(parent + Dispatchers.Default),
                        getTile = { zoom, row, col ->
                            val specs = TileSpecs(zoom, row, col)
                            if (specs == source && attempts.incrementAndGet() == 1) {
                                when (failure) {
                                    "source result" -> TileResult.Failure(specs)
                                    "tile failure result" -> TileResult.Failure(RasterTile(zoom, row, col, null))
                                    "source exception" -> error("Source failure")
                                    else -> TileResult.Success(RasterTile(zoom, row, col, null))
                                }
                            } else {
                                TileResult.Success(RasterTile(zoom, row, col, null))
                            }
                        },
                        processTile = { tile ->
                            if (tile == source && attempts.get() == 1 && failure == "processing exception") {
                                error("Processing failure")
                            }
                            tile
                        },
                    )
                    try {
                        renderer.updateRequiredTiles(listOf(source))
                        assertEquals(TileResult.Failure(source), renderer.results.receive())
                        assertTrue(renderer.results.tryReceive().isFailure)

                        renderer.updateRequiredTiles(listOf(source))
                        assertEquals(TileResult.Success(RasterTile(source.zoom, source.row, source.col, null)), renderer.results.receive())
                        assertEquals(2, attempts.get())
                        assertTrue(renderer.results.tryReceive().isFailure)
                    } finally {
                        parent.cancelAndJoin()
                    }
                }
            }
        }
    }
}

private suspend fun testRenderer(
    getTile: suspend (Int, Int, Int) -> TileResult<RasterTile>,
    test: (TileRenderer<RasterTile, RasterTile>, TestCoroutineScheduler) -> Unit,
) {
    val scheduler = TestCoroutineScheduler()
    val dispatcher = StandardTestDispatcher(scheduler)
    val parent = Job()
    val renderer = TileRenderer(
        coroutineScope = CoroutineScope(parent + dispatcher),
        getTile = getTile,
        processTile = { it },
        dispatcher = dispatcher,
    )
    try {
        test(renderer, scheduler)
    } finally {
        parent.cancel()
        scheduler.runCurrent()
        parent.join()
    }
}
