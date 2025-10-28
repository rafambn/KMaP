package com.rafambn.kmap.mapSource.tiled.vector

import androidx.compose.runtime.mutableStateOf
import com.rafambn.kmap.mapSource.tiled.CanvasEngine
import com.rafambn.kmap.mapSource.tiled.TileLayers
import com.rafambn.kmap.mapSource.tiled.TileSpecs
import com.rafambn.kmap.utils.style.Style
import kotlinx.coroutines.CoroutineScope

class VectorCanvasEngine(
    maxCacheTiles: Int = 20,
    getTile: suspend (zoom: Int, row: Int, column: Int) -> VectorTileResult,
    coroutineScope: CoroutineScope,
    style: Style
): CanvasEngine(maxCacheTiles, coroutineScope){

    override val tileLayers = mutableStateOf(TileLayers())
    override fun renderTiles(visibleTiles: List<TileSpecs>, zoomLevel: Int) {
        TODO("Not yet implemented")
    }
}
