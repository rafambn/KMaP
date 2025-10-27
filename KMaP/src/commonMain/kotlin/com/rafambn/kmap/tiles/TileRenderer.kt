package com.rafambn.kmap.tiles

import com.rafambn.kmap.utils.loopInZoom
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select

class TileRenderer(
    private val getTile: suspend (zoom: Int, row: Int, column: Int) -> TileResult,
    maxCacheTiles: Int,
    coroutineScope: CoroutineScope
) {
    private val _renderedTilesFlow = MutableSharedFlow<Tile>(
        replay = maxCacheTiles,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    @OptIn(ExperimentalCoroutinesApi::class)
    val renderedTilesFlow = _renderedTilesFlow.mapLatest { _ ->
        _renderedTilesFlow.replayCache
    }.stateIn(coroutineScope, SharingStarted.Eagerly, emptyList())

    private val tilesToProcessChannel = Channel<List<TileSpecs>>(capacity = Channel.UNLIMITED)
    private val workerResultSuccessChannel = Channel<TileResult>(capacity = Channel.UNLIMITED)

    init {
        coroutineScope.tilesKernel(tilesToProcessChannel, workerResultSuccessChannel)
    }

    suspend fun renderTiles(tilesSpecs: List<TileSpecs>) {
        tilesToProcessChannel.send(tilesSpecs)
    }

    private fun CoroutineScope.tilesKernel(
        tilesToProcess: ReceiveChannel<List<TileSpecs>>,
        tilesProcessResult: Channel<TileResult>
    ) = launch(Dispatchers.Default + SupervisorJob()) {
        val specsBeingProcessed = mutableListOf<TileSpecs>()
        val tilesBeingProcessed = mutableListOf<TileSpecs>()

        while (isActive) {
            select {
                tilesToProcess.onReceive { tilesToProcess ->
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
                                worker(loopedSpecs, tilesProcessResult)
                            }
                        }
                    }
                }
                tilesProcessResult.onReceive { tileResult ->
                    when (tileResult) {
                        is TileResult.Success -> {
                            _renderedTilesFlow.emit(tileResult.tile)
                            when (tileResult.tile) {
                                is RasterTile -> {
                                    tilesBeingProcessed.remove(tileResult.tile as TileSpecs)
                                }
                            }
                            specsBeingProcessed.removeAll {
                                TileSpecs(
                                    it.zoom,
                                    it.row.loopInZoom(it.zoom),
                                    it.col.loopInZoom(it.zoom)
                                ) == tileResult.tile
                            }
                        }

                        is TileResult.Failure -> {
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

    private fun CoroutineScope.worker(
        tileToProcess: TileSpecs,
        tilesProcessResult: SendChannel<TileResult>
    ) = launch(Dispatchers.Default) {
        try {
            tilesProcessResult.send(getTile(tileToProcess.zoom, tileToProcess.row, tileToProcess.col))
        } catch (ex: Exception) {
            println("Failed to process tile: $ex")
            tilesProcessResult.send(TileResult.Failure(tileToProcess))
        }
    }
}
