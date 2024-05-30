package io.github.rafambn.kmap

import io.github.rafambn.kmap.utils.CanvasPosition


interface DefaultPlacer {
    var coordinates: CanvasPosition
    val drawPosition: DrawPosition
    val groupId: Int
    val zIndex: Float
    val isGrouping: Boolean
    val placerType: MapComponentType
}

class MarkerPlacer(
    override var coordinates: CanvasPosition,
    override val drawPosition: DrawPosition,
    override val groupId: Int,
    override val zIndex: Float,
    override val isGrouping: Boolean,
    override val placerType: MapComponentType = MapComponentType.MARKER,
) : DefaultPlacer

class PathPlacer(
    override var coordinates: CanvasPosition,
    override val drawPosition: DrawPosition,
    override val groupId: Int,
    override val zIndex: Float,
    override val isGrouping: Boolean,
    override val placerType: MapComponentType = MapComponentType.PATH
) : DefaultPlacer

class DrawPosition(x: Float, y: Float) {
    val x = x.coerceIn(0.0f, 1.0f).also {
        if (it != x) println("Warning: x was coerced to the range [0, 1]")
    }
    val y = y.coerceIn(0.0f, 1.0f).also {
        if (it != y) println("Warning: y was coerced to the range [0, 1]")
    }

    companion object {
        val CENTER = DrawPosition(0.5F,0.5F)
        val CENTER_BOTTOM = DrawPosition(0.5F,1F)
        val CENTER_TOP = DrawPosition(0.5F,0F)
        val LEFT_BOTTOM = DrawPosition(0F,1F)
        val LEFT_CENTER = DrawPosition(0F,0.5F)
        val LEFT_TOP = DrawPosition(0F,0F)
        val RIGHT_BOTTOM = DrawPosition(1F,1F)
        val RIGHT_CENTER = DrawPosition(1F,0.5F)
        val RIGHT_TOP = DrawPosition(1F,0F)
    }
}