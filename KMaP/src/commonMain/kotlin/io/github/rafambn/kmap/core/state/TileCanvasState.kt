package io.github.rafambn.kmap.core.state

import io.github.rafambn.kmap.core.TileLayers
import io.github.rafambn.kmap.model.Tile
import io.github.rafambn.kmap.model.TileSpecs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select

class TileCanvasState(
    private val getTile: suspend (zoom: Int, row: Int, column: Int) -> Tile,
) {
    companion object {
        private const val MAX_RENDERED_TILES = 100
        private const val MAX_TRIES = 2
    }

    private val _tileLayersStateFlow = MutableStateFlow(TileLayers())
    val tileLayersStateFlow = _tileLayersStateFlow.asStateFlow()
    private val renderedTiles = mutableListOf<Tile>()
    private val tilesToProcessChannel = Channel<List<TileSpecs>>(capacity = Channel.UNLIMITED)
    private val workerResultSuccessChannel = Channel<Tile>(capacity = Channel.UNLIMITED)

    init {
        CoroutineScope(Dispatchers.Default).tilesKernel(tilesToProcessChannel, workerResultSuccessChannel)
    }

    internal fun onStateChange(
        visibleTiles: List<TileSpecs>,
        zoomLevel: Int
    ) {
        val newFrontLayer = renderedTiles.toList().filter {
            visibleTiles.contains(it.toTileSpecs())
        }
        val tilesToRender = visibleTiles.filter {
            !newFrontLayer.contains(it.toTile())
        }
        if (zoomLevel == _tileLayersStateFlow.value.frontLayerLevel) {
            _tileLayersStateFlow.update {
                it.copy(frontLayer = newFrontLayer)
            }
        } else {
            _tileLayersStateFlow.update {
                it.changeLayer(zoomLevel)
                it.copy(frontLayer = newFrontLayer)
            }
        }
        tilesToProcessChannel.trySend(tilesToRender)
    }

    private fun CoroutineScope.tilesKernel(
        tilesToProcess: ReceiveChannel<List<TileSpecs>>,
        tilesProcessSuccessResult: Channel<Tile>
    ) = launch(Dispatchers.Default) {
        val specsBeingProcessed = mutableSetOf<TileSpecs>()

        while (isActive) {
            select {
                tilesToProcess.onReceive { tilesToProcess ->
                    tilesToProcess.forEach { tileSpecs ->
                        if (!specsBeingProcessed.contains(tileSpecs)) {
                            specsBeingProcessed.add(tileSpecs)
                            worker(tileSpecs, tilesProcessSuccessResult)
                        }
                    }
                }
                tilesProcessSuccessResult.onReceive { tile ->
                    specsBeingProcessed.remove(tile.toTileSpecs())
                    renderedTiles.add(tile)
                    if (renderedTiles.size > MAX_RENDERED_TILES)
                        renderedTiles.removeAt(0)
                    if (tile.zoom == _tileLayersStateFlow.value.frontLayerLevel) {
                        _tileLayersStateFlow.update { tileLayers ->
                            tileLayers.copy(frontLayer = tileLayers.frontLayer.toMutableList().also { it.add(tile) })
                        }
                    }
                    if (tile.zoom == _tileLayersStateFlow.value.backLayerLevel) {
                        _tileLayersStateFlow.update { tileLayers ->
                            tileLayers.copy(backLayer = tileLayers.backLayer.toMutableList().also { it.add(tile) })
                        }
                    }
                }
            }
        }
    }

    private fun CoroutineScope.worker(
        tileToProcess: TileSpecs,
        tilesProcessSuccessResult: SendChannel<Tile>
    ) = launch(Dispatchers.Default) {
        try {
            tilesProcessSuccessResult.send(getTile(tileToProcess.zoom, tileToProcess.row, tileToProcess.col))
        } catch (ex: Exception) {
//            if (tileToProcess.numberOfTries < MAX_TRIES)
//                tilesProcessFailedResult.send(tileToProcess)
//            else
                println("Failed to process tile after $MAX_TRIES attempts: $ex")
        }
    }
}