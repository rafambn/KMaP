package com.rafambn.kmap.camera

import com.rafambn.kmap.geometry.angle.Degrees
import com.rafambn.kmap.geometry.plane.TilePoint

internal data class InternalCameraState(
    val zoom: Float = 0F,
    val angleDegrees: Degrees = Degrees.Zero,
    val tilePoint: TilePoint,
)
