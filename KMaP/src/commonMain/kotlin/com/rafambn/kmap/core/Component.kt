package com.rafambn.kmap.core

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Dp
import com.rafambn.kmap.model.ResultTile
import com.rafambn.kmap.utils.Degrees
import com.rafambn.kmap.utils.offsets.ProjectedCoordinates
import com.rafambn.kmap.utils.offsets.ScreenOffset

data class MarkerParameters(
    val coordinates: ProjectedCoordinates,
    val tag: String = "",
    val alpha: Float = 1F,
    val drawPosition: DrawPosition = DrawPosition.TOP_LEFT,
    val zIndex: Float = 2F,
    val scaleWithMap: Boolean = false, // TODO join this variable
    val zoomToFix: Float = 0F,//TODO
    val rotateWithMap: Boolean = false,
    val rotation: Degrees = 0.0
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
)

data class MarkerComponent(
    val markerParameters: MarkerParameters,
    val markerContent: @Composable (MarkerParameters) -> Unit
)

data class ClusterComponent(
    val clusterParameters: ClusterParameters,
    val clusterContent: @Composable (ClusterParameters, size: Int) -> Unit
)

data class Marker(
    val markerParameters: MarkerParameters,
    val placementOffset: ScreenOffset,
    val markerContent: @Composable (MarkerParameters) -> Unit
)

data class Canvas(
    val canvasParameters: CanvasParameters,
    val getTile: suspend (zoom: Int, row: Int, column: Int) -> ResultTile
)

data class Cluster(
    val clusterParameters: ClusterParameters,
    val placementOffset: ScreenOffset,
    val size: Int,
    val clusterContent: @Composable (ClusterParameters, size: Int) -> Unit
)

data class Component(
    val data: Any,
    val placementOffset: Offset,
    val placeable: Placeable
)