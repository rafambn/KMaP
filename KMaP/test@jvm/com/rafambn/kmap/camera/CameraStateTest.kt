package com.rafambn.kmap.camera

import com.rafambn.kmap.geometry.plane.Coordinates
import com.rafambn.kmap.geometry.plane.TilePoint
import kotlin.test.Test
import kotlin.test.assertFailsWith

class CameraStateTest {
    @Test
    fun cameraStatesRejectNonFiniteZoom() {
        assertFailsWith<IllegalArgumentException> {
            CameraState(
                zoom = Float.NaN,
                coordinates = Coordinates.Zero,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            InternalCameraState(
                zoom = Float.POSITIVE_INFINITY,
                tilePoint = TilePoint.Zero,
            )
        }
    }
}
