package com.rafambn.kmap.components.parameters

sealed interface CanvasParameters : Parameters {
    val id: Int
    val alpha: Float
    val zIndex: Float
    val maxCacheTiles: Int
}
