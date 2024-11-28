package com.rafambn.kmap.core.state

import com.rafambn.kmap.core.TileLayers
import com.rafambn.kmap.model.Tile
import com.rafambn.kmap.utils.TileRenderResult
import com.rafambn.kmap.model.TileSpecs
import com.rafambn.kmap.utils.loopInZoom
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select

class TileCanvasState( //TODO move this to mapState
    private val getTile: suspend (zoom: Int, row: Int, column: Int) -> TileRenderResult,
    private val maxTries: Int,
    private val maxCacheTiles: Int
) {
    private val _tileLayersStateFlow = MutableStateFlow(TileLayers())
    val tileLayersStateFlow = _tileLayersStateFlow.asStateFlow()
    private var renderedTiles = mutableSetOf<Tile>()
    private val tilesToProcessChannel = Channel<List<TileSpecs>>(capacity = Channel.UNLIMITED)
    private val workerResultSuccessChannel = Channel<TileRenderResult>(capacity = Channel.UNLIMITED)

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
        val frontLayerCopy = tileLayersStateFlow.value.frontLayer.toList()
        val renderedTilesCopy = renderedTiles.toList()
        val newFrontLayer = mutableListOf<Tile>()
        val tilesToRender = mutableListOf<TileSpecs>()
        visibleTiles.forEach { tile ->
            val foundInFrontLayer = frontLayerCopy.find { it == tile }
            val foundInRenderedTiles = renderedTilesCopy.find { it == TileSpecs(tile.zoom, tile.row.loopInZoom(tile.zoom), tile.col.loopInZoom(tile.zoom)) }

            when {
                foundInFrontLayer != null -> {
                    newFrontLayer.add(foundInFrontLayer)
                }
                foundInRenderedTiles != null -> {
                    newFrontLayer.add(Tile(tile.zoom, tile.row, tile.col, foundInRenderedTiles.imageBitmap))
                }
                else -> {
                    tilesToRender.add(tile)
                }
            }
        }
        _tileLayersStateFlow.update { it.copy(frontLayer = newFrontLayer) }
        tilesToProcessChannel.trySend(tilesToRender)
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
                        val tileSpecs = TileSpecs(
                            specs.zoom,
                            specs.row.loopInZoom(specs.zoom),
                            specs.col.loopInZoom(specs.zoom)
                        )
                        if (!specsBeingProcessed.contains(specs)) {
                            specsBeingProcessed.add(specs)
                            if (!tilesBeingProcessed.contains(tileSpecs)) {
                                tilesBeingProcessed.add(tileSpecs)
                                worker(tileSpecs, tilesProcessResult)
                            }
                        }
                    }
                }
                tilesProcessResult.onReceive { tileResult ->
                    when (tileResult) {
                        is TileRenderResult.Success -> {
                            if (maxCacheTiles != 0) {
                                renderedTiles.add(tileResult.tile)
                                if (renderedTiles.size > maxCacheTiles) {
                                    val mutableRendered = renderedTiles.toMutableList()
                                    mutableRendered.removeAt(0)
                                    renderedTiles = mutableRendered.toMutableSet()
                                }
                            }
                            if (tileResult.tile.zoom == _tileLayersStateFlow.value.frontLayerLevel) {
                                _tileLayersStateFlow.update { tileLayers ->
                                    tileLayers.copy(frontLayer = tileLayers.frontLayer.toMutableList().also { tileMutableList ->
                                        tileMutableList.add(tileResult.tile)
                                        specsBeingProcessed.remove(tileResult.tile)
                                        specsBeingProcessed.forEach {
                                            if (TileSpecs(it.zoom, it.row.loopInZoom(it.zoom), it.col.loopInZoom(it.zoom)) == tileResult.tile)
                                                tileMutableList.add(Tile(it.zoom, it.row, it.col, tileResult.tile.imageBitmap))
                                        }
                                    })
                                }
                            }
                            if (tileResult.tile.zoom == _tileLayersStateFlow.value.backLayerLevel) {
                                _tileLayersStateFlow.update { tileLayers ->
                                    tileLayers.copy(backLayer = tileLayers.backLayer.toMutableList().also { tileMutableList ->
                                        tileMutableList.add(tileResult.tile)
                                        specsBeingProcessed.remove(tileResult.tile)
                                        specsBeingProcessed.forEach {
                                            if (TileSpecs(it.zoom, it.row.loopInZoom(it.zoom), it.col.loopInZoom(it.zoom)) == tileResult.tile)
                                                tileMutableList.add(Tile(it.zoom, it.row, it.col, tileResult.tile.imageBitmap))
                                        }
                                    })
                                }
                            }
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

                        else -> {}
                    }
                }
            }
        }
    }

    private fun CoroutineScope.worker(
        tileToProcess: TileSpecs,
        tilesProcessSuccessResult: SendChannel<TileRenderResult>
    ) = launch(Dispatchers.Default) {
        var numberOfTries = 0
        while (numberOfTries < maxTries) {
            try {
                when (val resultTile = getTile(tileToProcess.zoom, tileToProcess.row, tileToProcess.col)) {
                    is TileRenderResult.Success -> {
                        tilesProcessSuccessResult.send(resultTile)
                        break
                    }

                    is TileRenderResult.Failure -> {
                        numberOfTries++
                    }
                }
            } catch (ex: Exception) {
                println("Failed to process tile on attempt ${numberOfTries}: $ex")
                numberOfTries++
            }
        }
        if (numberOfTries == maxTries) {
            println("Failed to process tile after $maxTries attempts")
            tilesProcessSuccessResult.send(TileRenderResult.Failure(tileToProcess))
        }
    }
}