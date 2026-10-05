package com.rafambn.kmap.source.internal.render

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.skiaCanvas

internal actual fun Canvas.clipTileBounds(bounds: Rect) {
    skiaCanvas.clipRect(bounds.left, bounds.top, bounds.right, bounds.bottom, antiAlias = false)
}
