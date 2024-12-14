package com.rafambn.kmap.core

import com.rafambn.kmap.utils.TilePoint
import com.rafambn.kmap.utils.Coordinates
import com.rafambn.kmap.utils.ScreenOffset

data class CameraState(
    val canvasSize: ScreenOffset = ScreenOffset.Zero,
    val zoom: Float = 0F,
    val angleDegrees: Double = 0.0,
    val coordinates: Coordinates,
    internal val tilePoint: TilePoint
)
