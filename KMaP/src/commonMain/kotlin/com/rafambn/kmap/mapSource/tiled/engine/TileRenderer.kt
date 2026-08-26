package com.rafambn.kmap.mapSource.tiled.engine

import com.rafambn.kmap.mapSource.tiled.TileResult
import com.rafambn.kmap.mapSource.tiled.tiles.Tile
import com.rafambn.kmap.mapSource.tiled.tiles.TileSpecs
import com.rafambn.kmap.utils.loopInZoom
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.select

class TileRenderer<T : Tile, R : Tile>(
    coroutineScope: CoroutineScope,
    private val getTile: suspend (zoom: Int, row: Int, column: Int) -> TileResult<T>,
    private val processTile: suspend (T) -> R
) {
    val tilesToProcessChannel = Channel<List<TileSpecs>>(capacity = CONFLATED)
    val tilesProcessedChannel = Channel<R>(capacity = MAX_CONCURRENT_REQUESTS)
    private val workerResultChannel = Channel<TileResult<R>>(capacity = MAX_CONCURRENT_REQUESTS)

    init {
        coroutineScope.launch(Dispatchers.Default) {
            val pending = ArrayDeque<TileSpecs>()
            val inFlight = mutableSetOf<TileSpecs>()

            fun startPendingRequests() {
                while (inFlight.size < MAX_CONCURRENT_REQUESTS && pending.isNotEmpty()) {
                    val specs = pending.removeFirst()
                    inFlight.add(specs)
                    launchTileRequest(specs)
                }
            }

            while (isActive) {
                select {
                    tilesToProcessChannel.onReceive { tilesToProcess ->
                        pending.clear()
                        tilesToProcess.forEach { specs ->
                            val normalized = TileSpecs(
                                specs.zoom,
                                specs.row.loopInZoom(specs.zoom),
                                specs.col.loopInZoom(specs.zoom),
                            )
                            if (normalized !in inFlight && normalized !in pending) {
                                pending.addLast(normalized)
                            }
                        }
                        startPendingRequests()
                    }
                    workerResultChannel.onReceive { tileResult ->
                        when (tileResult) {
                            is TileResult.Success -> {
                                val finishedSpecs = TileSpecs(
                                    tileResult.tile.zoom,
                                    tileResult.tile.row,
                                    tileResult.tile.col,
                                )
                                inFlight.remove(finishedSpecs)
                                tilesProcessedChannel.send(tileResult.tile)
                            }

                            is TileResult.Failure -> {
                                inFlight.remove(tileResult.specs)
                            }
                        }
                        startPendingRequests()
                    }
                }
            }
        }
    }

    private fun CoroutineScope.launchTileRequest(tileToProcess: TileSpecs) = launch {
        try {
            when (val tileResult = getTile(tileToProcess.zoom, tileToProcess.row, tileToProcess.col)) {
                is TileResult.Success -> {
                    val processed = processTile(tileResult.tile)
                    workerResultChannel.send(TileResult.Success(processed))
                }
                is TileResult.Failure -> {
                    workerResultChannel.send(tileResult)
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (ex: Exception) {
            println("Failed to process tile: $ex")
            workerResultChannel.send(TileResult.Failure(tileToProcess))
        }
    }

    private companion object {
        const val MAX_CONCURRENT_REQUESTS = 4
    }
}
