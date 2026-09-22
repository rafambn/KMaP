package com.rafambn.kmap.utils

import com.rafambn.kmap.mapProperties.coordinates.CardinalRange
import de.infix.testBalloon.framework.core.testSuite
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

val MathUtilsTest by testSuite {
    test("wraps tile indices at zoom limits") {
        assertEquals(0, (-1).loopInZoom(0))
        assertEquals(1073741823, (-1).loopInZoom(30))
        assertEquals(1073741823, Int.MAX_VALUE.loopInZoom(30))
        assertEquals(0, Int.MIN_VALUE.loopInZoom(30))
        assertEquals(0, 1073741824.loopInZoom(30))
    }

    test("rejects zooms that would recycle shift bits") {
        for (zoom in listOf(-1, 31, 32, 64)) {
            assertFailsWith<IllegalArgumentException> { 0.loopInZoom(zoom) }
        }
    }

    test("wraps coordinates inside either cardinal orientation") {
        val range = object : CardinalRange {
            override val start = 180.0
            override val end = -180.0
        }

        assertEquals(-180.0, 180.0.loopInRange(range))
        assertEquals(179.0, (-181.0).loopInRange(range))
        assertEquals(-179.0, 181.0.loopInRange(range))
    }

    test("wraps negative and positive tile coordinates") {
        assertEquals(511.0, (-1.0).loopInRange(512.0))
        assertEquals(1.0, 513.0.loopInRange(512.0))
        assertEquals(0.0, 512.0.loopInRange(512.0))
    }

    test("interpolates and floors both numeric types") {
        assertEquals(6.0, lerp(2.0, 10.0, 0.5))
        assertEquals(-2, (-1.2F).toIntFloor())
        assertEquals(1, 1.2F.toIntFloor())
        assertEquals(-2, (-1.2).toIntFloor())
        assertEquals(1, 1.2.toIntFloor())
    }
}
