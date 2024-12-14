package com.rafambn.kmap.core

import androidx.compose.ui.geometry.Size
import com.rafambn.kmap.utils.TilePoint
import com.rafambn.kmap.utils.ScreenOffset

data class BoundingBox(
    val topLeft: TilePoint,
    val topRight: TilePoint,
    val bottomRight: TilePoint,
    val bottomLeft: TilePoint
)

data class ViewPort( //TODO merge this 2 classes
    val origin: ScreenOffset,
    val size: Size,
)

fun getViewPort(drawPosition: DrawPosition, width: Float, height: Float, offset: ScreenOffset): ViewPort = ViewPort(
    offset - ScreenOffset(width * drawPosition.x, height * drawPosition.y),
    Size(width, height)
)

fun ViewPort.isViewPortIntersecting(other: ViewPort): Boolean {//TODO maybe modify to account for rotation
    val rect1Left = origin.x
    val rect1Top = origin.y
    val rect1Right = rect1Left + size.width
    val rect1Bottom = rect1Top + size.height

    val rect2Left = other.origin.x
    val rect2Top = other.origin.y
    val rect2Right = rect2Left + other.size.width
    val rect2Bottom = rect2Top + other.size.height

    return !(rect1Right < rect2Left || rect1Left > rect2Right || rect1Bottom < rect2Top || rect1Top > rect2Bottom)
}