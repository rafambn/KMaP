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

    init {
        coroutineScope.launch {
            while (isActive) {
                select { //TODO add filtering here to
                    tileRenderer.tilesProcessedChannel.onReceive { tile ->
                        val newCache = cachedTiles.toMutableList()
                        newCache.add(tile)
                        cachedTiles = if (newCache.size > maxCacheTiles)
                            newCache.takeLast(maxCacheTiles)
                        else
                            newCache.toList()

                        activeTiles.value = activeTiles.value.copy(
                            tiles = (activeTiles.value.tiles + tile).sortedBy { it.zoom }
                        )
                    }
                }
            }
        }
    }

    fun renderTiles(visibleTiles: List<TileSpecs>, zoomLevel: Int) {
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
        coroutineScope.launch { tileRenderer.tilesToProcessChannel.send(tilesToRender) }
    }
}
