package io.github.rafambn.kmap.model

import io.github.rafambn.kmap.config.characteristics.MapCoordinatesRange
import io.github.rafambn.kmap.config.border.OutsideTilesType

data class ScreenState(
    val viewPort: BoundingBox,
    val zoomLevel: Int,
    val coordinatesRange: MapCoordinatesRange,
    val outsideTiles: OutsideTilesType,
    val maxZoom: Int,
    val minZoom: Int,
)
