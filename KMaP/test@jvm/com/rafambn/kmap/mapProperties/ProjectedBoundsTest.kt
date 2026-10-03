package com.rafambn.kmap.mapProperties

import com.rafambn.kmap.geometry.plane.ProjectedCoordinates
import de.infix.testBalloon.framework.core.testSuite
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

val ProjectedBoundsTest by testSuite {
    test("spansStayPositiveWhenAxesAreReversed") {
        for ((left, right, xDirection) in listOf(Triple(0.0, 100.0, 1), Triple(100.0, 0.0, -1))) {
            for ((top, bottom, yDirection) in listOf(Triple(0.0, 100.0, 1), Triple(100.0, 0.0, -1))) {
                val bounds = ProjectedBounds(
                    topLeft = ProjectedCoordinates(left, top),
                    bottomRight = ProjectedCoordinates(right, bottom),
                )

                assertEquals(100.0, bounds.xSpan)
                assertEquals(100.0, bounds.ySpan)
                assertEquals(xDirection, bounds.xDirection)
                assertEquals(yDirection, bounds.yDirection)
            }
        }
    }

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
