package com.rafambn.kmap.mapProperties

import com.rafambn.kmap.mapProperties.border.BoundMapBorder
import com.rafambn.kmap.mapProperties.border.OutsideTilesType
import com.rafambn.kmap.mapSource.tiled.TileDimension
import com.rafambn.kmap.utils.Coordinates
import com.rafambn.kmap.utils.ProjectedCoordinates

interface MapProperties {
    val boundMap: BoundMapBorder
    val outsideTiles: OutsideTilesType
    val zoomLevels: ZoomLevelRange
    val coordinatesRange: CoordinatesRange
    val tileSize: TileDimension

    fun toProjectedCoordinates(coordinates: Coordinates): ProjectedCoordinates

    fun toCoordinates(projectedCoordinates: ProjectedCoordinates): Coordinates
}
