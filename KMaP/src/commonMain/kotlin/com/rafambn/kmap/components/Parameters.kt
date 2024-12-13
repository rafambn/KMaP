package com.rafambn.kmap.components

import androidx.compose.ui.graphics.Path
import com.rafambn.kmap.core.DrawPosition
import com.rafambn.kmap.utils.Degrees
import com.rafambn.kmap.utils.ProjectedCoordinates

sealed interface Parameters

data class MarkerParameters(
    val coordinates: ProjectedCoordinates,
    val alpha: Float = 1F,
    val drawPosition: DrawPosition = DrawPosition.TOP_LEFT,
    val zIndex: Float = 2F,
    val zoomToFix: Float? = null,
    val rotateWithMap: Boolean = false,
    val rotation: Degrees = 0.0,
    val clusterId: Int? = null
) : Parameters

data class CanvasParameters(
    val alpha: Float = 1F,
    val zIndex: Float = 0F,
    val maxCacheTiles: Int = 20
) : Parameters

data class ClusterParameters(
    val id: Int,
    val alpha: Float = 1F,
    val zIndex: Float = 2F,
    val rotateWithMap: Boolean = false,
    val rotation: Degrees = 0.0
) : Parameters

data class PathParameters(
    val origin: ProjectedCoordinates,
    val path: Path,
    val zIndex: Float = 1F,
    val alpha: Float = 1F,
    val zoomToFix: Float,
) : Parameters