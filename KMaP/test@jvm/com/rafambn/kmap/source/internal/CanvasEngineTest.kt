@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.rafambn.kmap.source.internal

import com.rafambn.kmap.source.RasterTile
import com.rafambn.kmap.source.TileResult
import com.rafambn.kmap.source.TileSpecs
import de.infix.testBalloon.framework.core.testSuite
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import kotlin.test.assertEquals
import kotlin.test.assertTrue

val CanvasEngineTest by testSuite {
    test("selection submission waits for the engine and retains only the latest pending selection") {
        testEngine { engine, renderer, scheduler ->
            engine.renderTiles(listOf(TileSpecs(3, 3, 3)), 3)
            engine.renderTiles(listOf(TileSpecs(3, 4, 4)), 3)
            val latest = listOf(TileSpecs(4, 12, 12))
            engine.renderTiles(latest, 4)

            assertEquals(ActiveTiles(), engine.activeTiles)
            assertTrue(renderer.tilesToProcessChannel.tryReceive().isFailure)
            scheduler.runCurrent()

            assertEquals(latest, renderer.tilesToProcessChannel.tryReceive().getOrThrow())
            assertTrue(renderer.tilesToProcessChannel.tryReceive().isFailure)
            assertEquals(ActiveTiles(currentZoom = 4), engine.activeTiles)
        }
    }

    test("selection submission takes ownership of the caller's list") {
        testEngine { engine, renderer, scheduler ->
            val expected = listOf(TileSpecs(3, 3, 3), TileSpecs(3, 4, 4))
            val submitted = expected.toMutableList()
            engine.renderTiles(submitted, 3)
            submitted.clear()
            submitted.add(TileSpecs(3, 7, 7))
            scheduler.runCurrent()

            assertEquals(expected, renderer.tilesToProcessChannel.tryReceive().getOrThrow())
            val loaded = expected.map { RasterTile(it.zoom, it.row, it.col, null) }
            loaded.forEach { renderer.tilesProcessedChannel.trySend(it).getOrThrow() }
            scheduler.runCurrent()
            assertEquals(ActiveTiles(currentZoom = 3, tiles = loaded), engine.activeTiles)
        }
    }

    test("empty selections replace obsolete work and publish zoom changes") {
        testEngine { engine, renderer, scheduler ->
            engine.renderTiles(listOf(TileSpecs(2, 1, 1)), 2)
            scheduler.runCurrent()
            engine.renderTiles(emptyList(), 2)
            scheduler.runCurrent()
            assertEquals(emptyList(), renderer.tilesToProcessChannel.tryReceive().getOrThrow())

            engine.renderTiles(emptyList(), 3)
            scheduler.runCurrent()
            assertEquals(ActiveTiles(currentZoom = 3), engine.activeTiles)
            assertEquals(emptyList(), renderer.tilesToProcessChannel.tryReceive().getOrThrow())

            engine.renderTiles(emptyList(), 3)
            scheduler.runCurrent()
            assertTrue(renderer.tilesToProcessChannel.tryReceive().isFailure)
        }
    }

    test("unchanged selections do not resubmit pending or loaded tiles") {
        testEngine { engine, renderer, scheduler ->
            val tile = RasterTile(3, 3, 3, null)
            val visible = listOf(TileSpecs(3, 3, 3))
            engine.renderTiles(visible, 3)
            scheduler.runCurrent()
            assertEquals(visible, renderer.tilesToProcessChannel.tryReceive().getOrThrow())

            engine.renderTiles(visible, 3)
            scheduler.runCurrent()
            assertTrue(renderer.tilesToProcessChannel.tryReceive().isFailure)

            renderer.tilesProcessedChannel.trySend(tile).getOrThrow()
            scheduler.runCurrent()
            engine.renderTiles(visible, 3)
            scheduler.runCurrent()
            assertEquals(ActiveTiles(currentZoom = 3, tiles = listOf(tile)), engine.activeTiles)
            assertTrue(renderer.tilesToProcessChannel.tryReceive().isFailure)
        }
    }

    test("late results populate the cache without restoring a previous selection") {
        testEngine { engine, renderer, scheduler ->
            val previous = RasterTile(3, 0, 0, null)
            val latest = RasterTile(4, 15, 15, null)
            engine.renderTiles(listOf(previous.specs()), 3)
            scheduler.runCurrent()
            renderer.tilesToProcessChannel.tryReceive().getOrThrow()

            engine.renderTiles(listOf(latest.specs()), 4)
            renderer.tilesProcessedChannel.trySend(previous).getOrThrow()
            scheduler.runCurrent()
            assertEquals(ActiveTiles(currentZoom = 4), engine.activeTiles)
            assertEquals(listOf(latest), renderer.tilesToProcessChannel.tryReceive().getOrThrow())

            renderer.tilesProcessedChannel.trySend(latest).getOrThrow()
            scheduler.runCurrent()
            assertEquals(ActiveTiles(currentZoom = 4, tiles = listOf(latest)), engine.activeTiles)

            engine.renderTiles(listOf(previous.specs()), 3)
            scheduler.runCurrent()
            assertEquals(ActiveTiles(currentZoom = 3, tiles = listOf(previous)), engine.activeTiles)
            assertEquals(emptyList(), renderer.tilesToProcessChannel.tryReceive().getOrThrow())
        }
    }

    test("late results cannot repopulate an empty viewport or change its zoom") {
        testEngine { engine, renderer, scheduler ->
            val tile = RasterTile(3, 3, 3, null)
            engine.renderTiles(listOf(tile.specs()), 3)
            scheduler.runCurrent()
            engine.renderTiles(emptyList(), 5)
            scheduler.runCurrent()
            assertEquals(emptyList(), renderer.tilesToProcessChannel.tryReceive().getOrThrow())

            renderer.tilesProcessedChannel.trySend(tile).getOrThrow()
            scheduler.runCurrent()
            assertEquals(ActiveTiles(currentZoom = 5), engine.activeTiles)
            assertTrue(renderer.tilesToProcessChannel.tryReceive().isFailure)

            engine.renderTiles(listOf(tile.specs()), 3)
            scheduler.runCurrent()
            assertEquals(ActiveTiles(currentZoom = 3, tiles = listOf(tile)), engine.activeTiles)
            assertEquals(emptyList(), renderer.tilesToProcessChannel.tryReceive().getOrThrow())
        }
    }

    test("parent and child tiles cover a zoom change until the requested tile arrives") {
        testEngine(maxCacheTiles = 1) { engine, renderer, scheduler ->
            val parent = RasterTile(1, 0, 0, null)
            val child = RasterTile(2, 1, 1, null)
            engine.renderTiles(listOf(parent.specs()), 1)
            renderer.tilesProcessedChannel.trySend(parent).getOrThrow()
            scheduler.runCurrent()

            engine.renderTiles(listOf(child.specs()), 2)
            scheduler.runCurrent()
            assertEquals(ActiveTiles(currentZoom = 2, tiles = listOf(parent)), engine.activeTiles)
            assertEquals(listOf(child), renderer.tilesToProcessChannel.tryReceive().getOrThrow())

            renderer.tilesProcessedChannel.trySend(child).getOrThrow()
            scheduler.runCurrent()
            assertEquals(ActiveTiles(currentZoom = 2, tiles = listOf(child)), engine.activeTiles)

            engine.renderTiles(listOf(parent.specs()), 1)
            scheduler.runCurrent()
            assertEquals(ActiveTiles(currentZoom = 1, tiles = listOf(child)), engine.activeTiles)
            assertEquals(listOf(parent), renderer.tilesToProcessChannel.tryReceive().getOrThrow())

            renderer.tilesProcessedChannel.trySend(parent).getOrThrow()
            scheduler.runCurrent()
            assertEquals(ActiveTiles(currentZoom = 1, tiles = listOf(parent)), engine.activeTiles)
        }
    }

    test("repeated coordinates reuse a normalized cached tile") {
        testEngine { engine, renderer, scheduler ->
            val normalized = RasterTile(2, 0, 3, null)
            engine.renderTiles(listOf(normalized.specs()), 2)
            renderer.tilesProcessedChannel.trySend(normalized).getOrThrow()
            scheduler.runCurrent()

            val repeated = listOf(TileSpecs(2, 4, -1), TileSpecs(2, -4, 7))
            engine.renderTiles(repeated, 2)
            scheduler.runCurrent()
            assertEquals(
                ActiveTiles(currentZoom = 2, tiles = repeated.map { normalized.withSpecs(it) }),
                engine.activeTiles,
            )
            assertEquals(emptyList(), renderer.tilesToProcessChannel.tryReceive().getOrThrow())
        }
    }

    test("the cache retains its limit while active tiles remain available") {
        testEngine(maxCacheTiles = 2) { engine, renderer, scheduler ->
            val tiles = (0..2).map { RasterTile(2, it, it, null) }
            engine.renderTiles(tiles.map { it.specs() }, 2)
            scheduler.runCurrent()
            tiles.forEach { renderer.tilesProcessedChannel.trySend(it).getOrThrow() }
            scheduler.runCurrent()
            assertEquals(ActiveTiles(currentZoom = 2, tiles = tiles), engine.activeTiles)

            engine.renderTiles(emptyList(), 2)
            scheduler.runCurrent()
            engine.renderTiles(tiles.map { it.specs() }, 2)
            scheduler.runCurrent()
            assertEquals(ActiveTiles(currentZoom = 2, tiles = tiles.takeLast(2)), engine.activeTiles)
            assertEquals(listOf(tiles.first()), renderer.tilesToProcessChannel.tryReceive().getOrThrow())
        }
    }

    for (startEngine in listOf(false, true)) {
        test("cancellation discards queued and future updates, started=$startEngine") {
            val scheduler = TestCoroutineScheduler()
            val parent = Job()
            val renderer = pausedRenderer()
            val engine = object : CanvasEngine<RasterTile>(
                coroutineScope = CoroutineScope(parent + StandardTestDispatcher(scheduler)),
                tileRenderer = renderer,
            ) {}
            try {
                if (startEngine) {
                    engine.renderTiles(emptyList(), 2)
                    scheduler.runCurrent()
                    renderer.tilesToProcessChannel.tryReceive().getOrThrow()
                }
                val before = engine.activeTiles
                engine.renderTiles(listOf(TileSpecs(3, 3, 3)), 3)
                renderer.tilesProcessedChannel.trySend(RasterTile(3, 3, 3, null)).getOrThrow()
                parent.cancel()
                scheduler.runCurrent()
                parent.join()

                engine.renderTiles(listOf(TileSpecs(4, 12, 12)), 4)
                scheduler.runCurrent()
                assertEquals(before, engine.activeTiles)
                assertTrue(renderer.tilesToProcessChannel.tryReceive().isFailure)
                assertTrue(parent.children.none())
            } finally {
                parent.cancel()
                scheduler.runCurrent()
                parent.join()
            }
        }
    }

    test("concurrent callers and tile workers converge on the final selection") {
        withContext(Dispatchers.Default) {
            withTimeout(5_000) {
                val parent = Job(coroutineContext[Job])
                val engine = RasterCanvasEngine(
                    maxCacheTiles = 20,
                    getTile = { zoom, row, col ->
                        yield()
                        TileResult.Success(RasterTile(zoom, row, col, null))
                    },
                    coroutineScope = CoroutineScope(parent + Dispatchers.Default),
                )
                try {
                    coroutineScope {
                        repeat(4) { producer ->
                            launch {
                                repeat(50) { index ->
                                    engine.renderTiles(listOf(TileSpecs(6, producer, index)), 6)
                                    yield()
                                }
                            }
                        }
                    }
                    val latest = RasterTile(7, 120, 120, null)
                    engine.renderTiles(listOf(latest.specs()), 7)
                    val expected = ActiveTiles(currentZoom = 7, tiles = listOf(latest))
                    while (engine.activeTiles != expected) delay(1)
                    assertEquals(expected, engine.activeTiles)
                } finally {
                    parent.cancelAndJoin()
                }
            }
        }
    }
}

private suspend fun testEngine(
    maxCacheTiles: Int = 20,
    test: (CanvasEngine<RasterTile>, TileRenderer<RasterTile, RasterTile>, TestCoroutineScheduler) -> Unit,
) {
    val scheduler = TestCoroutineScheduler()
    val parent = Job()
    val renderer = pausedRenderer()
    val engine = object : CanvasEngine<RasterTile>(
        maxCacheTiles = maxCacheTiles,
        coroutineScope = CoroutineScope(parent + StandardTestDispatcher(scheduler)),
        tileRenderer = renderer,
    ) {}
    try {
        test(engine, renderer, scheduler)
    } finally {
        parent.cancel()
        scheduler.runCurrent()
        parent.join()
    }
}

private fun pausedRenderer() = TileRenderer<RasterTile, RasterTile>(
    // Tests control requests and completions directly while the engine runs on its own scheduler.
    coroutineScope = CoroutineScope(Job().apply { cancel() }),
    getTile = { _, _, _ -> error("Renderer is paused") },
    processTile = { it },
)

private fun RasterTile.specs() = TileSpecs(zoom, row, col)
