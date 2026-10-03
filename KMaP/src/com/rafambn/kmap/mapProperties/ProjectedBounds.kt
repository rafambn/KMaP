package com.rafambn.kmap.mapProperties

import com.rafambn.kmap.geometry.plane.ProjectedCoordinates
import kotlin.math.abs

/**
 * Projected coordinates at the map's top-left and bottom-right corners before camera rotation.
 * Both corners use the units returned by [MapProperties.toProjectedCoordinates].
 * Either axis may increase or decrease toward the bottom-right corner.
 */
data class ProjectedBounds(
    val topLeft: ProjectedCoordinates,
    val bottomRight: ProjectedCoordinates,
) {
    /** Positive distance in projected X between the left and right edges. */
    val xSpan: Double = abs(bottomRight.x - topLeft.x)

    /** Positive distance in projected Y between the top and bottom edges. */
    val ySpan: Double = abs(bottomRight.y - topLeft.y)

    /** 1 if projected X increases toward the right edge, -1 if it decreases. */
    val xDirection: Int = bottomRight.x.compareTo(topLeft.x)

    /** 1 if projected Y increases toward the bottom edge, -1 if it decreases. */
    val yDirection: Int = bottomRight.y.compareTo(topLeft.y)

    init {
        require(xSpan.isFinite() && xSpan > 0.0) { "Projected X bounds must have a finite, positive span" }
        require(ySpan.isFinite() && ySpan > 0.0) { "Projected Y bounds must have a finite, positive span" }
    }
}
