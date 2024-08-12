package io.github.rafambn.kmap.core

import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Dp
import io.github.rafambn.kmap.utils.Degrees
import io.github.rafambn.kmap.utils.offsets.ProjectedCoordinates
import io.github.rafambn.kmap.utils.offsets.ScreenOffset

data class MarkerParameters(
    val coordinates: ProjectedCoordinates,
    val tag: String = "",
    val alpha: Float = 1F,
    val drawPosition: DrawPosition = DrawPosition.TOP_LEFT,
    val zIndex: Float = 2F,
    val scaleWithMap: Boolean = false,
    val zoomToFix: Float = 0F,
    val rotateWithMap: Boolean = false,
    val rotation: Degrees = 0.0
) {
    fun toMarkerData(placementOffset: ScreenOffset): MarkerData = MarkerData(
        coordinates, tag, alpha, drawPosition, zIndex, scaleWithMap, zoomToFix, rotateWithMap, rotation, placementOffset
    )
}

data class MarkerData(
    val coordinates: ProjectedCoordinates,
    val tag: String = "",
    val alpha: Float = 1F,
    val drawPosition: DrawPosition = DrawPosition.TOP_LEFT,
    val zIndex: Float = 2F,
    val scaleWithMap: Boolean = false,
    val zoomToFix: Float = 0F,
    val rotateWithMap: Boolean = false,
    val rotation: Degrees = 0.0,
    val placementOffset: ScreenOffset
)

data class CanvasParameters(
    val tag: String = "",
    val alpha: Float = 1F,
    val zIndex: Float = 1F
)

data class ClusterParameters(
    val tag: String,
    val clusterThreshold: Dp,
    val alpha: Float = 1F,
    val zIndex: Float = 1F,
    val drawPosition: DrawPosition = DrawPosition.CENTER,
    val rotateWithMap: Boolean = false,
    val rotation: Degrees = 0.0
) {
    fun toClusterData(placementOffset: ScreenOffset, size: Int): ClusterData = ClusterData(
        tag, clusterThreshold, alpha, zIndex, drawPosition, rotateWithMap, rotation, placementOffset, size
    )
}


data class ClusterData(
    val tag: String,
    val clusterThreshold: Dp,
    val alpha: Float = 1F,
    val zIndex: Float = 1F,
    val drawPosition: DrawPosition = DrawPosition.CENTER,
    val rotateWithMap: Boolean = false,
    val rotation: Degrees = 0.0,
    val placementOffset: ScreenOffset,
    val size: Int,
)

data class Component(val data: Any, val placeable: Placeable)