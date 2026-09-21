package com.rafambn.kmap.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import kotlin.jvm.JvmInline

@JvmInline
value class ViewPort(val value: Rect) {
    val topLeft: Offset get() = value.topLeft
    val topRight: Offset get() = value.topRight
    val bottomRight: Offset get() = value.bottomRight
    val bottomLeft: Offset get() = value.bottomLeft

    fun overlaps(other: ViewPort): Boolean = value.overlaps(other.value)

    companion object {
        val Zero = ViewPort(Rect(Offset.Zero, Size.Zero))
    }
}

fun getViewPort(drawPosition: DrawPosition, width: Float, height: Float, offset: Offset): ViewPort =
    ViewPort(
        Rect(
            offset - Offset(width * drawPosition.x, height * drawPosition.y),
            Size(width, height)
        )
    )
