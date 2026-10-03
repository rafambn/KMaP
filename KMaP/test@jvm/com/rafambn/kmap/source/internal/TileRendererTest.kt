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
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.test.assertEquals
import kotlin.test.assertTrue

val TileRendererTest by testSuite {
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
                    renderer.tilesToProcessChannel.send(listOf(source, source))
                    assertEquals(source, requests.receive())

                    renderer.tilesToProcessChannel.send(listOf(source, source, marker))
                    assertEquals(marker, requests.receive())
                    assertEquals(marker, renderer.tilesProcessedChannel.receive())
                    assertTrue(requests.tryReceive().isFailure)

                    finishTile.complete(Unit)
                    assertEquals(source, renderer.tilesProcessedChannel.receive())
                    renderer.tilesToProcessChannel.send(listOf(source))
                    assertEquals(source, requests.receive())
                    assertEquals(source, renderer.tilesProcessedChannel.receive())
                    assertTrue(requests.tryReceive().isFailure)
                    assertTrue(renderer.tilesProcessedChannel.tryReceive().isFailure)
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
                    val failedWorkerFinished = CompletableDeferred<Unit>()
                    val source = TileSpecs(3, 2, 4)
                    val marker = TileSpecs(3, 0, 0)
                    val renderer = TileRenderer(
                        coroutineScope = CoroutineScope(parent + Dispatchers.Default),
                        getTile = { zoom, row, col ->
                            val specs = TileSpecs(zoom, row, col)
                            if (specs == source && attempts.incrementAndGet() == 1) {
                                currentCoroutineContext()[Job]!!.invokeOnCompletion { failedWorkerFinished.complete(Unit) }
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
                        renderer.tilesToProcessChannel.send(listOf(source))
                        failedWorkerFinished.await()
                        // The worker queued its failure before this marker; receiving the marker drains that failure.
                        renderer.tilesToProcessChannel.send(listOf(marker))
                        assertEquals(marker, renderer.tilesProcessedChannel.receive())

                        renderer.tilesToProcessChannel.send(listOf(source))
                        assertEquals(source, renderer.tilesProcessedChannel.receive())
                        assertEquals(2, attempts.get())
                        assertTrue(renderer.tilesProcessedChannel.tryReceive().isFailure)
                    } finally {
                        parent.cancelAndJoin()
                    }
                }
            }
        }
    }
}
