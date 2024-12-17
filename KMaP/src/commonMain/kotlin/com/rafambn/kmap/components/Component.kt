package com.rafambn.kmap.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope.Companion.DefaultBlendMode
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.input.pointer.PointerInputScope
import com.rafambn.kmap.tiles.TileRenderResult
import com.rafambn.kmap.utils.Coordinates

data class MarkerComponent(
    val markerParameters: MarkerParameters,
    val markerContent: @Composable () -> Unit
)

data class ClusterComponent(
    val clusterParameters: ClusterParameters,
    val clusterContent: @Composable () -> Unit
)

data class CanvasComponent(
    val alpha: Float = 1F,
    val zIndex: Float = 0F,
    val maxCacheTiles: Int = 20,
    val getTile: suspend (zoom: Int, row: Int, column: Int) -> TileRenderResult,
    val gestureDetector: (suspend PointerInputScope.() -> Unit)? = null,
)

data class PathComponent(
    val origin: Coordinates,
    val path: Path,
    val color: Color,
    val zIndex: Float = 1F,
    val alpha: Float = 1F,
    val style: DrawStyle = Fill,
    val colorFilter: ColorFilter? = null,
    val blendMode: BlendMode = DefaultBlendMode,
    val gestureDetector: (suspend PointerInputScope.() -> Unit)? = null,
)