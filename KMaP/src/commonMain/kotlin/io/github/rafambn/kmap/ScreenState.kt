package io.github.rafambn.kmap

data class ScreenState(
    val viewPort: BoundingBox,
    val zoomLevel: Int,
    val coordinatesRange: MapCoordinatesRange,
    val outsideTiles: OutsideTilesType
)
