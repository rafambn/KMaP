package com.rafambn.kmap.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Path
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
    val content: @Composable () -> Unit
) : Component

data class Path(
    val parameters: PathParameters,
    val content: @Composable () -> Unit
) : Component
