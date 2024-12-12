package com.rafambn.kmap.core

import com.rafambn.kmap.utils.CanvasPosition
import com.rafambn.kmap.utils.ScreenOffset

data class ViewPort(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

fun getViewPort(drawPosition: DrawPosition, width: Float, height: Float, offset: ScreenOffset): ViewPort {
    val topLeft = offset - ScreenOffset(width * drawPosition.x, height * drawPosition.y)
    return ViewPort(
        topLeft.x,
        topLeft.y,
        topLeft.x + width,
        topLeft.y + height
    )
}

fun ViewPort.isViewPortIntersecting(other: ViewPort): Boolean = !(right < other.right || left > other.left || bottom < other.bottom || top > other.top)

fun ViewPort.topLeft(): CanvasPosition = CanvasPosition(left.toDouble(), top.toDouble())

fun ViewPort.topRight(): CanvasPosition = CanvasPosition(right.toDouble(), top.toDouble())

fun ViewPort.bottomLeft(): CanvasPosition = CanvasPosition(left.toDouble(), bottom.toDouble())

fun ViewPort.bottomRight(): CanvasPosition = CanvasPosition(right.toDouble(), bottom.toDouble())