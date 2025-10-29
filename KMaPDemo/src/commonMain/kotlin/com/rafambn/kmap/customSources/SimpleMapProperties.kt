package com.rafambn.kmap.customSources

import com.rafambn.kmap.mapProperties.*
import com.rafambn.kmap.mapProperties.border.BoundMapBorder
import com.rafambn.kmap.mapProperties.border.MapBorderType
import com.rafambn.kmap.mapProperties.border.OutsideTilesType
import com.rafambn.kmap.mapSource.tiled.TileDimension
import com.rafambn.kmap.utils.Coordinates
import com.rafambn.kmap.utils.ProjectedCoordinates

data class SimpleMapProperties(
    override val boundMap: BoundMapBorder = BoundMapBorder(MapBorderType.BOUND, MapBorderType.BOUND),
    override val outsideTiles: OutsideTilesType = OutsideTilesType.NONE,
    override val zoomLevels: ZoomLevelRange = SimpleZoomLevelRange(),
    override val coordinatesRange: CoordinatesRange = SimpleCoordinatesRange(),
    override val tileSize: TileDimension = TileDimension(900, 900)
) : MapProperties {
    override fun toProjectedCoordinates(coordinates: Coordinates): ProjectedCoordinates = ProjectedCoordinates(
        coordinates.x,
        coordinates.y
    )

    override fun toCoordinates(projectedCoordinates: ProjectedCoordinates): Coordinates = Coordinates(
        projectedCoordinates.x,
        projectedCoordinates.y
    )
}

data class SimpleZoomLevelRange(override val max: Int = 2, override val min: Int = 0) : ZoomLevelRange

data class SimpleCoordinatesRange(
    override val latitude: Latitude = Latitude(north = 90.0, south = -90.0),
    override val longitude: Longitude = Longitude(west = -180.0, east = 180.0)
) : CoordinatesRange
