package com.rafambn.kmap.mapSource.tiled.raster

import androidx.compose.runtime.mutableStateOf
import com.rafambn.kmap.mapSource.tiled.CanvasEngine
import com.rafambn.kmap.mapSource.tiled.Layer
import com.rafambn.kmap.mapSource.tiled.RasterTile
import com.rafambn.kmap.mapSource.tiled.Tile
import com.rafambn.kmap.mapSource.tiled.TileLayers
import com.rafambn.kmap.mapSource.tiled.TileSpecs
import com.rafambn.kmap.utils.loopInZoom
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select

class RasterCanvasEngine(
    maxCacheTiles: Int = 20,
    getTile: suspend (zoom: Int, row: Int, column: Int) -> RasterTileResult,
    coroutineScope: CoroutineScope,
) : CanvasEngine(maxCacheTiles, coroutineScope) {

    override val tileLayers = mutableStateOf(TileLayers())
    var cachedTiles = listOf<RasterTile>()

    init {
        coroutineScope.launch {
            while (isActive) {
                select {
                    rasterTileRenderer.tilesProcessedChannel.onReceive {//TODO understand why this part is being broken with viewmodel
                        val newCache = cachedTiles.toMutableList()
                        newCache.add(it)
                        cachedTiles = if (newCache.size > maxCacheTiles)
                            newCache.takeLast(maxCacheTiles)
                        else
                            newCache.toList()

                        if (it.zoom == tileLayers.value.frontLayer.level)
                            tileLayers.value = tileLayers.value.copy(
                                frontLayer = Layer(it.zoom, tileLayers.value.frontLayer.tiles.toMutableList().apply { add(it) })
                            )
                        else if (it.zoom == tileLayers.value.backLayer.level)
                            tileLayers.value = tileLayers.value.copy(
                                backLayer = Layer(it.zoom, tileLayers.value.frontLayer.tiles.toMutableList().apply { add(it) })
                            )
                    }
                }
            }
        }
    }

    val rasterTileRenderer = RasterTileRenderer(getTile, coroutineScope)

    override fun renderTiles(visibleTiles: List<TileSpecs>, zoomLevel: Int) {
        val needsToChangeLevel = zoomLevel != tileLayers.value.frontLayer.level

        val newFrontLayer = mutableListOf<Tile>()
        val tilesToRender = mutableListOf<TileSpecs>()
        visibleTiles.forEach { tileSpecs ->
            val foundInTiles = if (needsToChangeLevel)
                tileLayers.value.backLayer.tiles.find { it == tileSpecs }
            else
                tileLayers.value.frontLayer.tiles.find { it == tileSpecs }
            val foundInRenderedTiles = cachedTiles.find {
                it == TileSpecs(tileSpecs.zoom, tileSpecs.row.loopInZoom(tileSpecs.zoom), tileSpecs.col.loopInZoom(tileSpecs.zoom))
            }

            when {
                foundInTiles != null -> {
                    newFrontLayer.add(foundInTiles)
                }

                foundInRenderedTiles != null -> {
                    newFrontLayer.add(foundInRenderedTiles)
                }

                else -> {
                    tilesToRender.add(tileSpecs)
                }
            }
        }
        if (needsToChangeLevel)
            tileLayers.value = tileLayers.value.copy(frontLayer = Layer(zoomLevel, newFrontLayer), backLayer = tileLayers.value.frontLayer)
        else
            tileLayers.value = tileLayers.value.copy(frontLayer = Layer(zoomLevel, newFrontLayer))
        coroutineScope.launch { rasterTileRenderer.tilesToProcessChannel.send(tilesToRender) }
    }
}
