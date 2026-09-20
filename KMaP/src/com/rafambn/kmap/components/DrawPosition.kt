package com.rafambn.kmap.components

import androidx.compose.ui.graphics.TransformOrigin

data class DrawPosition(val x: Float, val y: Float) {
    init {
        require(x in 0F..1F) { "x must be in the range [0, 1]" }
        require(y in 0F..1F) { "y must be in the range [0, 1]" }
    }

    fun asTransformOrigin(): TransformOrigin = TransformOrigin(x, y)

    companion object {
        val CENTER = DrawPosition(0.5F, 0.5F)
        val CENTER_LEFT = DrawPosition(0F, 0.5F)
        val CENTER_RIGHT = DrawPosition(1F, 0.5F)
        val CENTER_TOP = DrawPosition(0.5F, 0F)
        val CENTER_BOTTOM = DrawPosition(0.5F, 1F)
        val TOP_LEFT = DrawPosition(0F, 0F)
        val TOP_RIGHT = DrawPosition(1F, 0F)
        val BOTTOM_RIGHT = DrawPosition(1F, 1F)
        val BOTTOM_LEFT = DrawPosition(0F, 1F)
    }
}
