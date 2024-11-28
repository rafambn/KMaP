package com.rafambn.kmap.customSources

import com.rafambn.kmap.config.MapProperties
import com.rafambn.kmap.config.border.BoundMapBorder
import com.rafambn.kmap.config.border.MapBorderType
import com.rafambn.kmap.config.border.OutsideTilesType
import com.rafambn.kmap.config.characteristics.Latitude
import com.rafambn.kmap.config.characteristics.Longitude
import com.rafambn.kmap.config.characteristics.MapCoordinatesRange
import com.rafambn.kmap.config.characteristics.MapZoomLevelsRange
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
