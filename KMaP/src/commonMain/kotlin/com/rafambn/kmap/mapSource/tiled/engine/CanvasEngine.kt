package com.rafambn.kmap.mapSource.tiled.engine

import androidx.compose.runtime.mutableStateOf
import com.rafambn.kmap.mapSource.tiled.ActiveTiles
import com.rafambn.kmap.mapSource.tiled.tiles.Tile
import com.rafambn.kmap.mapSource.tiled.tiles.TileSpecs
import com.rafambn.kmap.utils.loopInZoom
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select

abstract class CanvasEngine<T : Tile>(
    val maxCacheTiles: Int = 20,
    val coroutineScope: CoroutineScope,
    private val tileRenderer: TileRenderer<*, T>
) {
    val activeTiles = mutableStateOf(ActiveTiles())
    var cachedTiles = listOf<T>()
    var currentVisibleTiles = listOf<TileSpecs>()

    init {
        coroutineScope.launch {
            while (isActive) {
                select {
                    tileRenderer.tilesProcessedChannel.onReceive { tile ->
                        val newCache = cachedTiles.toMutableList()
                        newCache.add(tile)
                        cachedTiles = if (newCache.size > maxCacheTiles)
                            newCache.takeLast(maxCacheTiles)
                        else
                            newCache.toList()

                        filterActiveTiles(currentVisibleTiles, activeTiles.value.currentZoom)
                    }
                }
            }
        }
    }

    fun renderTiles(visibleTiles: List<TileSpecs>, zoomLevel: Int) {
        currentVisibleTiles = visibleTiles
        val tilesToRender = filterActiveTiles(currentVisibleTiles, zoomLevel)
        coroutineScope.launch { tileRenderer.tilesToProcessChannel.send(tilesToRender) }
    }

    private fun filterActiveTiles(visibleTiles: List<TileSpecs>, zoomLevel: Int): List<TileSpecs> {
        val activeTilesMap = activeTiles.value.tiles.associateBy { TileSpecs(it.zoom, it.row, it.col) }
        val cachedTilesMap = cachedTiles.associateBy { TileSpecs(it.zoom, it.row, it.col) }
        val newFrontLayer = mutableListOf<Tile>()
        val tilesToRender = mutableListOf<TileSpecs>()

        visibleTiles.forEach { tileSpecs ->
            activeTilesMap[tileSpecs]?.let {
                newFrontLayer.add(it)
            } ?: run {
                val normalized = TileSpecs(
                    tileSpecs.zoom,
                    tileSpecs.row.loopInZoom(tileSpecs.zoom),
                    tileSpecs.col.loopInZoom(tileSpecs.zoom)
                )
                cachedTilesMap[normalized]?.let {
                    newFrontLayer.add(it)  //TODO must be addeded with the original tile spec and no the looped one
                } ?: run {
                    tilesToRender.add(tileSpecs)
                }
            }
        }

        val allTiles = mutableListOf<Tile>()
        allTiles.addAll(newFrontLayer)

        if (tilesToRender.isEmpty()) {
            activeTiles.value = ActiveTiles(tiles = allTiles.sortedBy { it.zoom }, currentZoom = zoomLevel)
            return emptyList()
        }

        val allAvailableTiles = (activeTiles.value.tiles + cachedTiles).distinct().sortedBy { it.zoom }.asReversed()
        val parentTiles = mutableSetOf<Tile>()
        val childTiles = mutableSetOf<Tile>()

        for (tileToRender in tilesToRender) {
            for (availableTile in allAvailableTiles) {
                if (availableTile.isParentOf(tileToRender)) {
                    parentTiles.add(availableTile)
                    break
                } else if (availableTile.isChildOf(tileToRender)) {
                    childTiles.add(availableTile)
                }
            }
        }

        childTiles.removeAll { child ->
            parentTiles.any { parent -> parent.isParentOf(child) }
        }

        allTiles.addAll(parentTiles)
        allTiles.addAll(childTiles)

        activeTiles.value = ActiveTiles(tiles = allTiles.sortedBy { it.zoom }, currentZoom = zoomLevel)

        return tilesToRender
    }
}
