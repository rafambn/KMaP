package com.rafambn.kmap.customSources

import com.rafambn.kmap.mapProperties.MapProperties
import com.rafambn.kmap.mapProperties.border.BoundMapBorder
import com.rafambn.kmap.mapProperties.border.MapBorderType
import com.rafambn.kmap.mapProperties.border.OutsideTilesType
import com.rafambn.kmap.mapProperties.coordinates.Latitude
import com.rafambn.kmap.mapProperties.coordinates.Longitude
import com.rafambn.kmap.mapProperties.coordinates.MapCoordinatesRange
import com.rafambn.kmap.mapProperties.MapZoomLevelsRange
import com.rafambn.kmap.utils.CanvasPosition
import com.rafambn.kmap.utils.ProjectedCoordinates

data class SimpleMapProperties(
    override val boundMap: BoundMapBorder = BoundMapBorder(MapBorderType.BOUND, MapBorderType.BOUND),
    override val outsideTiles: OutsideTilesType = OutsideTilesType.NONE,
    override val zoomLevels: MapZoomLevelsRange = SimpleMapZoomLevelsRange(),
    override val mapCoordinatesRange: MapCoordinatesRange = SimpleMapCoordinatesRange(),
    override val tileSize: Int = 900
) : MapProperties {
    override fun toCanvasPosition(projectedCoordinates: ProjectedCoordinates): CanvasPosition = CanvasPosition(
        projectedCoordinates.longitude,
        projectedCoordinates.latitude
    )

    override fun toProjectedCoordinates(canvasPosition: CanvasPosition): ProjectedCoordinates = ProjectedCoordinates(
        canvasPosition.horizontal,
        canvasPosition.vertical
    )
}

data class SimpleMapZoomLevelsRange(override val max: Int = 2, override val min: Int = 0) : MapZoomLevelsRange

data class SimpleMapCoordinatesRange(
    override val latitude: Latitude = Latitude(north = 90.0, south = -90.0),
    override val longitude: Longitude = Longitude(east = 180.0, west = -180.0)
) : MapCoordinatesRange
