package io.github.rafambn.kmap

class MapProperties(
    val boundMap: BoundMapBorder = BoundMapBorder(MapBorderType.BOUND, MapBorderType.BOUND),
    val outsideTiles: OutsideTilesType = OutsideTilesType.NONE,
    val zoomLevels: MapZoomlevelsRange,
    val mapCoordinatesRange: MapCoordinatesRange
)

data class BoundMapBorder(val horizontal: MapBorderType, val vertical: MapBorderType)

object OSMCoordinatesRange : MapCoordinatesRange {
    override val latitude: Latitude
        get() = Latitude(north = 85.0511, south = -85.0511, orientation = 1)
    override val longitute: Longitude
        get() = Longitude(east = 180.0, west = -180.0, orientation = -1)

}

object OSMZoomlevelsRange : MapZoomlevelsRange(19, 0)