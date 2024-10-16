package com.rafambn.kmap.utils.offsets

import androidx.compose.ui.geometry.Offset

typealias ScreenOffset = Offset

fun ScreenOffset.toPosition(): CanvasPosition = CanvasPosition(x.toDouble(), y.toDouble())