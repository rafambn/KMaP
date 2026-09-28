package com.rafambn.kmap.style

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.toPixelMap
import de.infix.testBalloon.framework.core.testSuite
import kotlin.test.assertEquals
import kotlin.test.assertTrue

val SpriteAtlasTest by testSuite {
    test("sprite atlas keeps pixel ratio and prefixes image names") {
        val atlas = ImageBitmap(4, 2)
        Canvas(atlas).drawRect(Rect(0f, 0f, 2f, 2f), Paint().apply { color = Color.Red })
        Canvas(atlas).drawRect(Rect(2f, 0f, 4f, 2f), Paint().apply { color = Color.Blue })

        val sprites = decodeSpriteAtlas("""{
            "red": {"x": 0, "y": 0, "width": 2, "height": 2, "pixelRatio": 1},
            "blue": {"x": 2, "y": 0, "width": 2, "height": 2, "pixelRatio": 2, "sdf": true}
        }""", atlas, "misc:")

        assertEquals(setOf("misc:red", "misc:blue"), sprites.keys)
        assertEquals(2.0, sprites.getValue("misc:blue").pixelRatio)
        assertTrue(sprites.getValue("misc:blue").sdf)
        assertEquals(Color.Red, sprites.getValue("misc:red").bitmap.toPixelMap()[0, 0])
        assertEquals(Color.Blue, sprites.getValue("misc:blue").bitmap.toPixelMap()[0, 0])
    }
}
