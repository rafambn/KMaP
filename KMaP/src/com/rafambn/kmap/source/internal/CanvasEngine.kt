package com.rafambn.kmap.source.internal

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.rafambn.kmap.source.Tile
import com.rafambn.kmap.source.TileSpecs
import com.rafambn.kmap.utils.loopInZoom
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select

abstract class CanvasEngine<T : Tile>(
    private val maxCacheTiles: Int = 20,
    coroutineScope: CoroutineScope,
    private val tileRenderer: TileRenderer<*, T>
) {
    var activeTiles by mutableStateOf(ActiveTiles())
        private set

    private val selections = Channel<TileSelection>(Channel.CONFLATED)
    private var cachedTiles = listOf<T>()
    private var currentVisibleTiles = listOf<TileSpecs>()
    private var currentZoom: Int? = null

    init {
        coroutineScope.launch {
            while (isActive) {
                select<Unit> {
                    selections.onReceive { selection ->
                        if (currentZoom == selection.zoomLevel && currentVisibleTiles == selection.visibleTiles) return@onReceive

                        currentZoom = selection.zoomLevel
                        currentVisibleTiles = selection.visibleTiles
                        val tilesToRender = filterActiveTiles(currentVisibleTiles, selection.zoomLevel)
                        // An empty request also replaces pending work for tiles that are no longer visible.
                        tileRenderer.tilesToProcessChannel.trySend(tilesToRender)
                    }
                    tileRenderer.tilesProcessedChannel.onReceive { tile ->
                        val newCache = cachedTiles.toMutableList()
                        newCache.add(tile)
                        cachedTiles = if (newCache.size > maxCacheTiles)
                            newCache.takeLast(maxCacheTiles)
                        else
                            newCache.toList()

                        currentZoom?.let { filterActiveTiles(currentVisibleTiles, it) }
                    }
                }
            }
        }.invokeOnCompletion { selections.cancel() }
    }

    /**
     * Queues a snapshot of the selection; only the latest pending selection is retained.
     * The engine updates [activeTiles] asynchronously in its scope, independently of the caller's dispatcher.
     * Calls after the engine stops are ignored.
     */
    fun renderTiles(visibleTiles: List<TileSpecs>, zoomLevel: Int) {
        selections.trySend(TileSelection(visibleTiles.toList(), zoomLevel))
    }

    private fun filterActiveTiles(visibleTiles: List<TileSpecs>, zoomLevel: Int): List<TileSpecs> {
        val activeTilesMap = activeTiles.tiles.associateBy { TileSpecs(it.zoom, it.row, it.col) }
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
                cachedTilesMap[normalized]?.let { cachedTile ->
                    val newTile = cachedTile.withSpecs(tileSpecs) as T
                    newFrontLayer.add(newTile)
                } ?: run {
                    tilesToRender.add(tileSpecs)
                }
            }
        }

        val allTiles = mutableListOf<Tile>()
        allTiles.addAll(newFrontLayer)

        if (tilesToRender.isEmpty()) {
            activeTiles = ActiveTiles(tiles = allTiles.sortedBy { it.zoom }, currentZoom = zoomLevel)
            return emptyList()
        }

        val allAvailableTiles = (activeTiles.tiles + cachedTiles).distinct().sortedBy { it.zoom }.asReversed()
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

        activeTiles = ActiveTiles(tiles = allTiles.sortedBy { it.zoom }, currentZoom = zoomLevel)

        return tilesToRender
    }
}
