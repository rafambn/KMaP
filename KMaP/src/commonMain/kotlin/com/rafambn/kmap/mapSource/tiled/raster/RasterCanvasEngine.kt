package com.rafambn.kmap.mapSource.tiled.raster

import androidx.compose.runtime.mutableStateOf
import com.rafambn.kmap.mapSource.tiled.*
import com.rafambn.kmap.utils.loopInZoom
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select

class RasterCanvasEngine(
    maxCacheTiles: Int,
    getTile: suspend (zoom: Int, row: Int, column: Int) -> RasterTileResult,
    coroutineScope: CoroutineScope,
) : CanvasEngine(maxCacheTiles, coroutineScope) {

    override val activeTiles = mutableStateOf(ActiveTiles())
    var cachedTiles = listOf<RasterTile>()
    val rasterTileRenderer = RasterTileRenderer(getTile, coroutineScope)

    init {
        coroutineScope.launch {
            while (isActive) {
                select {
                    rasterTileRenderer.tilesProcessedChannel.onReceive { rasterTile ->
                        val newCache = cachedTiles.toMutableList()
                        newCache.add(rasterTile)
                        cachedTiles = if (newCache.size > maxCacheTiles)
                            newCache.takeLast(maxCacheTiles)
                        else
                            newCache.toList()

                        activeTiles.value = activeTiles.value.copy(
                            tiles = activeTiles.value.tiles.toMutableList()
                                .apply { add(TileWithVisibility(tile = rasterTile, isVisible = false)) }
                                .sortedBy { it.tile.zoom }
                        )
                    }
                }
            }
        }
    }

    override fun renderTiles(visibleTiles: List<TileSpecs>, zoomLevel: Int) {
        val newFrontLayer = mutableListOf<TileWithVisibility>()
        val tilesToRender = mutableListOf<TileSpecs>()
        visibleTiles.forEach { tileSpecs ->
            val foundInTiles = activeTiles.value.tiles.find { it.tile == tileSpecs }
            val foundInRenderedTiles = cachedTiles.find {
                it == TileSpecs(tileSpecs.zoom, tileSpecs.row.loopInZoom(tileSpecs.zoom), tileSpecs.col.loopInZoom(tileSpecs.zoom))
            }

            when {
                foundInTiles != null -> {
                    newFrontLayer.add(foundInTiles.copy(isVisible = true))
                }

                foundInRenderedTiles != null -> {
                    newFrontLayer.add(TileWithVisibility(foundInRenderedTiles, true))
                }

                else -> {
                    tilesToRender.add(tileSpecs)
                }
            }
        }

        val allTilesWithVisibility = mutableListOf<TileWithVisibility>()
        allTilesWithVisibility.addAll(newFrontLayer)

        activeTiles.value.tiles.forEach { oldTile ->
            if (newFrontLayer.none { it.tile == oldTile.tile }) {
                allTilesWithVisibility.add(oldTile.copy(isVisible = false))
            }
        }

        activeTiles.value = ActiveTiles(tiles = allTilesWithVisibility.sortedBy { it.tile.zoom }, currentZoom = zoomLevel)
        coroutineScope.launch { rasterTileRenderer.tilesToProcessChannel.send(tilesToRender) }
    }
}
