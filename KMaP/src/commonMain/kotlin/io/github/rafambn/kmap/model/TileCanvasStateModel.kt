package io.github.rafambn.kmap.model

import androidx.compose.ui.geometry.Offset
import io.github.rafambn.kmap.core.TileLayers
import io.github.rafambn.kmap.utils.offsets.CanvasDrawReference

data class TileCanvasStateModel(
    val translation: Offset,
    val rotation: Float,
    val magnifierScale: Float,
    val visibleTilesList: TileLayers,
    val positionOffset: CanvasDrawReference,
    val zoomLevel: Int,
    val tileSize: Int
)
