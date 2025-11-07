package com.rafambn.kmap.mapSource.tiled.vector

import androidx.compose.runtime.mutableStateOf
import com.rafambn.kmap.mapSource.tiled.*
import com.rafambn.kmap.utils.loopInZoom
import com.rafambn.kmap.utils.style.Style
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select

class VectorCanvasEngine(
    maxCacheTiles: Int,
    getTile: suspend (zoom: Int, row: Int, column: Int) -> VectorTileResult,
    coroutineScope: CoroutineScope,
    style: Style
) : CanvasEngine(maxCacheTiles, coroutineScope) {

    override val activeTiles = mutableStateOf(ActiveTiles())
    var cachedTiles = listOf<OptimizedVectorTile>()
    val vectorTileRenderer = VectorTileRenderer(getTile, coroutineScope, style)

    init {
        coroutineScope.launch {
            while (isActive) {
                select {
                    vectorTileRenderer.tilesProcessedChannel.onReceive { vectorTile ->
                        val newCache = cachedTiles.toMutableList()
                        newCache.add(vectorTile)
                        cachedTiles = if (newCache.size > maxCacheTiles)
                            newCache.takeLast(maxCacheTiles)
                        else
                            newCache.toList()

                        activeTiles.value = activeTiles.value.copy(
                            tiles = (activeTiles.value.tiles + vectorTile).sortedBy { it.zoom }
                        )
                    }
                }
            }
        }
    }

    override fun renderTiles(visibleTiles: List<TileSpecs>, zoomLevel: Int) {
        val activeTilesMap = activeTiles.value.tiles.associateBy { TileSpecs(it.zoom, it.row, it.col) }
        val cachedTilesMap = cachedTiles.associateBy { TileSpecs(it.zoom, it.row, it.col) }
        val newFrontLayer = mutableListOf<Tile>()
        val tilesToRender = mutableListOf<TileSpecs>()

        visibleTiles.forEach { tileSpecs ->
            activeTilesMap[tileSpecs]?.let {
                newFrontLayer.add(it)
            } ?: run {
                val normalized = TileSpecs(tileSpecs.zoom, tileSpecs.row.loopInZoom(tileSpecs.zoom), tileSpecs.col.loopInZoom(tileSpecs.zoom))
                cachedTilesMap[normalized]?.let {
                    newFrontLayer.add(it)
                } ?: run {
                    tilesToRender.add(tileSpecs)
                }
            }
        }

        val allTiles = mutableListOf<Tile>()
        allTiles.addAll(newFrontLayer)

        if (tilesToRender.isEmpty()) {
            activeTiles.value = ActiveTiles(tiles = allTiles.sortedBy { it.zoom }, currentZoom = zoomLevel)
            return
        }

        val allAvailableTiles = (activeTiles.value.tiles + cachedTiles).distinct()
        val parentTiles = mutableSetOf<Tile>()
        val childTiles = mutableSetOf<Tile>()

        tilesToRender.forEach { tileToRender ->
            allAvailableTiles.forEach { availableTile ->
                when {
                    availableTile.isParentOf(tileToRender) -> parentTiles.add(availableTile)
                    availableTile.isChildOf(tileToRender) -> childTiles.add(availableTile)
                }
            }
        }

        childTiles.removeAll { child ->
            parentTiles.any { parent -> parent.isParentOf(child) }
        }

        allTiles.addAll(parentTiles)
        allTiles.addAll(childTiles)

        activeTiles.value = ActiveTiles(tiles = allTiles.sortedBy { it.zoom }, currentZoom = zoomLevel)
        coroutineScope.launch { vectorTileRenderer.tilesToProcessChannel.send(tilesToRender) }
    }
}
