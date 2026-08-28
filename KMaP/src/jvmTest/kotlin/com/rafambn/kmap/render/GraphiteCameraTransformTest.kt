package com.rafambn.kmap.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntSize
import kotlin.test.Test
import kotlin.test.assertEquals

class GraphiteCameraTransformTest {
    @Test
    fun panAndMagnifierAreAppliedWhenTheRecordingIsInserted() {
        val transform = GraphiteMapCamera(
            canvasSize = IntSize(800, 600),
            translation = Offset(400f, 300f),
            rotationDegrees = 0f,
            magnifierScale = 1f,
            positionOffset = Offset(-10.2f, 8.9f),
            tileSizePx = Size(256f, 256f),
            zoom = 4.0,
        ).frameTransform()

        assertEquals(2f, transform[0, 0])
        assertEquals(2f, transform[1, 1])
        assertEquals(378f, transform[3, 0])
        assertEquals(316f, transform[3, 1])
    }
}
