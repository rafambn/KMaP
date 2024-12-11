package com.rafambn.kmap.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.pointer.PointerInputScope
import com.rafambn.kmap.tiles.TileRenderResult

data class MarkerComponent(
    val markerParameters: MarkerParameters,
    val markerContent: @Composable (marker: MarkerParameters) -> Unit
)

data class ClusterComponent(
    val clusterParameters: ClusterParameters,
    val clusterContent: @Composable (cluster: ClusterParameters) -> Unit
)

data class CanvasComponent(
    val canvasParameters: CanvasParameters,
    val getTile: suspend (zoom: Int, row: Int, column: Int) -> TileRenderResult,
    val gestureDetection: (suspend PointerInputScope.() -> Unit)? = null,
)