package com.rafambn.kmap.customSources

import com.rafambn.kmap.mapProperties.MapProperties
import com.rafambn.kmap.mapProperties.border.BoundMapBorder
import com.rafambn.kmap.mapProperties.border.MapBorderType
import com.rafambn.kmap.mapProperties.border.OutsideTilesType
import com.rafambn.kmap.mapProperties.CoordinatesRange
import com.rafambn.kmap.mapProperties.Latitude
import com.rafambn.kmap.mapProperties.Longitude
import com.rafambn.kmap.mapProperties.ZoomLevelRange
import com.rafambn.kmap.tiles.TileDimension
import com.rafambn.kmap.utils.TilePoint
import com.rafambn.kmap.utils.Coordinates

data class SimpleMapProperties(
    override val boundMap: BoundMapBorder = BoundMapBorder(MapBorderType.BOUND, MapBorderType.BOUND),
    override val outsideTiles: OutsideTilesType = OutsideTilesType.NONE,
    override val zoomLevels: ZoomLevelRange = SimpleZoomLevelRange(),
    override val coordinatesRange: CoordinatesRange = SimpleCoordinatesRange(),
    override val tileSize: TileDimension = TileDimension(900, 900)
) : MapProperties {
    override fun toTilePoint(coordinates: Coordinates): TilePoint = TilePoint(
        coordinates.longitude,
        coordinates.latitude
    )

    override fun toCoordinates(tilePoint: TilePoint): Coordinates = Coordinates(
        tilePoint.horizontal,
        tilePoint.vertical
    )
}

data class SimpleZoomLevelRange(override val max: Int = 2, override val min: Int = 0) : ZoomLevelRange

data class SimpleCoordinatesRange(
    override val latitude: Latitude = Latitude(north = 90.0, south = -90.0),
    override val longitude: Longitude = Longitude(west = -180.0, east = 180.0)
) : CoordinatesRange
