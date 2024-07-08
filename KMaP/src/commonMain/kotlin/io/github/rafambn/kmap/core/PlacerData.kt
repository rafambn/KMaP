package io.github.rafambn.kmap.core

import io.github.rafambn.kmap.utils.Degrees
import io.github.rafambn.kmap.utils.offsets.ProjectedCoordinates

data class PlacerData(
    var coordinates: ProjectedCoordinates,
    val tag: String = "",
    val alpha: Float = 1F,
    val drawPosition: DrawPosition = DrawPosition.TOP_LEFT,
    val zIndex: Float = 1F,
    val scaleWithMap: Boolean = false,
    val zoomToFix: Float = 0F,
    val rotateWithMap: Boolean = false,
    val rotation: Degrees = 0.0,
)

data class CanvasData(
    val zIndex: Float = 0F,
    val alpha: Float = 1F
)

data class GroupData(
//TODO add number of grouped placers
    val tag: String,
    val alpha: Float = 1F,
    val drawPosition: DrawPosition = DrawPosition.TOP_LEFT,
    val rotateWithMap: Boolean = false,
    val rotation: Degrees = 0.0,
)