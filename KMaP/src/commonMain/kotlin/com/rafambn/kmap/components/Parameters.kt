package com.rafambn.kmap.components

import androidx.compose.ui.unit.Dp
import com.rafambn.kmap.core.DrawPosition
import com.rafambn.kmap.utils.Degrees
import com.rafambn.kmap.utils.ProjectedCoordinates

data class MarkerParameters(
    val coordinates: ProjectedCoordinates,
    val tag: String = "",
    val alpha: Float = 1F,
    val drawPosition: DrawPosition = DrawPosition.TOP_LEFT,
    val zIndex: Float = 1F,
    val zoomToFix: Float? = null,
    val rotateWithMap: Boolean = false,
    val rotation: Degrees = 0.0
)

data class CanvasParameters(
    val tag: String = "",
    val alpha: Float = 1F,
    val zIndex: Float = 0F,
    val maxCacheTiles: Int = 20
)

data class ClusterParameters(
    val tag: String,
    val clusterThreshold: Dp,
    val alpha: Float = 1F,
    val zIndex: Float = 1F,
    val drawPosition: DrawPosition = DrawPosition.TOP_LEFT,
    val rotateWithMap: Boolean = false,
    val rotation: Degrees = 0.0
)