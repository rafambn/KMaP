package com.rafambn.kmap.components.canvas.tiled

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
    private val getTile: suspend (zoom: Int, row: Int, column: Int) -> TileRenderResult,
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
    private val workerResultSuccessChannel = Channel<TileRenderResult>(capacity = Channel.UNLIMITED)

    init {
        coroutineScope.tilesKernel(tilesToProcessChannel, workerResultSuccessChannel)
    }

    suspend fun renderTiles(tilesSpecs: List<TileSpecs>) {
        tilesToProcessChannel.send(tilesSpecs)
    }

    private fun CoroutineScope.tilesKernel(
        tilesToProcess: ReceiveChannel<List<TileSpecs>>,
        tilesProcessResult: Channel<TileRenderResult>
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
                        is TileRenderResult.Success -> {
                            _renderedTilesFlow.emit(tileResult.tile)
                            tilesBeingProcessed.remove(tileResult.tile)
                            specsBeingProcessed.removeAll {
                                TileSpecs(
                                    it.zoom,
                                    it.row.loopInZoom(it.zoom),
                                    it.col.loopInZoom(it.zoom)
                                ) == tileResult.tile
                            }
                        }

                        is TileRenderResult.Failure -> {
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
        tilesProcessResult: SendChannel<TileRenderResult>
    ) = launch(Dispatchers.Default) {
        try {
            tilesProcessResult.send(getTile(tileToProcess.zoom, tileToProcess.row, tileToProcess.col))
        } catch (ex: Exception) {
            println("Failed to process tile: $ex")
            tilesProcessResult.send(TileRenderResult.Failure(tileToProcess))
        }
    }
}
