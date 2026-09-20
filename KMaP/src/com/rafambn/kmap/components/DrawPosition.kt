package com.rafambn.kmap.components

import androidx.compose.ui.graphics.TransformOrigin

class DrawPosition(x: Float, y: Float) {
    val x = x.coerceIn(0.0f, 1.0f).also {
        if (it != x) println("Warning: x was coerced to the range [0, 1]")
    }
    val y = y.coerceIn(0.0f, 1.0f).also {
        if (it != y) println("Warning: y was coerced to the range [0, 1]")
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