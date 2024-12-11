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
import kotlin.math.E
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.tan

data class OSMMapProperties(
    override val boundMap: BoundMapBorder = BoundMapBorder(MapBorderType.BOUND, MapBorderType.BOUND),
    override val outsideTiles: OutsideTilesType = OutsideTilesType.NONE,
    override val zoomLevels: MapZoomLevelsRange = OSMZoomLevelsRange(),
    override val mapCoordinatesRange: MapCoordinatesRange = OSMCoordinatesRange(),
    override val tileSize: Int = 256
) : MapProperties {
    override fun toCanvasPosition(projectedCoordinates: ProjectedCoordinates): CanvasPosition = CanvasPosition(
        projectedCoordinates.longitude,
        ln(tan(PI / 4 + (PI * projectedCoordinates.latitude) / 360)) / (PI / 85.051129)
    )

    override fun toProjectedCoordinates(canvasPosition: CanvasPosition): ProjectedCoordinates = ProjectedCoordinates(
        canvasPosition.horizontal,
        (atan(E.pow(canvasPosition.vertical * (PI / 85.051129))) - PI / 4) * 360 / PI
    )
}

data class OSMZoomLevelsRange(override val max: Int = 19, override val min: Int = 0) : MapZoomLevelsRange

data class OSMCoordinatesRange(
    override val latitude: Latitude = Latitude(north = 85.051129, south = -85.051129),
    override val longitude: Longitude = Longitude(east = 180.0, west = -180.0)
) : MapCoordinatesRange
