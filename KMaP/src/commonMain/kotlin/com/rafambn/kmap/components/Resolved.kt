package com.rafambn.kmap.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.pointer.PointerInputScope
import com.rafambn.kmap.tiles.TileRenderResult
import com.rafambn.kmap.utils.ScreenOffset

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