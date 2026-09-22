package com.rafambn.kmap.camera

import com.rafambn.kmap.geometry.plane.TilePoint
import kotlin.test.Test
import kotlin.test.assertFailsWith

class CameraStateTest {
    @Test
    fun cameraStateRejectsNonFiniteZoom() {
        assertFailsWith<IllegalArgumentException> {
            CameraState(
                zoom = Float.NaN,
                tilePoint = TilePoint.Zero,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            CameraState(
                zoom = Float.POSITIVE_INFINITY,
                tilePoint = TilePoint.Zero,
            )
        }
    }
}
