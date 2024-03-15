package io.github.rafambn.kmap.model

import androidx.compose.ui.geometry.Offset
import io.github.rafambn.kmap.states.OutsideMapTiles

data class ScreenState(
    val position: Position,
    val zoomLevel: Int,
    val maxZoom: Int,
    val magnifierScale: Double,
    val angle: Float,
    val viewSize: Position,
    val mapSize: Position,
    val outsideTiles: OutsideMapTiles
)
