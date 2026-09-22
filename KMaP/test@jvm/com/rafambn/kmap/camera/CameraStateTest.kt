package com.rafambn.kmap.camera

import com.rafambn.kmap.geometry.plane.TilePoint
import de.infix.testBalloon.framework.core.testSuite
import kotlin.test.assertFailsWith

val CameraStateTest by testSuite {
    test("cameraStateRejectsNonFiniteZoom") {
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
