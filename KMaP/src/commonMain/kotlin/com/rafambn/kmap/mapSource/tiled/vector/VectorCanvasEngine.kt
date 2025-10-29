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
    maxCacheTiles: Int = 20,
    getTile: suspend (zoom: Int, row: Int, column: Int) -> VectorTileResult,
    coroutineScope: CoroutineScope,
    style: Style
): CanvasEngine(maxCacheTiles, coroutineScope){

    override val tileLayers = mutableStateOf(TileLayers())
    var cachedTiles = listOf<OptimizedVectorTile>()
    val vectorTileRenderer = VectorTileRenderer(getTile, coroutineScope, style)

    init {
        coroutineScope.launch {
            while (isActive) {
                select {
                    vectorTileRenderer.tilesProcessedChannel.onReceive {
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
        coroutineScope.launch { vectorTileRenderer.tilesToProcessChannel.send(tilesToRender) }
    }
}
