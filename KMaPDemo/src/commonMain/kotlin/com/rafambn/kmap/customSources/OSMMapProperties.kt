package com.rafambn.kmap.customSources

import com.rafambn.kmap.mapProperties.*
import com.rafambn.kmap.mapProperties.border.BoundMapBorder
import com.rafambn.kmap.mapProperties.border.MapBorderType
import com.rafambn.kmap.mapProperties.border.OutsideTilesType
import com.rafambn.kmap.utils.Coordinates
import com.rafambn.kmap.utils.ProjectedCoordinates
import kotlin.math.*

data class OSMMapProperties(
    override val boundMap: BoundMapBorder = BoundMapBorder(MapBorderType.BOUND, MapBorderType.BOUND),
    override val outsideTiles: OutsideTilesType = OutsideTilesType.NONE,
    override val zoomLevels: ZoomLevelRange = OSMZoomLevelRange(),
    override val coordinatesRange: CoordinatesRange = OSMCoordinatesRange(),
    override val tileSize: TileDimension = TileDimension(256,256)
) : MapProperties {
    override fun toProjectedCoordinates(coordinates: Coordinates): ProjectedCoordinates = ProjectedCoordinates(
        coordinates.x,
        ln(tan(PI / 4 + (PI * coordinates.y) / 360)) / (PI / 85.051129)
    )

    override fun toCoordinates(projectedCoordinates: ProjectedCoordinates): Coordinates = Coordinates(
        projectedCoordinates.x,
        (atan(E.pow(projectedCoordinates.y * (PI / 85.051129))) - PI / 4) * 360 / PI
    )
}

data class OSMZoomLevelRange(override val max: Int = 19, override val min: Int = 0) : ZoomLevelRange

data class OSMCoordinatesRange(
    override val latitude: Latitude = Latitude(north = 85.051129, south = -85.051129),
    override val longitude: Longitude = Longitude(east = 180.0, west = -180.0)
) : CoordinatesRange
