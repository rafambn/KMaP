package com.rafambn.kmap.geometry.angle

import de.infix.testBalloon.framework.core.testSuite
import kotlin.test.assertFailsWith

val AngleTest by testSuite {
    test("anglesRejectNonFiniteValues") {
        assertFailsWith<IllegalArgumentException> {
            Degrees(Double.NaN)
        }
        assertFailsWith<IllegalArgumentException> {
            Radians(Double.NEGATIVE_INFINITY)
        }
    }
}
