package com.rafambn.kmap.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Dp
import com.rafambn.kmap.core.DrawPosition
import com.rafambn.kmap.tiles.TileRenderResult
import com.rafambn.kmap.utils.Degrees
import com.rafambn.kmap.utils.ProjectedCoordinates
import com.rafambn.kmap.utils.ScreenOffset

data class MarkerComponent(
    val markerParameters: MarkerParameters,
    val markerContent: @Composable (marker: MarkerParameters) -> Unit
)

data class ClusterComponent(
    val clusterParameters: ClusterParameters,
    val clusterContent: @Composable (cluster: ClusterParameters, size: Int) -> Unit
)

data class CanvasComponent(
    val canvasParameters: CanvasParameters,
    val getTile: suspend (zoom: Int, row: Int, column: Int) -> TileRenderResult,
    val gestureDetection: (suspend PointerInputScope.() -> Unit)? = null,
)