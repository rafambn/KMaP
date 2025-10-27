package com.rafambn.kmap.tiles

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.rafambn.kmap.core.ViewPort
import com.rafambn.kmap.mapProperties.MapProperties
import com.rafambn.kmap.utils.loopInZoom
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class CanvasEngine(
    maxCacheTiles: Int = 20,
    getTile: suspend (zoom: Int, row: Int, column: Int) -> TileResult,
    val coroutineScope: CoroutineScope
) {

    var tileLayers by mutableStateOf(TileLayers())
    val tileRenderer = TileRenderer(getTile, maxCacheTiles, coroutineScope)

    init {
        coroutineScope.launch {
            tileRenderer.renderedTilesFlow.collect {
                it.forEach {
                    tileLayers.insertNewTileBitmap(it)
                }
                tileLayers = tileLayers.copy(trigger = tileLayers.trigger * -1)
                //TODO (improve lifecycle of tileLayers, improve tiles to use sets instead of lists)
            }
        }
    }

    fun renderTile(viewPort: ViewPort, zoomLevel: Int, mapProperties: MapProperties) {
        val visibleTiles = TileFinder.getVisibleTilesForLevel(
            viewPort,
            zoomLevel,
            mapProperties.outsideTiles,
            mapProperties.tileSize
        )

        val renderedTilesCache = tileRenderer.renderedTilesFlow.value
        if (zoomLevel != tileLayers.frontLayer.level)
            tileLayers.changeLayer(zoomLevel)

        val newFrontLayer = mutableListOf<Tile>()
        val tilesToRender = mutableListOf<TileSpecs>()
        visibleTiles.forEach { tileSpecs ->
            val foundInFrontLayer = tileLayers.frontLayer.tiles.find { it == tileSpecs }
            val foundInRenderedTiles = renderedTilesCache.find {
                it == TileSpecs(
                    tileSpecs.zoom,
                    tileSpecs.row.loopInZoom(tileSpecs.zoom),
                    tileSpecs.col.loopInZoom(tileSpecs.zoom)
                )
            }

            when {
                foundInFrontLayer != null -> {
                    newFrontLayer.add(foundInFrontLayer)
                }

                foundInRenderedTiles != null -> {
                    when (foundInRenderedTiles) {
                        is RasterTile -> {
                            newFrontLayer.add(RasterTile(tileSpecs.zoom, tileSpecs.row, tileSpecs.col, foundInRenderedTiles.imageBitmap))
                        }

                        is VectorTile -> {
                            newFrontLayer.add(VectorTile(tileSpecs.zoom, tileSpecs.row, tileSpecs.col, foundInRenderedTiles.mvtile))
                        }
                    }
                }

                else -> {
                    when (foundInRenderedTiles) {
                        is RasterTile -> {
                            newFrontLayer.add(RasterTile(tileSpecs.zoom, tileSpecs.row, tileSpecs.col, null))
                        }

                        is VectorTile -> {
                            newFrontLayer.add(VectorTile(tileSpecs.zoom, tileSpecs.row, tileSpecs.col, null))
                        }
                    }
                    tilesToRender.add(tileSpecs)
                }
            }
        }
        tileLayers.updateFrontLayerTiles(newFrontLayer)
        coroutineScope.launch { tileRenderer.renderTiles(tilesToRender) }
    }
}
