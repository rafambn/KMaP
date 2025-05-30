package com.rafambn.kmap.mapProperties

import com.rafambn.kmap.mapProperties.border.BoundMapBorder
import com.rafambn.kmap.mapProperties.border.OutsideTilesType
import com.rafambn.kmap.components.canvas.tiled.TileDimension
import com.rafambn.kmap.utils.TilePoint
import com.rafambn.kmap.utils.Coordinates

interface MapProperties {
    val boundMap: BoundMapBorder
    val outsideTiles: OutsideTilesType
    val zoomLevels: ZoomLevelRange
    val coordinatesRange: CoordinatesRange
    val tileSize: TileDimension //TODO change name to something the convey the info that is the size of map at zoom 0

    fun toTilePoint(coordinates: Coordinates): TilePoint

    fun toCoordinates(tilePoint: TilePoint): Coordinates
}
