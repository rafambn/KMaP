package com.rafambn.kmap.mapProperties

import com.rafambn.kmap.geometry.plane.ProjectedCoordinates

/**
 * Projected coordinates at the map's top-left and bottom-right corners before camera rotation.
 * Both corners use the units returned by [MapProperties.toProjectedCoordinates].
 * Either axis may increase or decrease toward the bottom-right corner.
 */
data class ProjectedBounds(
    val topLeft: ProjectedCoordinates,
    val bottomRight: ProjectedCoordinates,
) {
    /** Signed change in projected X from the left edge to the right edge. */
    val xSpan: Double = bottomRight.x - topLeft.x

    /** Signed change in projected Y from the top edge to the bottom edge. */
    val ySpan: Double = bottomRight.y - topLeft.y

    init {
        require(xSpan.isFinite() && xSpan != 0.0) { "Projected X bounds must have a finite, non-zero span" }
        require(ySpan.isFinite() && ySpan != 0.0) { "Projected Y bounds must have a finite, non-zero span" }
    }
}
