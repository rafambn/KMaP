package io.github.rafambn.kmap

import androidx.compose.ui.geometry.Offset

data class TileCanvasStateModel(
    val translation: Offset,
    val rotation: Float,
    val magnifierScale: Float,
    val visibleTilesList: TileLayers,
    val positionOffset: Position,
    val zoomLevel: Int
)
