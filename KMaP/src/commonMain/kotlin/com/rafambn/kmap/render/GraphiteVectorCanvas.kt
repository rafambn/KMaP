package com.rafambn.kmap.render

import com.rafambn.kmap.mapSource.tiled.ActiveTiles
import com.rafambn.kmap.utils.style.OptimizedStyle

internal data class GraphiteVectorCanvas(
    val id: Int,
    val declarationIndex: Int,
    val zIndex: Float,
    val style: OptimizedStyle,
    val activeTiles: ActiveTiles,
)
