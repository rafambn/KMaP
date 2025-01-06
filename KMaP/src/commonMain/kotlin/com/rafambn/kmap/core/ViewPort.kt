package com.rafambn.kmap.core

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size

typealias ViewPort = Rect

fun getViewPort(drawPosition: DrawPosition, width: Float, height: Float, offset: Offset): ViewPort = Rect(
    offset - Offset(width * drawPosition.x, height * drawPosition.y),
    Size(width, height)
)