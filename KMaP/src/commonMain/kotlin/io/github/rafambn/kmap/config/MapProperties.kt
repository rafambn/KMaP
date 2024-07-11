package io.github.rafambn.kmap.config

import io.github.rafambn.kmap.config.border.BoundMapBorder
import io.github.rafambn.kmap.config.border.OutsideTilesType
import io.github.rafambn.kmap.config.characteristics.MapCoordinatesRange
import io.github.rafambn.kmap.config.characteristics.MapZoomLevelsRange
import io.github.rafambn.kmap.utils.offsets.CanvasPosition
import io.github.rafambn.kmap.utils.offsets.ProjectedCoordinates

interface MapProperties {
    val boundMap: BoundMapBorder
    val outsideTiles: OutsideTilesType
    val zoomLevels: MapZoomLevelsRange
    val mapCoordinatesRange: MapCoordinatesRange
    val tileSize: Int

    fun toCanvasPosition(projectedCoordinates: ProjectedCoordinates): CanvasPosition

    fun toProjectedCoordinates(canvasPosition: CanvasPosition): ProjectedCoordinates
}