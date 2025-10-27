package com.rafambn.kmap.components

import androidx.compose.runtime.Composable

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
