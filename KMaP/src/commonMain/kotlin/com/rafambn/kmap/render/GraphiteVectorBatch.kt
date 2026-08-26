package com.rafambn.kmap.render

import com.rafambn.kmap.mapSource.tiled.ActiveTiles
import com.rafambn.kmap.utils.style.OptimizedStyleLayer

internal data class GraphiteVectorBatch(
    val canvasId: Int,
    val canvasOrder: Int,
    val styleLayerIndex: Int,
    val styleLayer: OptimizedStyleLayer,
    val activeTiles: ActiveTiles,
)

internal fun <T> List<Pair<Int, T>>.inVisualOrder(): List<Pair<Int, T>> = sortedBy { it.first }
