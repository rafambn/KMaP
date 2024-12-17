package com.rafambn.kmap.components

import com.rafambn.kmap.core.DrawPosition
import com.rafambn.kmap.utils.Degrees
import com.rafambn.kmap.utils.Coordinates

sealed interface Parameters

open class MarkerParameters(
    val coordinates: Coordinates,
    val alpha: Float = 1F,
    val drawPosition: DrawPosition = DrawPosition.TOP_LEFT,
    val zIndex: Float = 2F,
    val zoomToFix: Float? = null,
    val rotateWithMap: Boolean = false,
    val rotation: Degrees = 0.0,
    val clusterId: Int? = null
) : Parameters

open class ClusterParameters(
    val id: Int,
    val alpha: Float = 1F,
    val zIndex: Float = 2F,
    val rotateWithMap: Boolean = false,
    val rotation: Degrees = 0.0
) : Parameters