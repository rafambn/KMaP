package com.rafambn.kmap.core

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Dp
import com.rafambn.kmap.utils.TileRenderResult
import com.rafambn.kmap.utils.Degrees
import com.rafambn.kmap.utils.ProjectedCoordinates
import com.rafambn.kmap.utils.ScreenOffset

data class MarkerParameters(
    val coordinates: ProjectedCoordinates,
    val tag: String = "",
    val alpha: Float = 1F,
    val drawPosition: DrawPosition = DrawPosition.TOP_LEFT,
    val zIndex: Float = 2F,
    val zoomToFix: Float? = null,
    val rotateWithMap: Boolean = false,
    val rotation: Degrees = 0.0
)

data class CanvasParameters(
    val tag: String = "",
    val alpha: Float = 1F,
    val zIndex: Float = 1F,
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

data class MarkerComponent(
    val markerParameters: MarkerParameters,
    val markerContent: @Composable (marker: MarkerParameters) -> Unit
)

data class ClusterComponent(
    val clusterParameters: ClusterParameters,
    val clusterContent: @Composable (cluster: ClusterParameters, size: Int) -> Unit
)

data class Marker(
    val markerParameters: MarkerParameters,
    val placementOffset: ScreenOffset,
    val markerContent: @Composable (marker: MarkerParameters) -> Unit
)

data class Canvas(
    val canvasParameters: CanvasParameters,
    val getTile: suspend (zoom: Int, row: Int, column: Int) -> TileRenderResult,
    val gestureDetection: (suspend PointerInputScope.() -> Unit)? = null,
)

data class Cluster(
    val clusterParameters: ClusterParameters,
    val placementOffset: ScreenOffset,
    val size: Int,
    val clusterContent: @Composable (cluster: ClusterParameters, size: Int) -> Unit
)

data class Component(
    val data: Any,
    val placementOffset: Offset,
    val placeable: Placeable
)