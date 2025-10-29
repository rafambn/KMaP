package com.rafambn.kmap.mapSource.tiled.raster

import com.rafambn.kmap.mapSource.tiled.RasterTile
import com.rafambn.kmap.mapSource.tiled.TileSpecs
import com.rafambn.kmap.utils.loopInZoom
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select

class RasterTileRenderer(
    private val getTile: suspend (zoom: Int, row: Int, column: Int) -> RasterTileResult,
    coroutineScope: CoroutineScope
) {
    val tilesToProcessChannel = Channel<List<TileSpecs>>(capacity = Channel.UNLIMITED)
    val tilesProcessedChannel = Channel<RasterTile>(capacity = Channel.UNLIMITED)
    private val workerResultChannel = Channel<RasterTileResult>(capacity = Channel.UNLIMITED)

    init {
        coroutineScope.launch(Dispatchers.Default + SupervisorJob()) {
            val specsBeingProcessed = mutableListOf<TileSpecs>()
            val tilesBeingProcessed = mutableListOf<TileSpecs>()

            while (isActive) {
                select {
                    tilesToProcessChannel.onReceive { tilesToProcess ->
                        tilesToProcess.forEach { specs ->
                            val loopedSpecs = TileSpecs(
                                specs.zoom,
                                specs.row.loopInZoom(specs.zoom),
                                specs.col.loopInZoom(specs.zoom)
                            )
                            if (!specsBeingProcessed.contains(specs)) {
                                specsBeingProcessed.add(specs)
                                if (!tilesBeingProcessed.contains(loopedSpecs)) {
                                    tilesBeingProcessed.add(loopedSpecs)
                                    worker(loopedSpecs, workerResultChannel)
                                }
                            }
                        }
                    }
                    workerResultChannel.onReceive { tileResult ->
                        when (tileResult) {
                            is RasterTileResult.Success -> {
                                tilesProcessedChannel.send(tileResult.tile)
                                tilesBeingProcessed.remove(tileResult.tile as TileSpecs)
                                specsBeingProcessed.removeAll {
                                    TileSpecs(
                                        it.zoom,
                                        it.row.loopInZoom(it.zoom),
                                        it.col.loopInZoom(it.zoom)
                                    ) == tileResult.tile
                                }
                            }

                            is RasterTileResult.Failure -> {
                                tilesBeingProcessed.remove(tileResult.specs)
                                specsBeingProcessed.removeAll {
                                    TileSpecs(
                                        it.zoom,
                                        it.row.loopInZoom(it.zoom),
                                        it.col.loopInZoom(it.zoom)
                                    ) == tileResult.specs
                                }
                            }
                        }
                    }
                }
            }

        }
    }

    private fun CoroutineScope.worker(
        tileToProcess: TileSpecs,
        tilesProcessResult: SendChannel<RasterTileResult>
    ) = launch(Dispatchers.Default) {
        try {
            tilesProcessResult.send(getTile(tileToProcess.zoom, tileToProcess.row, tileToProcess.col))
        } catch (ex: Exception) {
            println("Failed to process tile: $ex")
            tilesProcessResult.send(RasterTileResult.Failure(tileToProcess))
        }
    }
}
