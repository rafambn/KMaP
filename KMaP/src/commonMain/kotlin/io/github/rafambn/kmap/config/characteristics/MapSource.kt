package io.github.rafambn.kmap.config.characteristics

import io.github.rafambn.kmap.model.ResultTile
import io.github.rafambn.kmap.utils.offsets.CanvasPosition
import io.github.rafambn.kmap.utils.offsets.ProjectedCoordinates

interface MapSource {
    val zoomLevels: MapZoomLevelsRange
    val mapCoordinatesRange: MapCoordinatesRange
    val tileSize: Int

    fun toCanvasPosition(projectedCoordinates: ProjectedCoordinates): CanvasPosition

    fun toProjectedCoordinates(canvasPosition: CanvasPosition): ProjectedCoordinates

    suspend fun getTile(zoom: Int, row: Int, column: Int): ResultTile
}