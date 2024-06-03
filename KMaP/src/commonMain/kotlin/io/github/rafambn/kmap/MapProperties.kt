package io.github.rafambn.kmap

class MapProperties(
    val boundMap: BoundMapBorder = BoundMapBorder(MapBorderType.BOUND, MapBorderType.BOUND),
    val outsideTiles: OutsideTilesType = OutsideTilesType.NONE,
    val zoomLevels: MapZoomlevelsRange,
    val mapCoordinatesRange: MapCoordinatesRange,
    val tileSize: Int
)

data class BoundMapBorder(val horizontal: MapBorderType, val vertical: MapBorderType)

object OSMCoordinatesRange : MapCoordinatesRange { //TODO create an interface
    override val latitude: Latitude
        get() = Latitude(north = 85.051129, south = -85.051129, orientation = 1)
    override val longitute: Longitude
        get() = Longitude(east = 180.0, west = -180.0, orientation = -1)

}

object OSMZoomlevelsRange : MapZoomlevelsRange(19, 0)
