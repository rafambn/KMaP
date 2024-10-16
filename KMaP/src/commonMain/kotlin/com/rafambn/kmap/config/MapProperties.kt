package com.rafambn.kmap.config

import com.rafambn.kmap.config.border.BoundMapBorder
import com.rafambn.kmap.config.border.OutsideTilesType
import com.rafambn.kmap.config.characteristics.MapCoordinatesRange
import com.rafambn.kmap.config.characteristics.MapZoomLevelsRange
import com.rafambn.kmap.utils.offsets.CanvasPosition
import com.rafambn.kmap.utils.offsets.ProjectedCoordinates

interface MapProperties {
    val boundMap: BoundMapBorder
    val outsideTiles: OutsideTilesType
    val zoomLevels: MapZoomLevelsRange
    val mapCoordinatesRange: MapCoordinatesRange
    val tileSize: Int

    fun toCanvasPosition(projectedCoordinates: ProjectedCoordinates): CanvasPosition

    fun toProjectedCoordinates(canvasPosition: CanvasPosition): ProjectedCoordinates
}