package com.rafambn.kmap.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntSize

internal data class GraphiteMapCamera(
    val canvasSize: IntSize,
    val translation: Offset,
    val rotationDegrees: Float,
    val magnifierScale: Float,
    val positionOffset: Offset,
    val tileSizePx: Size,
    val zoom: Double,
)
