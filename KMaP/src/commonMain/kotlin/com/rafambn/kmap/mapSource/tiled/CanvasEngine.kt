package com.rafambn.kmap.mapSource.tiled

import androidx.compose.runtime.MutableState
import kotlinx.coroutines.CoroutineScope

abstract class CanvasEngine(
    maxCacheTiles: Int = 20,
    val coroutineScope: CoroutineScope
) {

    abstract val activeTiles: MutableState<ActiveTiles>

    abstract fun renderTiles(visibleTiles: List<TileSpecs>, zoomLevel: Int)
}
