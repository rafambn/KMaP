package com.rafambn.kmap.mapSource.tiled.vector

import com.rafambn.kmap.mapSource.tiled.OptimizedVectorTile
import com.rafambn.kmap.mapSource.tiled.TileSpecs
import com.rafambn.kmap.utils.loopInZoom
import com.rafambn.kmap.utils.style.Style
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select

class VectorTileRenderer(
    private val getTile: suspend (zoom: Int, row: Int, column: Int) -> VectorTileResult,
    coroutineScope: CoroutineScope,
    val style: Style
) {
    val tilesToProcessChannel = Channel<List<TileSpecs>>(capacity = Channel.UNLIMITED)
    val tilesProcessedChannel = Channel<OptimizedVectorTile>(capacity = Channel.UNLIMITED)
    private val workerResultChannel = Channel<OptimizedVectorTileResult>(capacity = Channel.UNLIMITED)

    init {
        coroutineScope.launch(Dispatchers.Default + SupervisorJob()) { //TODO add cancellation handler because failed child coroutines might lead to unhandled tiles
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
                            is OptimizedVectorTileResult.Success -> {
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

                            is OptimizedVectorTileResult.Failure -> {
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
        tilesProcessResult: SendChannel<OptimizedVectorTileResult>
    ) = launch(Dispatchers.Default) {
        try {
            val mvTile = getTile(tileToProcess.zoom, tileToProcess.row, tileToProcess.col)
            if (mvTile is VectorTileResult.Failure)
                tilesProcessResult.send(OptimizedVectorTileResult.Failure(tileToProcess))
            else if (mvTile is VectorTileResult.Success)
                tilesProcessResult.send(OptimizedVectorTileResult.Success(optimizeMVTile(mvTile)))
        } catch (ex: Exception) {
            println("Failed to process tile: $ex")
            tilesProcessResult.send(OptimizedVectorTileResult.Failure(tileToProcess))
        }
    }

    private fun optimizeMVTile(mvtile: VectorTileResult.Success): OptimizedVectorTile {
        return OptimizedVectorTile(-1, -1, -1, null)
    }
}
