package com.rafambn.kmap.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.pointer.PointerInputScope
import com.rafambn.kmap.tiles.TileRenderResult

sealed interface Component

data class Marker(
    val parameters: MarkerParameters,
    val content: @Composable () -> Unit
) : Component

data class Cluster(
    val parameters: ClusterParameters,
    val content: @Composable () -> Unit
) : Component

data class Canvas(
    val parameters: CanvasParameters,
    val maxCacheTiles: Int = 20,
    val getTile: suspend (zoom: Int, row: Int, column: Int) -> TileRenderResult,
    val gestureDetector: (suspend PointerInputScope.() -> Unit)? = null,
) : Component

data class Path(
    val parameters: PathParameters,
    val gestureDetector: (suspend PointerInputScope.() -> Unit)? = null,
) : Component
