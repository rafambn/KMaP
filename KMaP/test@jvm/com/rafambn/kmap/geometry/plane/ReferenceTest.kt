package com.rafambn.kmap.geometry.plane

import com.rafambn.kmap.geometry.angle.Radians
import com.rafambn.kmap.geometry.angle.rotate
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class ReferenceTest {
    @Test
    fun mapReferencesRejectNonFiniteValues() {
        assertFailsWith<IllegalArgumentException> {
            Coordinates(Double.NaN, 0.0)
        }
        assertFailsWith<IllegalArgumentException> {
            TilePoint(0.0, Double.POSITIVE_INFINITY)
        }
    }

    @Test
    fun arithmeticPreservesTheReferenceType() {
        assertEquals(TilePoint(4.0, 6.0), TilePoint(1.0, 2.0) + TilePoint(3.0, 4.0))
        assertEquals(ScreenOffset(-2.0, -4.0), ScreenOffset(1.0, 2.0) - ScreenOffset(3.0, 6.0))
        assertEquals(Coordinates(-1.0, -2.0), -Coordinates(1.0, 2.0))
        assertEquals(ProjectedCoordinates(2.0, 4.0), ProjectedCoordinates(1.0, 2.0) * 2.0)
        assertEquals(DifferentialScreenOffset(0.5, 1.0), DifferentialScreenOffset(1.0, 2.0) / 2.0)
        assertEquals(CanvasDrawReference(4.0, 6.0), CanvasDrawReference(1.0, 2.0) + CanvasDrawReference(3.0, 4.0))
    }

    @Test
    fun differentReferenceTypesAreNotEqual() {
        val coordinates: Reference = Coordinates(1.0, 2.0)
        val screenOffset: Reference = ScreenOffset(1.0, 2.0)

        assertNotEquals(coordinates, screenOffset)
    }

    @Test
    fun typedTransformationsReturnTheInputType() {
        val rotated: TilePoint = TilePoint(1.0, 0.0).rotate(Radians(PI / 2.0))
        val interpolated: TilePoint = lerp(TilePoint.Zero, TilePoint(4.0, 8.0), 0.5)

        assertEquals(0.0, rotated.x, 0.0000000001)
        assertEquals(1.0, rotated.y, 0.0000000001)
        assertEquals(TilePoint(2.0, 4.0), interpolated)
    }
}
