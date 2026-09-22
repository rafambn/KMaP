package com.rafambn.kmap.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MathUtilsTest {
    @Test
    fun wrapsTileIndicesAtZoomLimits() {
        assertEquals(0, (-1).loopInZoom(0))
        assertEquals(1073741823, (-1).loopInZoom(30))
        assertEquals(1073741823, Int.MAX_VALUE.loopInZoom(30))
        assertEquals(0, Int.MIN_VALUE.loopInZoom(30))
        assertEquals(0, 1073741824.loopInZoom(30))
    }

    @Test
    fun rejectsZoomsThatWouldRecycleShiftBits() {
        for (zoom in listOf(-1, 31, 32, 64)) {
            assertFailsWith<IllegalArgumentException> { 0.loopInZoom(zoom) }
        }
    }
}
