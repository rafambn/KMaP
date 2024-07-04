package io.github.rafambn.kmap.model

import androidx.compose.ui.geometry.Offset
import io.github.rafambn.kmap.utils.offsets.CanvasDrawReference

data class TileCanvasStateModel(
    val translation: Offset,
    val rotation: Float,
    val magnifierScale: Float,
    val positionOffset: CanvasDrawReference,
    val tileSize: Int,
    val visibleTileSpecs: List<TileSpecs>,
    val zoomLevel: Int
)
