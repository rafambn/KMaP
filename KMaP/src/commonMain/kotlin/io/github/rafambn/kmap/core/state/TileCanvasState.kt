package io.github.rafambn.kmap.core.state

import io.github.rafambn.kmap.core.TileLayers
import io.github.rafambn.kmap.model.ResultTile
import io.github.rafambn.kmap.model.Tile
import io.github.rafambn.kmap.model.TileResult
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
    private val getTile: suspend (zoom: Int, row: Int, column: Int) -> ResultTile,
    private val maxTries: Int,
    private val maxCacheTiles: Int
) {
    private val _tileLayersStateFlow = MutableStateFlow(TileLayers())
    val tileLayersStateFlow = _tileLayersStateFlow.asStateFlow()
    private val renderedTiles = mutableListOf<Tile>()
    private val tilesToProcessChannel = Channel<List<TileSpecs>>(capacity = Channel.UNLIMITED)
    private val workerResultSuccessChannel = Channel<ResultTile>(capacity = Channel.UNLIMITED)

    init {
        CoroutineScope(Dispatchers.Default).tilesKernel(tilesToProcessChannel, workerResultSuccessChannel)
    }

    internal fun onStateChange(
        visibleTiles: List<TileSpecs>,
        zoomLevel: Int
    ) {
        if (zoomLevel != _tileLayersStateFlow.value.frontLayerLevel)
            _tileLayersStateFlow.update {
                it.changeLayer(zoomLevel)
                it.copy()
            }
        val newFrontLayer = _tileLayersStateFlow.value.frontLayer.toMutableList()
            .also { if (renderedTiles.size != 0) it.addAll(renderedTiles) }
            .toSet()
            .toList()
            .filter { visibleTiles.contains(it.toTileSpecs()) }
        val tilesToRender = visibleTiles.filter {
            !newFrontLayer.contains(it.toTile())
        }
        println(renderedTiles.size)
        _tileLayersStateFlow.update {
            it.copy(frontLayer = newFrontLayer)
        }
        tilesToProcessChannel.trySend(tilesToRender)
    }

    private fun CoroutineScope.tilesKernel(
        tilesToProcess: ReceiveChannel<List<TileSpecs>>,
        tilesProcessResult: Channel<ResultTile>
    ) = launch(Dispatchers.Default) {
        val specsBeingProcessed = mutableSetOf<TileSpecs>()

        while (isActive) {
            select {
                tilesToProcess.onReceive { tilesToProcess ->
                    tilesToProcess.forEach { tileSpecs ->
                        if (!specsBeingProcessed.contains(tileSpecs)) {
                            specsBeingProcessed.add(tileSpecs)
                            worker(tileSpecs, tilesProcessResult)
                        }
                    }
                }
                tilesProcessResult.onReceive { tileResult ->
                    when (tileResult.result) {
                        TileResult.SUCCESS -> {
                            val tile = tileResult.tile!!
                            if (maxCacheTiles != 0) {
                                renderedTiles.add(tile)
                                if (renderedTiles.size > maxCacheTiles)
                                    renderedTiles.removeAt(0)
                            }
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
                            specsBeingProcessed.remove(tile.toTileSpecs())
                        }

                        TileResult.FAILURE -> {
                            specsBeingProcessed.remove(tileResult.tile!!.toTileSpecs())
                        }
                    }
                }
            }
        }
    }

    private fun CoroutineScope.worker(
        tileToProcess: TileSpecs,
        tilesProcessSuccessResult: SendChannel<ResultTile>
    ) = launch(Dispatchers.Default) {
        var resultTile: ResultTile
        var numberOfTries = 0
        while (numberOfTries < maxTries) {
            try {
                resultTile = getTile(tileToProcess.zoom, tileToProcess.row, tileToProcess.col)
                if (resultTile.result == TileResult.SUCCESS) {
                    tilesProcessSuccessResult.send(resultTile)
                    break
                }
            } catch (ex: Exception) {
                println("Failed to process tile on attempt ${numberOfTries}: $ex")
                numberOfTries++
            }
        }
        if (numberOfTries == maxTries) {
            println("Failed to process tile after $maxTries attempts")
            tilesProcessSuccessResult.send(ResultTile(tileToProcess.toTile(), TileResult.FAILURE))
        }
    }
}