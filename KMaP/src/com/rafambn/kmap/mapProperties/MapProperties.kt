package com.rafambn.kmap.mapProperties

import com.rafambn.kmap.geometry.plane.Coordinates
import com.rafambn.kmap.geometry.plane.ProjectedCoordinates
import com.rafambn.kmap.mapProperties.border.MapBoundaryBehavior
import com.rafambn.kmap.mapProperties.border.TileRepeatMode
import com.rafambn.kmap.mapProperties.coordinates.CoordinatesRange

interface MapProperties {
    /** Controls whether the camera position clamps or wraps on each axis. */
    val boundaryBehavior: MapBoundaryBehavior

    /** Controls whether tiles repeat beyond map bounds, independently of camera movement. */
    val tileRepeatMode: TileRepeatMode
    val zoomLevels: ZoomLevelRange
    val coordinatesRange: CoordinatesRange
    val tileSize: TileDimension

    fun toProjectedCoordinates(coordinates: Coordinates): ProjectedCoordinates

    fun toCoordinates(projectedCoordinates: ProjectedCoordinates): Coordinates
}
