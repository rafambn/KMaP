package com.rafambn.kmap.mapProperties

import com.rafambn.kmap.mapProperties.border.BoundMapBorder
import com.rafambn.kmap.mapProperties.border.OutsideTilesType
import com.rafambn.kmap.utils.TilePoint
import com.rafambn.kmap.utils.Coordinates

interface MapProperties {
    val boundMap: BoundMapBorder
    val outsideTiles: OutsideTilesType
    val zoomLevels: ZoomLevelRange
    val coordinatesRange: CoordinatesRange
    val tileSize: Int

    fun toTilePoint(coordinates: Coordinates): TilePoint

    fun toCoordinates(tilePoint: TilePoint): Coordinates
}