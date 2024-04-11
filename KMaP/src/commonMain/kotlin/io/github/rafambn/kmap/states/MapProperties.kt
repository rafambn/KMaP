package io.github.rafambn.kmap.states

import io.github.rafambn.kmap.enums.MapBorderType
import io.github.rafambn.kmap.enums.OutsideTilesType
import io.github.rafambn.kmap.ranges.Latitude
import io.github.rafambn.kmap.ranges.Longitude
import io.github.rafambn.kmap.ranges.MapCoordinatesRange
import io.github.rafambn.kmap.ranges.MapZoomlevelsRange

class MapProperties(
    val boundMap: BoundMapBorder = BoundMapBorder(MapBorderType.BOUND, MapBorderType.BOUND),
    val outsideTiles: OutsideTilesType = OutsideTilesType.LOOP,
    val zoomLevels: MapZoomlevelsRange,
    val mapCoordinatesRange: MapCoordinatesRange
)

data class BoundMapBorder(val horizontal: MapBorderType, val vertical: MapBorderType)

object OSMCoordinatesRange : MapCoordinatesRange {
    override val latitude: Latitude
        get() = Latitude(north = 85.0511, south = -85.0511)
    override val longitute: Longitude
        get() = Longitude(east = 180.0, west = -180.0)

}

object OSMZoomlevelsRange : MapZoomlevelsRange(19, 0)
