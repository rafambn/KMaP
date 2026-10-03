package com.rafambn.kmap.source.internal.render

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas

internal actual fun Canvas.clipTileBounds(bounds: Rect) {
    // Android's Canvas already uses a non-antialiased rectangular clip.
    clipRect(bounds)
}
