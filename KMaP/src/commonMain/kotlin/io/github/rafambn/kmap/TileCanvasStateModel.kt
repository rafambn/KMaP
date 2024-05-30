package io.github.rafambn.kmap

import androidx.compose.ui.geometry.Offset
import io.github.rafambn.kmap.utils.CanvasPosition

data class TileCanvasStateModel(
    val translation: Offset,
    val rotation: Float,
    val magnifierScale: Float,
    val visibleTilesList: TileLayers,
    val positionOffset: CanvasPosition,
    val zoomLevel: Int
)
