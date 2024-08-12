package io.github.rafambn.kmap.core

import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Dp
import io.github.rafambn.kmap.utils.Degrees
import io.github.rafambn.kmap.utils.offsets.ProjectedCoordinates
import io.github.rafambn.kmap.utils.offsets.ScreenOffset

data class MarkerData(
    var coordinates: ProjectedCoordinates,
    val tag: String = "",
    val alpha: Float = 1F,
    val drawPosition: DrawPosition = DrawPosition.TOP_LEFT,
    val zIndex: Float = 1F,
    val scaleWithMap: Boolean = false,
    val zoomToFix: Float = 0F,
    val rotateWithMap: Boolean = false,
    val rotation: Degrees = 0.0
)

data class CanvasData(
    val tag: String = "",
    val alpha: Float = 1F,
    val zIndex: Float = 1F
)

data class ClusterCondition(
    val tag: String,
    val clusterThreshold: Dp,
    val alpha: Float = 1F,
    val zIndex: Float = 1F
)

data class ClusterData(
    val tag: String,
    val clusterThreshold: Dp,
    val alpha: Float = 1F,
    val zIndex: Float = 1F,
    val coordinates: ScreenOffset,
    val size: Int,
)

data class Component(val data: Any, val placeable: Placeable)