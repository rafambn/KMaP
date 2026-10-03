package com.rafambn.kmap.source.internal.render

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas

// Adjacent tiles need hard clip edges; antialiased clips expose the background at their join.
internal expect fun Canvas.clipTileBounds(bounds: Rect)
