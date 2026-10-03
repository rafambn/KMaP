@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.rafambn.kmap.source.internal

import com.rafambn.kmap.source.RasterTile
import com.rafambn.kmap.source.TileResult
import com.rafambn.kmap.source.TileSpecs
import de.infix.testBalloon.framework.core.testSuite
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import kotlin.test.assertEquals
import kotlin.test.assertTrue

val CanvasEngineTest by testSuite {
    test("selection submission waits for the engine and retains only the latest pending selection") {
        testEngine {
            engine.renderTiles(listOf(TileSpecs(3, 3, 3)), 3)
            engine.renderTiles(listOf(TileSpecs(3, 4, 4)), 3)
            val latest = listOf(TileSpecs(4, 12, 12))
            engine.renderTiles(latest, 4)

            assertEquals(ActiveTiles(), engine.activeTiles)
            assertTrue(takeRequests().isEmpty())
            scheduler.runCurrent()

            assertEquals(latest, takeRequests())
            assertTrue(takeRequests().isEmpty())
            assertEquals(ActiveTiles(currentZoom = 4), engine.activeTiles)
        }
    }

    test("selection submission takes ownership of the caller's list") {
        testEngine {
            val expected = listOf(TileSpecs(3, 3, 3), TileSpecs(3, 4, 4))
            val submitted = expected.toMutableList()
            engine.renderTiles(submitted, 3)
            submitted.clear()
            submitted.add(TileSpecs(3, 7, 7))
            scheduler.runCurrent()

            assertEquals(expected, takeRequests())
            val loaded = expected.map { RasterTile(it.zoom, it.row, it.col, null) }
            loaded.forEach { completeTile(it) }
            scheduler.runCurrent()
            assertEquals(ActiveTiles(currentZoom = 3, tiles = loaded), engine.activeTiles)
        }
    }

    test("empty selections replace obsolete work and publish zoom changes") {
        testEngine {
            engine.renderTiles(listOf(TileSpecs(2, 1, 1)), 2)
            engine.renderTiles(emptyList(), 2)
            scheduler.runCurrent()
            assertEquals(emptyList(), takeRequests())

            engine.renderTiles(emptyList(), 3)
            scheduler.runCurrent()
            assertEquals(ActiveTiles(currentZoom = 3), engine.activeTiles)
            assertEquals(emptyList(), takeRequests())

            engine.renderTiles(emptyList(), 3)
            scheduler.runCurrent()
            assertTrue(takeRequests().isEmpty())
        }
    }

    test("unchanged selections do not resubmit pending or loaded tiles") {
        testEngine {
            val tile = RasterTile(3, 3, 3, null)
            val visible = listOf(TileSpecs(3, 3, 3))
            engine.renderTiles(visible, 3)
            scheduler.runCurrent()
            assertEquals(visible, takeRequests())

            engine.renderTiles(visible, 3)
            scheduler.runCurrent()
            assertTrue(takeRequests().isEmpty())

            completeTile(tile)
            scheduler.runCurrent()
            engine.renderTiles(visible, 3)
            scheduler.runCurrent()
            assertEquals(ActiveTiles(currentZoom = 3, tiles = listOf(tile)), engine.activeTiles)
            assertTrue(takeRequests().isEmpty())
        }
    }

    test("late results populate the cache without restoring a previous selection") {
        testEngine {
            val previous = RasterTile(3, 0, 0, null)
            val latest = RasterTile(4, 15, 15, null)
            engine.renderTiles(listOf(previous.specs()), 3)
            scheduler.runCurrent()
            takeRequests()

            engine.renderTiles(listOf(latest.specs()), 4)
            completeTile(previous)
            scheduler.runCurrent()
            assertEquals(ActiveTiles(currentZoom = 4), engine.activeTiles)
            assertEquals(listOf(latest), takeRequests())

            completeTile(latest)
            scheduler.runCurrent()
            assertEquals(ActiveTiles(currentZoom = 4, tiles = listOf(latest)), engine.activeTiles)

            engine.renderTiles(listOf(previous.specs()), 3)
            scheduler.runCurrent()
            assertEquals(ActiveTiles(currentZoom = 3, tiles = listOf(previous)), engine.activeTiles)
            assertEquals(emptyList(), takeRequests())
        }
    }

    test("late results cannot repopulate an empty viewport or change its zoom") {
        testEngine {
            val tile = RasterTile(3, 3, 3, null)
            engine.renderTiles(listOf(tile.specs()), 3)
            scheduler.runCurrent()
            assertEquals(listOf(tile.specs()), takeRequests())
            engine.renderTiles(emptyList(), 5)
            scheduler.runCurrent()
            assertEquals(emptyList(), takeRequests())

            completeTile(tile)
            scheduler.runCurrent()
            assertEquals(ActiveTiles(currentZoom = 5), engine.activeTiles)
            assertTrue(takeRequests().isEmpty())

            engine.renderTiles(listOf(tile.specs()), 3)
            scheduler.runCurrent()
            assertEquals(ActiveTiles(currentZoom = 3, tiles = listOf(tile)), engine.activeTiles)
            assertEquals(emptyList(), takeRequests())
        }
    }

    test("parent and child tiles cover a zoom change until the requested tile arrives") {
        testEngine(maxCacheTiles = 1) {
            val parent = RasterTile(1, 0, 0, null)
            val child = RasterTile(2, 1, 1, null)
            engine.renderTiles(listOf(parent.specs()), 1)
            scheduler.runCurrent()
            assertEquals(listOf(parent.specs()), takeRequests())
            completeTile(parent)
            scheduler.runCurrent()

            engine.renderTiles(listOf(child.specs()), 2)
            scheduler.runCurrent()
            assertEquals(ActiveTiles(currentZoom = 2, tiles = listOf(parent)), engine.activeTiles)
            assertEquals(listOf(child), takeRequests())

            completeTile(child)
            scheduler.runCurrent()
            assertEquals(ActiveTiles(currentZoom = 2, tiles = listOf(child)), engine.activeTiles)

            engine.renderTiles(listOf(parent.specs()), 1)
            scheduler.runCurrent()
            assertEquals(ActiveTiles(currentZoom = 1, tiles = listOf(child)), engine.activeTiles)
            assertEquals(listOf(parent), takeRequests())

            completeTile(parent)
            scheduler.runCurrent()
            assertEquals(ActiveTiles(currentZoom = 1, tiles = listOf(parent)), engine.activeTiles)
        }
    }

    test("repeated coordinates reuse a normalized cached tile") {
        testEngine {
            val normalized = RasterTile(2, 0, 3, null)
            engine.renderTiles(listOf(normalized.specs()), 2)
            scheduler.runCurrent()
            assertEquals(listOf(normalized.specs()), takeRequests())
            completeTile(normalized)
            scheduler.runCurrent()

            val repeated = listOf(TileSpecs(2, 4, -1), TileSpecs(2, -4, 7))
            engine.renderTiles(repeated, 2)
            scheduler.runCurrent()
            assertEquals(
                ActiveTiles(currentZoom = 2, tiles = repeated.map { normalized.withSpecs(it) }),
                engine.activeTiles,
            )
            assertEquals(emptyList(), takeRequests())
        }
    }

    for ((zoom, visible, expectedRequests) in listOf(
        Triple(
            0,
            listOf(TileSpecs(0, -1, 1), TileSpecs(0, 0, 0), TileSpecs(0, Int.MIN_VALUE, Int.MAX_VALUE)),
            listOf(TileSpecs(0, 0, 0)),
        ),
        Triple(
            2,
            listOf(TileSpecs(2, 4, -1), TileSpecs(2, -4, 7), TileSpecs(2, -1, 4), TileSpecs(2, 3, 0)),
            listOf(TileSpecs(2, 0, 3), TileSpecs(2, 3, 0)),
        ),
        Triple(
            30,
            listOf(
                TileSpecs(30, -1, 1073741824), TileSpecs(30, 1073741823, 0),
                TileSpecs(30, Int.MIN_VALUE, Int.MAX_VALUE), TileSpecs(30, 0, 1073741823),
            ),
            listOf(TileSpecs(30, 1073741823, 0), TileSpecs(30, 0, 1073741823)),
        ),
    )) {
        test("the engine requests each normalized tile once and restores every display copy at zoom $zoom") {
            testEngine {
                engine.renderTiles(visible, zoom)
                scheduler.runCurrent()

                assertEquals(expectedRequests, takeRequests())
                assertTrue(takeRequests().isEmpty())
                for (specs in expectedRequests) {
                    completeTile(RasterTile(specs.zoom, specs.row, specs.col, null))
                }
                scheduler.runCurrent()

                assertEquals(
                    ActiveTiles(currentZoom = zoom, tiles = visible.map { RasterTile(it.zoom, it.row, it.col, null) }),
                    engine.activeTiles,
                )
            }
        }
    }

    test("repeated parent and child fallbacks keep their display coordinates") {
        testEngine(maxCacheTiles = 1) {
            val parent = TileSpecs(1, -1, 2)
            val child = TileSpecs(2, -1, 5)
            val parentTile = RasterTile(1, 1, 0, null)
            val childTile = RasterTile(2, 3, 1, null)
            engine.renderTiles(listOf(parent), 1)
            scheduler.runCurrent()
            assertEquals(listOf(parentTile.specs()), takeRequests())
            completeTile(parentTile)
            scheduler.runCurrent()

            engine.renderTiles(listOf(child), 2)
            scheduler.runCurrent()
            assertEquals(ActiveTiles(currentZoom = 2, tiles = listOf(parentTile.withSpecs(parent))), engine.activeTiles)
            assertEquals(listOf(childTile.specs()), takeRequests())

            completeTile(childTile)
            scheduler.runCurrent()
            assertEquals(ActiveTiles(currentZoom = 2, tiles = listOf(childTile.withSpecs(child))), engine.activeTiles)

            engine.renderTiles(listOf(parent), 1)
            scheduler.runCurrent()
            assertEquals(ActiveTiles(currentZoom = 1, tiles = listOf(childTile.withSpecs(child))), engine.activeTiles)
            assertEquals(listOf(parentTile.specs()), takeRequests())

            completeTile(parentTile)
            scheduler.runCurrent()
            assertEquals(ActiveTiles(currentZoom = 1, tiles = listOf(parentTile.withSpecs(parent))), engine.activeTiles)
        }
    }

    test("an in-flight source tile serves the latest repeated copies after a viewport change") {
        withContext(Dispatchers.Default) {
            withTimeout(5_000) {
                val parent = Job(coroutineContext[Job])
                val requests = Channel<TileSpecs>(Channel.UNLIMITED)
                val finishTile = CompletableDeferred<Unit>()
                val scope = CoroutineScope(parent + Dispatchers.Default)
                val engine = CanvasEngine(
                    maxCacheTiles = 20,
                    coroutineScope = scope,
                    tileRenderer = TileRenderer(
                        coroutineScope = scope,
                        getTile = { zoom, row, col ->
                            requests.send(TileSpecs(zoom, row, col))
                            if (row == 0 && col == 3) finishTile.await()
                            TileResult.Success(RasterTile(zoom, row, col, null))
                        },
                        processTile = { it },
                    ),
                )
                try {
                    engine.renderTiles(listOf(TileSpecs(2, 4, -1), TileSpecs(2, -4, 7)), 2)
                    assertEquals(TileSpecs(2, 0, 3), requests.receive())

                    val marker = TileSpecs(2, 1, 1)
                    val latest = listOf(TileSpecs(2, 8, 3), TileSpecs(2, 0, -5), marker)
                    engine.renderTiles(latest, 2)
                    // The marker confirms the renderer received this selection before the first tile finishes.
                    assertEquals(marker, requests.receive())
                    finishTile.complete(Unit)
                    val expected = ActiveTiles(currentZoom = 2, tiles = latest.map { RasterTile(it.zoom, it.row, it.col, null) })
                    while (engine.activeTiles != expected) delay(1)
                    assertEquals(expected, engine.activeTiles)
                    assertTrue(requests.tryReceive().isFailure)
                } finally {
                    parent.cancelAndJoin()
                }
            }
        }
    }

    test("an unchanged selection retries failed repeated tiles on the next update") {
        testEngine {
            val source = TileSpecs(2, 0, 3)
            val visible = listOf(TileSpecs(2, 4, -1), TileSpecs(2, -4, 7))
            engine.renderTiles(visible, 2)
            scheduler.runCurrent()
            assertEquals(listOf(source), takeRequests())

            failTile(source)
            scheduler.runCurrent()
            assertEquals(ActiveTiles(currentZoom = 2), engine.activeTiles)
            assertTrue(takeRequests().isEmpty())

            engine.renderTiles(visible, 2)
            scheduler.runCurrent()
            assertEquals(listOf(source), takeRequests())
            completeTile(RasterTile(2, 0, 3, null))
            scheduler.runCurrent()
            assertEquals(
                ActiveTiles(currentZoom = 2, tiles = visible.map { RasterTile(it.zoom, it.row, it.col, null) }),
                engine.activeTiles,
            )
            engine.renderTiles(visible, 2)
            scheduler.runCurrent()
            assertTrue(takeRequests().isEmpty())
        }
    }

    test("a failed tile preserves its fallback and successful neighbors during a retry") {
        testEngine {
            val parent = RasterTile(1, 0, 0, null)
            val first = RasterTile(2, 0, 0, null)
            val second = RasterTile(2, 0, 1, null)
            engine.renderTiles(listOf(parent.specs()), 1)
            scheduler.runCurrent()
            takeRequests()
            completeTile(parent)
            scheduler.runCurrent()

            val visible = listOf(first.specs(), second.specs())
            engine.renderTiles(visible, 2)
            scheduler.runCurrent()
            assertEquals(visible, takeRequests())
            failTile(first.specs())
            completeTile(second)
            scheduler.runCurrent()
            val fallback = ActiveTiles(currentZoom = 2, tiles = listOf(parent, second))
            assertEquals(fallback, engine.activeTiles)
            assertTrue(takeRequests().isEmpty())

            engine.renderTiles(visible, 2)
            scheduler.runCurrent()
            assertEquals(listOf(first.specs()), takeRequests())
            assertEquals(fallback, engine.activeTiles)
            completeTile(first)
            scheduler.runCurrent()
            assertEquals(ActiveTiles(currentZoom = 2, tiles = listOf(first, second)), engine.activeTiles)
        }
    }

    test("a late failure does not restore or retry a cleared viewport") {
        testEngine {
            val source = TileSpecs(2, 1, 1)
            engine.renderTiles(listOf(source), 2)
            scheduler.runCurrent()
            takeRequests()
            engine.renderTiles(emptyList(), 3)
            scheduler.runCurrent()

            failTile(source)
            scheduler.runCurrent()
            assertEquals(ActiveTiles(currentZoom = 3), engine.activeTiles)
            engine.renderTiles(emptyList(), 3)
            scheduler.runCurrent()
            assertTrue(takeRequests().isEmpty())

            engine.renderTiles(listOf(source), 2)
            scheduler.runCurrent()
            assertEquals(listOf(source), takeRequests())
            completeTile(RasterTile(2, 1, 1, null))
            scheduler.runCurrent()
            assertEquals(ActiveTiles(currentZoom = 2, tiles = listOf(RasterTile(2, 1, 1, null))), engine.activeTiles)
        }
    }

    test("the cache retains its limit while active tiles remain available") {
        testEngine(maxCacheTiles = 2) {
            val tiles = (0..2).map { RasterTile(2, it, it, null) }
            engine.renderTiles(tiles.map { it.specs() }, 2)
            scheduler.runCurrent()
            assertEquals(tiles.map { it.specs() }, takeRequests())
            tiles.forEach { completeTile(it) }
            scheduler.runCurrent()
            assertEquals(ActiveTiles(currentZoom = 2, tiles = tiles), engine.activeTiles)

            engine.renderTiles(emptyList(), 2)
            scheduler.runCurrent()
            engine.renderTiles(tiles.map { it.specs() }, 2)
            scheduler.runCurrent()
            assertEquals(ActiveTiles(currentZoom = 2, tiles = tiles.takeLast(2)), engine.activeTiles)
            assertEquals(listOf(tiles.first()), takeRequests())
        }
    }

    for (startEngine in listOf(false, true)) {
        test("cancellation discards queued and future updates, started=$startEngine") {
            testEngine {
                if (startEngine) {
                    engine.renderTiles(listOf(TileSpecs(2, 1, 1)), 2)
                    scheduler.runCurrent()
                    takeRequests()
                    completeTile(RasterTile(2, 1, 1, null))
                }
                val before = engine.activeTiles
                engine.renderTiles(listOf(TileSpecs(3, 3, 3)), 3)
                job.cancel()
                scheduler.runCurrent()
                job.join()

                engine.renderTiles(listOf(TileSpecs(4, 12, 12)), 4)
                scheduler.runCurrent()
                assertEquals(before, engine.activeTiles)
                assertTrue(takeRequests().isEmpty())
                assertTrue(job.children.none())
            }
        }
    }

    test("concurrent callers and tile workers converge on the final selection") {
        withContext(Dispatchers.Default) {
            withTimeout(5_000) {
                val parent = Job(coroutineContext[Job])
                val scope = CoroutineScope(parent + Dispatchers.Default)
                val engine = CanvasEngine(
                    maxCacheTiles = 20,
                    coroutineScope = scope,
                    tileRenderer = TileRenderer(
                        coroutineScope = scope,
                        getTile = { zoom, row, col ->
                            yield()
                            TileResult.Success(RasterTile(zoom, row, col, null))
                        },
                        processTile = { it },
                    ),
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
    test: suspend CanvasEngineTestFixture.() -> Unit,
) {
    val fixture = CanvasEngineTestFixture(maxCacheTiles)
    try {
        fixture.test()
    } finally {
        fixture.job.cancel()
        fixture.scheduler.runCurrent()
        fixture.job.join()
    }
}

private fun RasterTile.specs() = TileSpecs(zoom, row, col)
