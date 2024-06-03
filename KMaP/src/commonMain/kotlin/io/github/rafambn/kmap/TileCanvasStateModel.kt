package io.github.rafambn.kmap

import androidx.compose.ui.geometry.Offset
import io.github.rafambn.kmap.utils.CanvasPosition
import io.github.rafambn.kmap.utils.ScreenOffset

data class TileCanvasStateModel(
    val translation: Offset,
    val rotation: Float,
    val magnifierScale: Float,
    val visibleTilesList: TileLayers,
    val positionOffset: ScreenOffset,
    val zoomLevel: Int,
    val tileSize: Int
)
