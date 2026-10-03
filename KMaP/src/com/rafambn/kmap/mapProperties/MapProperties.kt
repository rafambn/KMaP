package com.rafambn.kmap.mapProperties

import com.rafambn.kmap.MapState
import com.rafambn.kmap.geometry.plane.Coordinates
import com.rafambn.kmap.geometry.plane.ProjectedCoordinates
import com.rafambn.kmap.mapProperties.border.BoundMapBorder
import com.rafambn.kmap.mapProperties.border.OutsideTilesType

/**
 * Map configuration and coordinate projection, fixed for the lifetime of a [MapState].
 * Implementations must keep property values and projection behavior unchanged after construction.
 * Create a new [MapState] to use a different configuration or projection.
 */
interface MapProperties {
    val boundMap: BoundMapBorder
    val outsideTiles: OutsideTilesType
    val zoomLevels: ZoomLevelRange
    /** Bounds in the same coordinate system and units as [toProjectedCoordinates]. */
    val projectedBounds: ProjectedBounds

    /** Tile dimensions in density-independent units at integer zoom levels. */
    val tileSize: TileDimension

    /**
     * Projects application coordinates into the plane described by [projectedBounds].
     * Implementations must document input units, the supported domain, and behavior outside it.
     * Valid inputs must produce finite coordinates and approximately round-trip through
     * [toCoordinates], allowing for floating-point precision.
     */
    fun toProjectedCoordinates(coordinates: Coordinates): ProjectedCoordinates

    /**
     * Reverses [toProjectedCoordinates], returning coordinates in the application's input units.
     * Implementations must document how they handle projected coordinates outside their domain.
     * Bounds conversion does not clamp inputs, so callers may request coordinates outside the map.
     */
    fun toCoordinates(projectedCoordinates: ProjectedCoordinates): Coordinates
}
