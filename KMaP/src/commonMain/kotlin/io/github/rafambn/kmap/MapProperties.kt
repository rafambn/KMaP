package io.github.rafambn.kmap

import io.github.rafambn.kmap.utils.CanvasPosition
import io.github.rafambn.kmap.utils.ProjectedCoordinates
import kotlin.math.PI
import kotlin.math.ln
import kotlin.math.tan

class MapProperties(
    val boundMap: BoundMapBorder = BoundMapBorder(MapBorderType.BOUND, MapBorderType.BOUND),
    val outsideTiles: OutsideTilesType = OutsideTilesType.NONE,
    val mapSource: MapSource = OSMMapSource
)

data class BoundMapBorder(val horizontal: MapBorderType, val vertical: MapBorderType)

interface MapSource {
    val zoomLevels: MapZoomlevelsRange
    val mapCoordinatesRange: MapCoordinatesRange
    val tileSize: Int

    fun toCanvasPosition(projectedCoordinates: ProjectedCoordinates): CanvasPosition
}

object OSMMapSource : MapSource {
    override val zoomLevels = OSMZoomlevelsRange
    override val mapCoordinatesRange = OSMCoordinatesRange
    override val tileSize = 256 //TODO add source future -- online, db, cache or mapFile

    override fun toCanvasPosition(projectedCoordinates: ProjectedCoordinates): CanvasPosition = CanvasPosition(
        projectedCoordinates.horizontal,
        ln(tan(PI / 4 + (PI * projectedCoordinates.vertical) / 360)) / (PI / 85.051129)
    )
}

object OSMCoordinatesRange : MapCoordinatesRange {
    override val latitude: Latitude
        get() = Latitude(north = 85.051129, south = -85.051129, orientation = 1)
    override val longitute: Longitude
        get() = Longitude(east = 180.0, west = -180.0, orientation = -1)

}

object OSMZoomlevelsRange : MapZoomlevelsRange(19, 0)
