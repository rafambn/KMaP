package com.rafambn.kmap.components.canvas

import androidx.compose.ui.input.pointer.PointerInputScope
import com.rafambn.kmap.components.Component
import com.rafambn.kmap.components.canvas.tiled.TileRenderResult

data class CanvasComponent(
    val alpha: Float = 1F,
    val zIndex: Float = 0F,
    val maxCacheTiles: Int = 20,
    val getTile: suspend (zoom: Int, row: Int, column: Int) -> TileRenderResult,
    val gestureDetector: (suspend PointerInputScope.() -> Unit)? = null,
): Component
