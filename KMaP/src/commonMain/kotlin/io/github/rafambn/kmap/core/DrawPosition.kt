package io.github.rafambn.kmap.core

class DrawPosition(x: Float, y: Float) {
    val x = x.coerceIn(0.0f, 1.0f).also {
        if (it != x) println("Warning: x was coerced to the range [0, 1]")
    }
    val y = y.coerceIn(0.0f, 1.0f).also {
        if (it != y) println("Warning: y was coerced to the range [0, 1]")
    }

    companion object {
        val CENTER = DrawPosition(0.5F, 0.5F)
        val CENTER_LEFT = DrawPosition(0F, 0.5F)
        val CENTER_RIGHT = DrawPosition(1F, 0.5F)
        val BOTTOM_CENTER = DrawPosition(0.5F, 1F)
        val BOTTOM_LEFT = DrawPosition(0F, 1F)
        val BOTTOM_RIGHT = DrawPosition(1F, 1F)
        val TOP_CENTER = DrawPosition(0.5F, 0F)
        val TOP_LEFT = DrawPosition(0F, 0F)
        val TOP_RIGHT = DrawPosition(1F, 0F)
    }
}