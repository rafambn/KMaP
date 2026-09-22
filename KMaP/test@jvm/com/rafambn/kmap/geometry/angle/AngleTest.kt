package com.rafambn.kmap.geometry.angle

import kotlin.test.Test
import kotlin.test.assertFailsWith

class AngleTest {
    @Test
    fun anglesRejectNonFiniteValues() {
        assertFailsWith<IllegalArgumentException> {
            Degrees(Double.NaN)
        }
        assertFailsWith<IllegalArgumentException> {
            Radians(Double.NEGATIVE_INFINITY)
        }
    }
}
