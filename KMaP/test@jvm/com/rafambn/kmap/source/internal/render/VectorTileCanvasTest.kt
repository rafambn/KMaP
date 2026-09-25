package com.rafambn.kmap.source.internal.render

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.rafambn.kmap.mvttile.OptimizedGeometry
import com.rafambn.kmap.mvttile.OptimizedRenderFeature
import com.rafambn.kmap.style.Style
import com.rafambn.kmap.style.StyleLayer
import com.rafambn.kmap.style.StyleResolver
import de.infix.testBalloon.framework.core.testSuite
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.assertEquals
import kotlin.test.assertTrue

val VectorTileCanvasTest by testSuite {
    test("fill opacity multiplies color alpha and fades the outline") {
        val path = Path().apply { addRect(Rect(4f, 4f, 28f, 28f)) }
        val geometry = OptimizedGeometry.Polygon(path)

        fun render(opacity: Double): ImageBitmap {
            val style = StyleResolver().resolve(
                Style(
                    version = 8,
                    sources = emptyMap(),
                    layers = listOf(
                        StyleLayer(
                            id = "fill",
                            type = "fill",
                            paint = mapOf(
                                "fill-color" to JsonPrimitive("rgba(255, 0, 0, 0.5)"),
                                "fill-opacity" to JsonPrimitive(opacity),
                                "fill-outline-color" to JsonPrimitive("rgba(0, 0, 255, 1)")
                            )
                        )
                    )
                )
            )
            val bitmap = ImageBitmap(32, 32)
            drawFillFeature(Canvas(bitmap), geometry, emptyMap(), style.layers.single(), 0.0, 1f, 1f, 1f)
            return bitmap
        }

        assertEquals(0.25f, render(0.5).toPixelMap()[16, 16].alpha, 0.02f)

        val transparent = render(0.0).toPixelMap()
        assertTrue((0 until 32).all { y -> (0 until 32).all { x -> transparent[x, y].alpha == 0f } })
    }

    test("default outline only covers the outer edge") {
        val path = Path().apply { addRect(Rect(8.25f, 8.25f, 24.25f, 24.25f)) }
        val geometry = OptimizedGeometry.Polygon(path)

        fun render(explicitOutline: Boolean, antialias: Boolean): ImageBitmap {
            val paint = mutableMapOf(
                "fill-color" to JsonPrimitive("rgba(255, 0, 0, 0.5)"),
                "fill-antialias" to JsonPrimitive(antialias)
            )
            if (explicitOutline) paint["fill-outline-color"] = JsonPrimitive("rgba(255, 0, 0, 0.5)")
            val style = StyleResolver().resolve(
                Style(
                    version = 8,
                    sources = emptyMap(),
                    layers = listOf(StyleLayer(id = "fill", type = "fill", paint = paint))
                )
            )
            val bitmap = ImageBitmap(32, 32)
            drawFillFeature(Canvas(bitmap), geometry, emptyMap(), style.layers.single(), 0.0, 1f, 1f, 1f)
            return bitmap
        }

        val noOutline = render(false, false).toPixelMap()
        val defaultOutline = render(false, true).toPixelMap()
        val explicitOutline = render(true, true).toPixelMap()

        assertEquals(0.5f, defaultOutline[16, 16].alpha, 0.02f)
        assertEquals(noOutline[8, 16].alpha, defaultOutline[8, 16].alpha, 0.02f)
        assertTrue(defaultOutline[7, 16].alpha > noOutline[7, 16].alpha)
        assertTrue(defaultOutline[8, 16].alpha < explicitOutline[8, 16].alpha)
    }

    test("fill outline stays one screen pixel across tile and map scales") {
        val style = StyleResolver().resolve(
            Style(
                version = 8,
                sources = emptyMap(),
                layers = listOf(
                    StyleLayer(
                        id = "fill",
                        type = "fill",
                        paint = mapOf(
                            "fill-color" to JsonPrimitive("rgba(255, 0, 0, 0)"),
                            "fill-outline-color" to JsonPrimitive("#0000ff")
                        )
                    )
                )
            )
        )
        fun render(tileScaleX: Float, tileScaleY: Float, screenScale: Float): ImageBitmap {
            val path = Path().apply {
                addRect(Rect(32f / (tileScaleX * screenScale), 32f / (tileScaleY * screenScale),
                    96f / (tileScaleX * screenScale), 96f / (tileScaleY * screenScale)))
            }
            val bitmap = ImageBitmap(128, 128)
            val canvas = Canvas(bitmap)
            canvas.scale(screenScale, screenScale)
            canvas.scale(tileScaleX, tileScaleY)
            drawFillFeature(
                canvas, OptimizedGeometry.Polygon(path), emptyMap(), style.layers.single(),
                0.0, tileScaleX, tileScaleY, screenScale
            )
            return bitmap
        }

        val baseline = render(1f, 1f, 1f).toPixelMap()
        val tileScaled = render(4f, 2f, 1f).toPixelMap()
        val mapScaled = render(4f, 2f, 2f).toPixelMap()
        for (x in 30..34) {
            assertEquals(baseline[x, 64].alpha, tileScaled[x, 64].alpha, 0.02f)
            assertEquals(baseline[x, 64].alpha, mapScaled[x, 64].alpha, 0.02f)
        }
        for (y in 30..34) {
            assertEquals(baseline[64, y].alpha, tileScaled[64, y].alpha, 0.02f)
            assertEquals(baseline[64, y].alpha, mapScaled[64, y].alpha, 0.02f)
        }
    }

    test("line width uses screen pixels across tile and map scales") {
        val style = StyleResolver().resolve(
            Style(
                version = 8,
                sources = emptyMap(),
                layers = listOf(
                    StyleLayer(
                        id = "outline",
                        type = "line",
                        paint = mapOf(
                            "line-color" to JsonPrimitive("#0000ff"),
                            "line-width" to JsonPrimitive(6)
                        )
                    )
                )
            )
        )

        fun render(tileScaleX: Float, tileScaleY: Float, screenScale: Float): ImageBitmap {
            val path = Path().apply {
                addRect(Rect(
                    32f / (tileScaleX * screenScale), 32f / (tileScaleY * screenScale),
                    96f / (tileScaleX * screenScale), 96f / (tileScaleY * screenScale)
                ))
            }
            val bitmap = ImageBitmap(128, 128)
            val canvas = Canvas(bitmap)
            canvas.scale(screenScale, screenScale)
            canvas.scale(tileScaleX, tileScaleY)
            drawLineFeature(canvas, path, emptyMap(), style.layers.single(), 0.0, tileScaleX, tileScaleY, screenScale)
            return bitmap
        }

        val baseline = render(1f, 1f, 1f).toPixelMap()
        val tileScaled = render(4f, 2f, 1f).toPixelMap()
        val mapScaled = render(4f, 2f, 2f).toPixelMap()
        assertEquals(0f, baseline[64, 64].alpha)
        assertTrue(baseline[32, 64].alpha > 0.9f)
        assertTrue(baseline[64, 32].alpha > 0.9f)
        for (x in 27..37) {
            assertEquals(baseline[x, 64].alpha, tileScaled[x, 64].alpha, 0.02f)
            assertEquals(baseline[x, 64].alpha, mapScaled[x, 64].alpha, 0.02f)
        }
        for (y in 27..37) {
            assertEquals(baseline[64, y].alpha, tileScaled[64, y].alpha, 0.02f)
            assertEquals(baseline[64, y].alpha, mapScaled[64, y].alpha, 0.02f)
        }
    }

    test("polygon line outlines outer and inner rings without filling them") {
        val style = StyleResolver().resolve(
            Style(
                version = 8,
                sources = emptyMap(),
                layers = listOf(
                    StyleLayer(
                        id = "outline",
                        type = "line",
                        paint = mapOf(
                            "line-color" to JsonPrimitive("rgba(255, 0, 0, 0.5)"),
                            "line-opacity" to JsonPrimitive(0.5),
                            "line-width" to JsonPrimitive(4)
                        )
                    )
                )
            )
        )
        val path = Path().apply {
            addRect(Rect(8f, 8f, 56f, 56f))
            addRect(Rect(24f, 24f, 40f, 40f))
        }
        val bitmap = ImageBitmap(64, 64)
        val canvas = Canvas(bitmap)
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, Size(64f, 64f)) {
            drawRenderFeature(
                canvas,
                OptimizedRenderFeature(OptimizedGeometry.Polygon(path), emptyMap()),
                createFontFamilyResolver(),
                Density(1f),
                style.layers.single(),
                0.0,
                1f,
                0f,
                1f,
                1f,
                1f,
            )
        }
        val pixels = bitmap.toPixelMap()

        assertEquals(0.25f, pixels[8, 32].alpha, 0.02f)
        assertEquals(0.25f, pixels[24, 32].alpha, 0.02f)
        assertEquals(0f, pixels[16, 16].alpha)
        assertEquals(0f, pixels[32, 32].alpha)
    }
}
