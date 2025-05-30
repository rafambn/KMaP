package com.rafambn.kmap.customSources

import com.rafambn.kmap.mapProperties.MapProperties
import com.rafambn.kmap.mapProperties.border.BoundMapBorder
import com.rafambn.kmap.mapProperties.border.MapBorderType
import com.rafambn.kmap.mapProperties.border.OutsideTilesType
import com.rafambn.kmap.mapProperties.CoordinatesRange
import com.rafambn.kmap.mapProperties.Latitude
import com.rafambn.kmap.mapProperties.Longitude
import com.rafambn.kmap.mapProperties.ZoomLevelRange
import com.rafambn.kmap.components.canvas.tiled.TileDimension
import com.rafambn.kmap.utils.TilePoint
import com.rafambn.kmap.utils.Coordinates
import kotlin.math.E
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.tan

data class OSMMapProperties(
    override val boundMap: BoundMapBorder = BoundMapBorder(MapBorderType.BOUND, MapBorderType.BOUND),
    override val outsideTiles: OutsideTilesType = OutsideTilesType.NONE,
    override val zoomLevels: ZoomLevelRange = OSMZoomLevelRange(),
    override val coordinatesRange: CoordinatesRange = OSMCoordinatesRange(),
    override val tileSize: TileDimension = TileDimension(256,256)
) : MapProperties {
    override fun toTilePoint(coordinates: Coordinates): TilePoint = TilePoint(
        coordinates.longitude,
        ln(tan(PI / 4 + (PI * coordinates.latitude) / 360)) / (PI / 85.051129)
    )

    override fun toCoordinates(tilePoint: TilePoint): Coordinates = Coordinates(
        tilePoint.horizontal,
        (atan(E.pow(tilePoint.vertical * (PI / 85.051129))) - PI / 4) * 360 / PI
    )
}

data class OSMZoomLevelRange(override val max: Int = 19, override val min: Int = 0) : ZoomLevelRange

data class OSMCoordinatesRange(
    override val latitude: Latitude = Latitude(north = 85.051129, south = -85.051129),
    override val longitude: Longitude = Longitude(east = 180.0, west = -180.0)
) : CoordinatesRange
