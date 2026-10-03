package com.rafambn.kmap.mapProperties

import com.rafambn.kmap.geometry.plane.ProjectedCoordinates
import de.infix.testBalloon.framework.core.testSuite
import kotlin.test.assertFailsWith

val ProjectedBoundsTest by testSuite {
    test("rejectsZeroAndNonFiniteSpansOnEitherAxis") {
        for (end in listOf(0.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)) {
            assertFailsWith<IllegalArgumentException> {
                ProjectedBounds(ProjectedCoordinates.Zero, ProjectedCoordinates(end, 1.0))
            }
            assertFailsWith<IllegalArgumentException> {
                ProjectedBounds(ProjectedCoordinates.Zero, ProjectedCoordinates(1.0, end))
            }
        }
    }

    test("rejectsFiniteEndpointsWhoseSpanOverflows") {
        assertFailsWith<IllegalArgumentException> {
            ProjectedBounds(
                topLeft = ProjectedCoordinates(-Double.MAX_VALUE, 0.0),
                bottomRight = ProjectedCoordinates(Double.MAX_VALUE, 1.0),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            ProjectedBounds(
                topLeft = ProjectedCoordinates(0.0, -Double.MAX_VALUE),
                bottomRight = ProjectedCoordinates(1.0, Double.MAX_VALUE),
            )
        }
    }
}
