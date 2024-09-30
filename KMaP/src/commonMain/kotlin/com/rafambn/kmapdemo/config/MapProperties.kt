package com.rafambn.kmapdemo.config

import com.rafambn.kmapdemo.config.border.BoundMapBorder
import com.rafambn.kmapdemo.config.border.OutsideTilesType
import com.rafambn.kmapdemo.config.characteristics.MapCoordinatesRange
import com.rafambn.kmapdemo.config.characteristics.MapZoomLevelsRange
import com.rafambn.kmapdemo.utils.offsets.CanvasPosition
import com.rafambn.kmapdemo.utils.offsets.ProjectedCoordinates

interface MapProperties {
    val boundMap: BoundMapBorder
    val outsideTiles: OutsideTilesType
    val zoomLevels: MapZoomLevelsRange
    val mapCoordinatesRange: MapCoordinatesRange
    val tileSize: Int

    fun toCanvasPosition(projectedCoordinates: ProjectedCoordinates): CanvasPosition

    fun toProjectedCoordinates(canvasPosition: CanvasPosition): ProjectedCoordinates
}