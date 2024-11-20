package com.rafambn.kmap.model

import androidx.compose.ui.geometry.Offset
import com.rafambn.kmap.utils.offsets.CanvasPosition

data class CameraState(
    val canvasSize: Offset = Offset.Zero,
    val zoom: Float = 0F,
    val angleDegrees: Double = 0.0,
    val rawPosition: CanvasPosition = CanvasPosition.Zero,
)
