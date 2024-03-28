package io.github.rafambn.kmap.model

import io.github.rafambn.kmap.ranges.MapCoordinatesRange

data class ScreenState(
    val viewPort: VeiwPort,
    val zoomLevel: Int,
    val outsideTiles: MapCoordinatesRange,
)
