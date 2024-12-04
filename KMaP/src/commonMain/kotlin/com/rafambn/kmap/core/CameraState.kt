package com.rafambn.kmap.core

import com.rafambn.kmap.utils.CanvasPosition
import com.rafambn.kmap.utils.ScreenOffset

data class CameraState(
    val canvasSize: ScreenOffset = ScreenOffset.Zero,
    val zoom: Float = 0F,
    val angleDegrees: Double = 0.0,
    val rawPosition: CanvasPosition = CanvasPosition.Zero,
)
