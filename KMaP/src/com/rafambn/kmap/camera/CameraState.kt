package com.rafambn.kmap.camera

import com.rafambn.kmap.geometry.plane.Coordinates
import com.rafambn.kmap.geometry.angle.Degrees
import com.rafambn.kmap.geometry.plane.ScreenOffset
import com.rafambn.kmap.geometry.plane.TilePoint

data class CameraState(
    val canvasSize: ScreenOffset = ScreenOffset.Zero,
    val zoom: Float = 0F,
    val angleDegrees: Degrees = Degrees.Zero,
    val coordinates: Coordinates,
    internal val tilePoint: TilePoint
)
