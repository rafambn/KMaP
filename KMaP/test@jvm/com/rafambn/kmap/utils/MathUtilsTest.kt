package com.rafambn.kmap.utils

import de.infix.testBalloon.framework.core.testSuite
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

val MathUtilsTest by testSuite {
    test("wrapsTileIndicesAtZoomLimits") {
        assertEquals(0, (-1).loopInZoom(0))
        assertEquals(1073741823, (-1).loopInZoom(30))
        assertEquals(1073741823, Int.MAX_VALUE.loopInZoom(30))
        assertEquals(0, Int.MIN_VALUE.loopInZoom(30))
        assertEquals(0, 1073741824.loopInZoom(30))
    }

    test("rejectsZoomsThatWouldRecycleShiftBits") {
        for (zoom in listOf(-1, 31, 32, 64)) {
            assertFailsWith<IllegalArgumentException> { 0.loopInZoom(zoom) }
        }
    }
}
