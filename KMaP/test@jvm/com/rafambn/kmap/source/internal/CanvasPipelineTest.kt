@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class, kotlinx.serialization.ExperimentalSerializationApi::class)
package com.rafambn.kmap.source.internal

import com.rafambn.kmap.components.parameters.RasterCanvasParameters
import com.rafambn.kmap.mvttile.*
import com.rafambn.kmap.source.*
import com.rafambn.kmap.source.preparation.*
import com.rafambn.kmap.style.compiled.CompiledStyle
import de.infix.testBalloon.framework.core.testSuite
import kotlinx.coroutines.*
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.test.*

val CanvasPipelineTest by testSuite {
    test("registration returns an empty snapshot until the owner processes it") {
        testPipeline {
            engine.renderTiles(listOf(TileSpecs(2, 1, 1)), 2)
            engine.refreshCanvas(listOf(vector()))
            assertEquals(ActiveTiles(), engine.getActiveTiles(0))
            scheduler.runCurrent()
            assertEquals(1, requests.size)
            complete()
            scheduler.runCurrent()
            assertEquals(1, engine.getStatus(0).workerTiles)
            assertEquals(listOf(TileSpecs(2, 1, 1)), engine.getActiveTiles(0).tiles.map { TileSpecs(it.zoom, it.row, it.col) })
        }
    }
    test("duplicate IDs and negative cache limits are rejected") {
        testPipeline {
            assertFailsWith<IllegalArgumentException> { engine.refreshCanvas(listOf(vector(), vector())) }
            assertFailsWith<IllegalArgumentException> {
                engine.refreshCanvas(listOf(RasterCanvasParameters(0, maxCacheTiles = -1, tileSource = { _, _, _ -> error("unused") })))
            }
        }
    }
    test("one normalized preparation serves repeated world copies with shared geometry") {
        testPipeline {
            engine.refreshCanvas(listOf(vector()))
            val display = listOf(TileSpecs(2, 4, -1), TileSpecs(2, -4, 7))
            engine.renderTiles(display, 2)
            scheduler.runCurrent()
            assertEquals(listOf(TileSpecs(2, 0, 3)), requests.map { it.specs })
            complete()
            scheduler.runCurrent()
            val tiles = engine.getActiveTiles(0).tiles.map { assertIs<OptimizedVectorTile>(it) }
            assertEquals(display, tiles.map { TileSpecs(it.zoom, it.row, it.col) })
            assertSame(tiles[0].optimizedTile, tiles[1].optimizedTile)
        }
    }
    test("paint only style changes reuse prepared data without fetching or preparing") {
        testPipeline {
            val style = pipelineStyle()
            var fetched = 0
            val source: suspend (Int, Int, Int) -> TileResult<VectorTile> = { z, r, c ->
                fetched++
                TileResult.Success(EncodedVectorTile(z, r, c, pipelineBytes()))
            }
            engine.refreshCanvas(listOf(vector(style = style, source = source)))
            engine.renderTiles(listOf(TileSpecs(0, 0, 0)), 0)
            scheduler.runCurrent()
            complete()
            scheduler.runCurrent()
            val original = engine.getActiveTiles(0).tiles.single()
            engine.refreshCanvas(listOf(vector(style = style.copy(glyphs = emptyMap()), source = source)))
            scheduler.runCurrent()
            assertSame(original, engine.getActiveTiles(0).tiles.single())
            assertEquals(1, fetched)
            assertEquals(1, requests.size)
        }
    }
    test("changed filters reuse raw bytes and obsolete style replies cannot replace current results") {
        testPipeline {
            var fetched = 0
            val source: suspend (Int, Int, Int) -> TileResult<VectorTile> = { z, r, c ->
                fetched++
                TileResult.Success(EncodedVectorTile(z, r, c, pipelineBytes()))
            }
            engine.refreshCanvas(listOf(vector(source = source)))
            engine.renderTiles(listOf(TileSpecs(0, 0, 0)), 0)
            scheduler.runCurrent()
            val old = requests.single()
            engine.refreshCanvas(listOf(vector(style = pipelineStyle(",\"filter\":[\"==\",[\"get\",\"kind\"],\"water\"]"), source = source)))
            scheduler.runCurrent()
            val current = requests.last()
            assertNotEquals(old.styleRevision, current.styleRevision)
            complete(current)
            scheduler.runCurrent()
            val tile = assertIs<OptimizedVectorTile>(engine.getActiveTiles(0).tiles.single())
            assertTrue(tile.optimizedTile!!.layerFeatures.getValue("places").isEmpty())
            complete(old)
            scheduler.runCurrent()
            assertSame(tile, engine.getActiveTiles(0).tiles.single())
            assertEquals(1, fetched)
        }
    }
    test("a style change while fetching applies the new filter when bytes arrive") {
        testPipeline {
            val fetched = CompletableDeferred<TileResult<VectorTile>>()
            engine.refreshCanvas(listOf(vector(source = { _, _, _ -> fetched.await() })))
            engine.renderTiles(listOf(TileSpecs(0, 0, 0)), 0)
            scheduler.runCurrent()
            val style = pipelineStyle(",\"filter\":[\"==\",1,2]")
            engine.refreshCanvas(listOf(vector(style = style, source = { _, _, _ -> fetched.await() })))
            scheduler.runCurrent()
            fetched.complete(TileResult.Success(EncodedVectorTile(0, 0, 0, pipelineBytes())))
            scheduler.runCurrent()
            assertEquals(style.preparation, requests.single().style)
            complete()
            scheduler.runCurrent()
            assertTrue(assertIs<OptimizedVectorTile>(engine.getActiveTiles(0).tiles.single()).optimizedTile!!.featuresEmpty())
        }
    }
    test("invalidation survives a conflated registration and rejects delayed old preparations") {
        testPipeline {
            var content = 1UL
            val source: suspend (Int, Int, Int) -> TileResult<VectorTile> = { z, r, c ->
                TileResult.Success(EncodedVectorTile(z, r, c, pipelineBytes(content)))
            }
            engine.refreshCanvas(listOf(vector(source = source)))
            engine.renderTiles(listOf(TileSpecs(0, 0, 0)), 0)
            scheduler.runCurrent()
            val old = requests.single()
            content = 2UL
            engine.invalidateTiles(0)
            engine.refreshCanvas(listOf(vector(source = source)))
            scheduler.runCurrent()
            val current = requests.last()
            assertNotEquals(old.canvasGeneration, current.canvasGeneration)
            complete(current)
            scheduler.runCurrent()
            complete(old)
            scheduler.runCurrent()
            val tile = assertIs<OptimizedVectorTile>(engine.getActiveTiles(0).tiles.single())
            assertEquals(2UL, tile.optimizedTile!!.layerFeatures.getValue("places").single().id)
        }
    }
    test("lambda replacement without explicit invalidation does not invalidate loaded tiles") {
        testPipeline {
            engine.refreshCanvas(listOf(vector()))
            engine.renderTiles(listOf(TileSpecs(0, 0, 0)), 0)
            scheduler.runCurrent()
            complete()
            scheduler.runCurrent()
            engine.refreshCanvas(listOf(vector(source = { _, _, _ -> error("must retain content") })))
            scheduler.runCurrent()
            assertEquals(1, requests.size)
            assertEquals(1, engine.getActiveTiles(0).tiles.size)
        }
    }
    test("invalidation clears raw and prepared caches only for the selected canvas") {
        testPipeline {
            var content = 1UL
            val fetched = mutableListOf<Int>()
            fun parameters(id: Int) = vector(id, source = { z, r, c ->
                fetched.add(id)
                TileResult.Success(EncodedVectorTile(z, r, c, pipelineBytes(content)))
            })
            engine.refreshCanvas(listOf(parameters(1), parameters(2)))
            engine.renderTiles(listOf(TileSpecs(0, 0, 0)), 0)
            scheduler.runCurrent()
            requests.toList().forEach { complete(it) }
            scheduler.runCurrent()
            val sibling = engine.getActiveTiles(2).tiles.single()

            content = 2UL
            engine.invalidateTiles(1)
            scheduler.runCurrent()
            assertTrue(engine.getActiveTiles(1).tiles.isEmpty())
            assertSame(sibling, engine.getActiveTiles(2).tiles.single())
            assertEquals(listOf(1, 2, 1), fetched)
            complete()
            scheduler.runCurrent()
            assertEquals(2UL, assertIs<OptimizedVectorTile>(engine.getActiveTiles(1).tiles.single())
                .optimizedTile!!.layerFeatures.getValue("places").single().id)

            content = 3UL
            engine.invalidateTiles()
            scheduler.runCurrent()
            assertTrue(engine.getActiveTiles(1).tiles.isEmpty())
            assertTrue(engine.getActiveTiles(2).tiles.isEmpty())
            requests.takeLast(2).forEach { complete(it) }
            scheduler.runCurrent()
            for (id in listOf(1, 2)) assertEquals(3UL,
                assertIs<OptimizedVectorTile>(engine.getActiveTiles(id).tiles.single())
                    .optimizedTile!!.layerFeatures.getValue("places").single().id)
            assertEquals(listOf(1, 2, 1, 1, 2), fetched)
        }
    }
    test("invalidation cancels old fetches and discards a noncancellable delayed result") {
        testPipeline {
            val reply = CompletableDeferred<TileResult<VectorTile>>()
            var fetched = 0
            var cancelled = false
            engine.refreshCanvas(listOf(vector(source = { z, r, c ->
                fetched++
                if (fetched == 1) {
                    try { withContext(NonCancellable) { reply.await() } }
                    finally { cancelled = !currentCoroutineContext().isActive }
                } else TileResult.Success(EncodedVectorTile(z, r, c, pipelineBytes(2UL)))
            })))
            engine.renderTiles(listOf(TileSpecs(0, 0, 0)), 0)
            scheduler.runCurrent()
            engine.invalidateTiles(0)
            scheduler.runCurrent()
            assertEquals(2, fetched)
            complete()
            scheduler.runCurrent()
            reply.complete(TileResult.Success(EncodedVectorTile(0, 0, 0, pipelineBytes())))
            scheduler.runCurrent()
            assertTrue(cancelled)
            assertEquals(1, requests.size)
            assertEquals(2UL, assertIs<OptimizedVectorTile>(engine.getActiveTiles(0).tiles.single())
                .optimizedTile!!.layerFeatures.getValue("places").single().id)
        }
    }
    test("remove and recreate before the owner runs still rejects the previous generation") {
        testPipeline {
            engine.refreshCanvas(listOf(vector()))
            engine.renderTiles(listOf(TileSpecs(0, 0, 0)), 0)
            scheduler.runCurrent()
            val old = requests.single()
            engine.refreshCanvas(emptyList())
            engine.refreshCanvas(listOf(vector(source = { z, r, c ->
                TileResult.Success(EncodedVectorTile(z, r, c, pipelineBytes(3UL)))
            })))
            scheduler.runCurrent()
            val current = requests.last()
            assertNotEquals(old.canvasGeneration, current.canvasGeneration)
            complete(old)
            scheduler.runCurrent()
            assertTrue(engine.getActiveTiles(0).tiles.isEmpty())
            complete(current)
            scheduler.runCurrent()
            assertEquals(3UL, assertIs<OptimizedVectorTile>(engine.getActiveTiles(0).tiles.single())
                .optimizedTile!!.layerFeatures.getValue("places").single().id)
        }
    }
    test("removing a canvas cancels only its fetch while a sibling remains active") {
        testPipeline {
            val started = CompletableDeferred<Unit>()
            val cancelled = CompletableDeferred<Unit>()
            val removed = RasterCanvasParameters(1, tileSource = { _, _, _ ->
                started.complete(Unit)
                try { awaitCancellation() } finally { cancelled.complete(Unit) }
            })
            engine.refreshCanvas(listOf(removed, vector(2)))
            engine.renderTiles(listOf(TileSpecs(0, 0, 0)), 0)
            scheduler.runCurrent()
            assertTrue(started.isCompleted)
            engine.refreshCanvas(listOf(vector(2)))
            scheduler.runCurrent()
            assertTrue(cancelled.isCompleted)
            assertTrue(parent.isActive)
            complete()
            scheduler.runCurrent()
            assertEquals(1, engine.getActiveTiles(2).tiles.size)
            assertTrue(engine.getActiveTiles(1).tiles.isEmpty())
        }
    }
    test("empty selection stops pending admission and late preparation cannot repopulate it") {
        testPipeline {
            engine.refreshCanvas(listOf(vector()))
            engine.renderTiles((0..9).map { TileSpecs(4, 0, it) }, 4)
            scheduler.runCurrent()
            assertEquals(2, requests.size)
            engine.renderTiles(emptyList(), 5)
            scheduler.runCurrent()
            requests.toList().forEach { complete(it) }
            scheduler.runCurrent()
            assertEquals(2, requests.size)
            assertEquals(ActiveTiles(currentZoom = 5), engine.getActiveTiles(0))
            assertEquals(0, engine.getStatus(0).pendingPreparation)
        }
    }
    test("panning away and back reuses accepted data without restoring an old viewport") {
        testPipeline {
            engine.refreshCanvas(listOf(vector()))
            val first = TileSpecs(2, 0, 0)
            val second = TileSpecs(2, 3, 3)
            engine.renderTiles(listOf(first), 2)
            scheduler.runCurrent()
            val old = requests.single()
            engine.renderTiles(listOf(second), 2)
            scheduler.runCurrent()
            complete(old)
            scheduler.runCurrent()
            assertTrue(engine.getActiveTiles(0).tiles.isEmpty())
            complete(requests.last())
            scheduler.runCurrent()
            assertEquals(second, engine.getActiveTiles(0).tiles.single().let { TileSpecs(it.zoom, it.row, it.col) })
            engine.renderTiles(listOf(first), 2)
            scheduler.runCurrent()
            assertEquals(2, requests.size)
            assertEquals(first, engine.getActiveTiles(0).tiles.single().let { TileSpecs(it.zoom, it.row, it.col) })
        }
    }
    test("vector parent and child fallbacks preserve repeated display coordinates") {
        testPipeline {
            engine.refreshCanvas(listOf(vector()))
            val parentDisplay = TileSpecs(1, -1, 2)
            val childDisplay = TileSpecs(2, -1, 5)
            engine.renderTiles(listOf(parentDisplay), 1)
            scheduler.runCurrent()
            complete()
            scheduler.runCurrent()
            engine.renderTiles(listOf(childDisplay), 2)
            scheduler.runCurrent()
            assertEquals(parentDisplay, engine.getActiveTiles(0).tiles.single().let { TileSpecs(it.zoom, it.row, it.col) })
            complete()
            scheduler.runCurrent()
            assertEquals(childDisplay, engine.getActiveTiles(0).tiles.single().let { TileSpecs(it.zoom, it.row, it.col) })
        }
    }
    test("malformed preparation fails once and preserves successful neighbors and fallbacks") {
        testPipeline {
            engine.refreshCanvas(listOf(vector()))
            engine.renderTiles(listOf(TileSpecs(1, 0, 0)), 1)
            scheduler.runCurrent()
            complete()
            scheduler.runCurrent()
            engine.renderTiles(listOf(TileSpecs(2, 0, 0), TileSpecs(2, 0, 1)), 2)
            scheduler.runCurrent()
            val failed = requests[1]
            fail(failed, IllegalArgumentException("malformed geometry"))
            complete(requests[2])
            scheduler.runCurrent()
            val active = engine.getActiveTiles(0)
            assertEquals(listOf(1, 2), active.tiles.map { it.zoom })
            assertEquals("preparation", engine.getStatus(0).failures.single().stage)
            repeat(10) { engine.renderTiles(listOf(TileSpecs(2, 0, 0), TileSpecs(2, 0, 1)), 2); scheduler.runCurrent() }
            assertEquals(3, requests.size)
            assertEquals(active, engine.getActiveTiles(0))
        }
    }
    test("worker failure replaces the worker and retries current work once with a new request ID") {
        testPipeline {
            engine.refreshCanvas(listOf(vector()))
            engine.renderTiles(listOf(TileSpecs(0, 0, 0)), 0)
            scheduler.runCurrent()
            val old = requests.single()
            fail(old, WorkerFailure("worker crashed"))
            scheduler.runCurrent()
            val retry = requests.last()
            assertNotEquals(old.requestId, retry.requestId)
            assertEquals(2, createdWorkers)
            assertEquals(1, closedWorkers)
            assertEquals(old.style, retry.style)
            complete(retry)
            scheduler.runCurrent()
            assertEquals(1, engine.getActiveTiles(0).tiles.size)
        }
    }
    test("repeated worker failure is visible and cannot create an infinite retry loop") {
        testPipeline {
            engine.refreshCanvas(listOf(vector()))
            engine.renderTiles(listOf(TileSpecs(0, 0, 0)), 0)
            scheduler.runCurrent()
            fail(error = WorkerFailure("asset missing"))
            scheduler.runCurrent()
            fail(error = WorkerFailure("asset still missing"))
            scheduler.runCurrent()
            repeat(10) { engine.renderTiles(listOf(TileSpecs(0, 0, 0)), 0); scheduler.runCurrent() }
            assertEquals(2, requests.size)
            assertEquals("worker", engine.getStatus(0).failures.single().stage)
            assertEquals(2, engine.getStatus(0).failures.single().attempts)
        }
    }
    test("six shared fetches bound admission while center tiles and sibling canvases get priority") {
        testPipeline {
            val fetched = mutableListOf<Pair<Int, TileSpecs>>()
            val pending = CompletableDeferred<TileResult<RasterTile>>()
            fun raster(id: Int) = RasterCanvasParameters(id, tileSource = { z, r, c ->
                fetched.add(id to TileSpecs(z, r, c))
                pending.await()
            })
            engine.refreshCanvas(listOf(raster(1), raster(2)))
            engine.renderTiles((0..19).map { TileSpecs(5, 0, it) }, 5)
            scheduler.runCurrent()
            assertEquals(6, fetched.size)
            assertEquals(listOf(1, 2, 1, 2, 1, 2), fetched.map { it.first })
            assertEquals(setOf(9, 10), fetched.take(4).map { it.second.col }.toSet())
            assertEquals(3, engine.getStatus(1).fetching)
            assertEquals(3, engine.getStatus(2).fetching)
        }
    }
    test("slow processing bounds prepared queues and shared CPU admission during viewport churn") {
        testPipeline {
            var fetched = 0
            engine.refreshCanvas(listOf(vector(source = { z, r, c ->
                fetched++
                TileResult.Success(EncodedVectorTile(z, r, c, pipelineBytes()))
            })))
            repeat(20) { offset ->
                engine.renderTiles((0..20).map { TileSpecs(8, offset, it) }, 8)
                scheduler.runCurrent()
                assertTrue(engine.getStatus(0).pendingPreparation <= 8)
                assertTrue(engine.getStatus(0).preparing <= 2)
            }
            assertEquals(2, requests.size)
            assertTrue(fetched <= 2 + 20 * 8)
            assertEquals(2, createdWorkers)
        }
    }
    test("removing obsolete CPU work retains capacity until its terminal reply") {
        testPipeline {
            engine.refreshCanvas(listOf(vector()))
            engine.renderTiles(listOf(TileSpecs(2, 0, 0), TileSpecs(2, 0, 1)), 2)
            scheduler.runCurrent()
            val old = requests.toList()
            engine.refreshCanvas(emptyList())
            engine.refreshCanvas(listOf(vector(2)))
            scheduler.runCurrent()
            assertEquals(2, requests.size)
            complete(old[0])
            scheduler.runCurrent()
            assertEquals(3, requests.size)
            assertEquals(2, createdWorkers)
        }
    }
    test("closing the map terminates workers and owned fetches while preserving the parent scope") {
        testPipeline {
            val cancelled = CompletableDeferred<Unit>()
            val raster = RasterCanvasParameters(1, tileSource = { _, _, _ ->
                try { awaitCancellation() } finally { cancelled.complete(Unit) }
            })
            engine.refreshCanvas(listOf(raster, vector(2)))
            engine.renderTiles(listOf(TileSpecs(0, 0, 0)), 0)
            scheduler.runCurrent()
            engine.close()
            scheduler.runCurrent()
            assertTrue(cancelled.isCompleted)
            assertEquals(createdWorkers, closedWorkers)
            assertTrue(parent.isActive)
            assertTrue(parent.children.none())
            engine.refreshCanvas(listOf(vector(3)))
            engine.renderTiles(listOf(TileSpecs(0, 0, 0)), 0)
            scheduler.runCurrent()
            assertTrue(engine.getActiveTiles(3).tiles.isEmpty())
        }
    }
    test("parent cancellation closes every worker") {
        testPipeline {
            engine.refreshCanvas(listOf(vector()))
            engine.renderTiles(listOf(TileSpecs(2, 0, 0), TileSpecs(2, 0, 1)), 2)
            scheduler.runCurrent()
            parent.cancel()
            scheduler.runCurrent()
            assertEquals(createdWorkers, closedWorkers)
            assertTrue(parent.children.none())
        }
    }
    test("decoded custom styles expose their compatibility path and raster data bypasses workers") {
        testPipeline {
            val decoded = ProtoBuf.decodeFromByteArray(RawMVTile.serializer(), pipelineBytes()).parse()
            engine.refreshCanvas(listOf(
                vector(style = CompiledStyle(emptyList()), source = { z, r, c -> TileResult.Success(VectorTile(z, r, c, decoded)) }),
                RasterCanvasParameters(1) { z, r, c -> TileResult.Success(RasterTile(z, r, c, null)) },
            ))
            engine.renderTiles(listOf(TileSpecs(0, 0, 0)), 0)
            scheduler.runCurrent()
            assertEquals(0, createdWorkers)
            assertEquals(1, engine.getStatus(0).compatibilityTiles)
            assertIs<RasterTile>(engine.getActiveTiles(1).tiles.single())
        }
    }
    test("encoded sources require a portable style and never silently prepare on the owner") {
        testPipeline {
            engine.refreshCanvas(listOf(vector(style = CompiledStyle(emptyList()))))
            engine.renderTiles(listOf(TileSpecs(0, 0, 0)), 0)
            scheduler.runCurrent()
            assertEquals(0, createdWorkers)
            assertEquals(0, engine.getStatus(0).compatibilityTiles)
            assertEquals("preparation", engine.getStatus(0).failures.single().stage)
        }
    }
    test("cache resizing under an existing ID retains active content and trims stored bytes") {
        testPipeline {
            val source: suspend (Int, Int, Int) -> TileResult<VectorTile> = { z, r, c ->
                TileResult.Success(EncodedVectorTile(z, r, c, pipelineBytes()))
            }
            val style = pipelineStyle()
            engine.refreshCanvas(listOf(vector(style = style, source = source)))
            engine.renderTiles(listOf(TileSpecs(0, 0, 0)), 0)
            scheduler.runCurrent()
            complete()
            scheduler.runCurrent()
            assertTrue(engine.getStatus(0).rawCacheBytes > 0)
            val original = engine.getActiveTiles(0).tiles.single()
            engine.refreshCanvas(listOf(com.rafambn.kmap.components.parameters.VectorCanvasParameters(
                0, maxCacheTiles = 0, tileSource = source, style = style,
            )))
            scheduler.runCurrent()
            assertSame(original, engine.getActiveTiles(0).tiles.single())
            assertEquals(0, engine.getStatus(0).rawCacheBytes)
            engine.renderTiles(emptyList(), 0)
            scheduler.runCurrent()
            engine.renderTiles(listOf(TileSpecs(0, 0, 0)), 0)
            scheduler.runCurrent()
            assertEquals(2, requests.size)
        }
    }
    test("a self-cancelled source releases capacity and waits for a viewport update before retrying") {
        testPipeline {
            var attempts = 0
            engine.refreshCanvas(listOf(vector(source = { _, _, _ ->
                attempts++
                throw CancellationException("source cancelled")
            })))
            engine.renderTiles(listOf(TileSpecs(0, 0, 0)), 0)
            scheduler.runCurrent()
            assertEquals(1, attempts)
            assertEquals(0, engine.getStatus(0).fetching)
            assertEquals("source", engine.getStatus(0).failures.single().stage)
            engine.renderTiles(listOf(TileSpecs(0, 0, 0)), 0)
            scheduler.runCurrent()
            repeat(3) { engine.renderTiles(listOf(TileSpecs(0, 0, 0)), 0); scheduler.runCurrent() }
            assertEquals(2, attempts)
            assertTrue(parent.isActive)
        }
    }
    test("cancelling a fetch before its first suspension still releases its admission slot") {
        testPipeline {
            val blocked = RasterCanvasParameters(0, tileSource = { _, _, _ -> awaitCancellation() })
            engine.refreshCanvas(listOf(blocked))
            engine.renderTiles((0..5).map { TileSpecs(4, 0, it) }, 4)
            scheduler.runCurrent()
            engine.refreshCanvas(emptyList())
            scheduler.runCurrent()
            engine.refreshCanvas(listOf(vector()))
            engine.renderTiles(listOf(TileSpecs(0, 0, 0)), 0)
            scheduler.runCurrent()
            assertEquals(1, requests.size)
        }
    }
}

private fun OptimizedMVTile.featuresEmpty() = layerFeatures.values.all { it.isEmpty() }
