package io.github.rafambn.kmap.model

import androidx.compose.ui.geometry.Offset
import io.github.rafambn.kmap.states.OutsideMapTiles

data class ScreenState(
    val position: Offset,
    val zoomLevel: Int,
    val maxZoom: Float,
    val magnifierScale: Float,
    val angle: Float,
    val viewSize: Offset,
    val mapSize: Offset,
    val outsideTiles: OutsideMapTiles
)
