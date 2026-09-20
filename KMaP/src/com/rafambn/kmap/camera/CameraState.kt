package com.rafambn.kmap.camera

import com.rafambn.kmap.utils.Coordinates
import com.rafambn.kmap.geometry.angle.Degrees
import com.rafambn.kmap.utils.ScreenOffset
import com.rafambn.kmap.utils.TilePoint

data class CameraState(
    val canvasSize: ScreenOffset = ScreenOffset.Zero,
    val zoom: Float = 0F,
    val angleDegrees: Degrees = Degrees.Zero,
    val coordinates: Coordinates,
    internal val tilePoint: TilePoint
)
