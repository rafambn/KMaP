package com.rafambn.kmap.components.marker

import com.rafambn.kmap.components.Parameters
import com.rafambn.kmap.core.DrawPosition
import com.rafambn.kmap.utils.Coordinates
import com.rafambn.kmap.utils.Degrees

open class MarkerParameters(
    val coordinates: Coordinates,
    val alpha: Float = 1F,
    val drawPosition: DrawPosition = DrawPosition.TOP_LEFT,
    val zIndex: Float = 2F,
    val zoomParameters: MarkerZoomParameter = MarkerZoomParameter(),
    val rotateWithMap: Boolean = false,
    val rotation: Degrees = 0.0,
    val clusterId: Int? = null
) : Parameters

data class MarkerZoomParameter(
    val zoomVisibilityRange: ClosedFloatingPointRange<Float> = 0F..Float.MAX_VALUE,
    val zoomToFix: Float? = null
)
