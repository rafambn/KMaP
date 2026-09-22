package com.rafambn.kmap.camera

import com.rafambn.kmap.geometry.angle.Degrees
import com.rafambn.kmap.geometry.plane.Coordinates

data class CameraState(
    val zoom: Float = 0F,
    val angleDegrees: Degrees = Degrees.Zero,
    val coordinates: Coordinates,
) {
    init {
        require(zoom.isFinite()) { "Zoom must be finite" }
    }
}
