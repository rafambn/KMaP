package com.rafambn.kmap.components.parameters

import com.rafambn.kmap.components.DrawPosition
import com.rafambn.kmap.geometry.angle.Degrees
import com.rafambn.kmap.utils.Coordinates

open class MarkerParameters(
    val coordinates: Coordinates,
    val alpha: Float = 1F,
    val drawPosition: DrawPosition = DrawPosition.TOP_LEFT,
    val zIndex: Float = 2F,
    val zoomVisibilityRange: ClosedFloatingPointRange<Float> = 0F..Float.MAX_VALUE,
    val zoomToFix: Float? = null,
    val rotateWithMap: Boolean = false,
    val rotation: Degrees = Degrees.Zero,
    val clusterId: Int? = null
) : Parameters
