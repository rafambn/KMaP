package com.rafambn.kmap.source.internal.render

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.assertEquals
import kotlin.test.assertTrue

val VectorTileCanvasTest by testSuite {
    test("plain symbol text uses text-color without format") {
        val style = StyleResolver().resolve(
            Style(
                version = 8,
                sources = emptyMap(),
                layers = listOf(StyleLayer(
                    id = "label",
                    type = "symbol",
                    layout = mapOf(
                        "text-field" to JsonPrimitive("A"),
                        "text-size" to JsonPrimitive(24)
                    ),
                    paint = mapOf("text-color" to JsonPrimitive("#0000ff"))
                ))
            )
        )
        val bitmap = ImageBitmap(128, 64)
        val canvas = Canvas(bitmap)
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, Size(128f, 64f)) {
            drawRenderFeature(
                canvas,
                OptimizedRenderFeature(OptimizedGeometry.Point(listOf(64f to 32f)), emptyMap()),
                createFontFamilyResolver(), Density(1f), style.layers.single(),
                0.0, 1f, 0f, 1f, 1f, 1f
            )
        }
        val pixels = bitmap.toPixelMap()
        assertTrue((0 until 128).any { x -> (0 until 64).any { y ->
            pixels[x, y].let { it.alpha > 0.5f && it.blue > 0.9f && it.red < 0.1f }
        } })
    }

    test("symbol image expression draws a supplied sprite") {
        val sprite = ImageBitmap(4, 4)
        Canvas(sprite).drawRect(Rect(0f, 0f, 4f, 4f), Paint().apply { color = Color.Red })
        val style = StyleResolver().resolve(
            Style(
                version = 8,
                sources = emptyMap(),
                layers = listOf(StyleLayer(
                    id = "icon",
                    type = "symbol",
                    layout = mapOf("icon-image" to JsonArray(listOf(JsonPrimitive("image"), JsonPrimitive("dot")))),
                    paint = mapOf("icon-opacity" to JsonPrimitive(0.5))
                ))
            ),
            sprites = mapOf("dot" to sprite)
        )
        val bitmap = ImageBitmap(32, 32)
        val canvas = Canvas(bitmap)
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, Size(32f, 32f)) {
            drawRenderFeature(
                canvas,
                OptimizedRenderFeature(OptimizedGeometry.Point(listOf(16f to 16f)), emptyMap()),
                createFontFamilyResolver(), Density(1f), style.layers.single(),
                0.0, 1f, 0f, 1f, 1f, 1f
            )
        }
        val pixels = bitmap.toPixelMap()
        assertEquals(0.5f, pixels[16, 16].alpha, 0.02f)
        assertEquals(0f, pixels[10, 10].alpha)
    }

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

    test("line join none leaves corners unjoined and ignores line cap") {
        val path = Path().apply {
            moveTo(8f, 24f)
            lineTo(24f, 24f)
            lineTo(24f, 8f)
        }

        fun render(join: String, cap: String): ImageBitmap {
            val style = StyleResolver().resolve(
                Style(
                    version = 8,
                    sources = emptyMap(),
                    layers = listOf(
                        StyleLayer(
                            id = "line",
                            type = "line",
                            layout = mapOf(
                                "line-join" to JsonPrimitive(join),
                                "line-cap" to JsonPrimitive(cap)
                            ),
                            paint = mapOf(
                                "line-color" to JsonPrimitive("#0000ff"),
                                "line-width" to JsonPrimitive(8)
                            )
                        )
                    )
                )
            )
            val bitmap = ImageBitmap(32, 32)
            drawLineFeature(Canvas(bitmap), path, emptyMap(), style.layers.single(), 0.0, 1f, 1f, 1f)
            return bitmap
        }

        val miter = render("miter", "butt").toPixelMap()
        val none = render("none", "butt").toPixelMap()
        val noneSquare = render("none", "square").toPixelMap()

        assertTrue(miter[26, 26].alpha > 0.9f)
        assertEquals(0f, none[26, 26].alpha)
        assertEquals(0f, noneSquare[26, 26].alpha)
        assertEquals(0f, noneSquare[5, 24].alpha)
        assertTrue(none[16, 24].alpha > 0.9f)
    }

    test("line join none closes polygon rings") {
        val style = StyleResolver().resolve(
            Style(
                version = 8,
                sources = emptyMap(),
                layers = listOf(
                    StyleLayer(
                        id = "line",
                        type = "line",
                        layout = mapOf("line-join" to JsonPrimitive("none")),
                        paint = mapOf(
                            "line-color" to JsonPrimitive("#0000ff"),
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
        drawLineFeature(Canvas(bitmap), path, emptyMap(), style.layers.single(), 0.0, 1f, 1f, 1f)
        val pixels = bitmap.toPixelMap()

        assertTrue(pixels[8, 32].alpha > 0.9f)
        assertTrue(pixels[24, 32].alpha > 0.9f)
        assertEquals(0f, pixels[16, 16].alpha)
        assertEquals(0f, pixels[32, 32].alpha)
    }

    test("line dasharray uses line widths and stays in screen pixels") {
        val style = StyleResolver().resolve(
            Style(
                version = 8,
                sources = emptyMap(),
                layers = listOf(
                    StyleLayer(
                        id = "line",
                        type = "line",
                        paint = mapOf(
                            "line-color" to JsonPrimitive("#0000ff"),
                            "line-width" to JsonPrimitive(4),
                            "line-dasharray" to JsonArray(listOf(JsonPrimitive(2), JsonPrimitive(1)))
                        )
                    )
                )
            )
        )

        fun render(tileScaleX: Float, tileScaleY: Float, screenScale: Float): ImageBitmap {
            val path = Path().apply {
                moveTo(8f / (tileScaleX * screenScale), 16f / (tileScaleY * screenScale))
                lineTo(72f / (tileScaleX * screenScale), 16f / (tileScaleY * screenScale))
            }
            val bitmap = ImageBitmap(80, 32)
            val canvas = Canvas(bitmap)
            canvas.scale(screenScale, screenScale)
            canvas.scale(tileScaleX, tileScaleY)
            drawLineFeature(canvas, path, emptyMap(), style.layers.single(), 0.0, tileScaleX, tileScaleY, screenScale)
            return bitmap
        }

        val baseline = render(1f, 1f, 1f).toPixelMap()
        val tileScaled = render(4f, 2f, 1f).toPixelMap()
        val mapScaled = render(4f, 2f, 2f).toPixelMap()
        assertTrue(baseline[12, 16].alpha > 0.9f)
        assertEquals(0f, baseline[18, 16].alpha)
        assertTrue(baseline[24, 16].alpha > 0.9f)
        assertEquals(0f, baseline[30, 16].alpha)
        for (x in 8..70) {
            assertEquals(baseline[x, 16].alpha, tileScaled[x, 16].alpha, 0.02f)
            assertEquals(baseline[x, 16].alpha, mapScaled[x, 16].alpha, 0.02f)
        }
    }

    test("line dasharray joins odd endpoints and is disabled by line pattern") {
        fun render(pattern: Boolean): ImageBitmap {
            val paint = mutableMapOf(
                "line-color" to JsonPrimitive("#0000ff"),
                "line-width" to JsonPrimitive(4),
                "line-dasharray" to JsonArray(listOf(JsonPrimitive(2), JsonPrimitive(1), JsonPrimitive(3)))
            )
            if (pattern) paint["line-pattern"] = JsonPrimitive("road-texture")
            val style = StyleResolver().resolve(
                Style(
                    version = 8,
                    sources = emptyMap(),
                    layers = listOf(StyleLayer(id = "line", type = "line", paint = paint))
                )
            )
            val path = Path().apply {
                moveTo(8f, 16f)
                lineTo(72f, 16f)
            }
            val bitmap = ImageBitmap(80, 32)
            drawLineFeature(Canvas(bitmap), path, emptyMap(), style.layers.single(), 0.0, 1f, 1f, 1f)
            return bitmap
        }

        val dashed = render(false).toPixelMap()
        val patterned = render(true).toPixelMap()
        assertTrue(dashed[12, 16].alpha > 0.9f)
        assertEquals(0f, dashed[18, 16].alpha)
        assertTrue(dashed[24, 16].alpha > 0.9f)
        assertTrue(dashed[36, 16].alpha > 0.9f)
        assertEquals(0f, dashed[42, 16].alpha)
        assertTrue(patterned[18, 16].alpha > 0.9f)
    }

    test("zero-length dashes render dots only with round caps") {
        fun render(cap: String, dashArray: List<Int> = listOf(0, 2)): ImageBitmap {
            val style = StyleResolver().resolve(
                Style(
                    version = 8,
                    sources = emptyMap(),
                    layers = listOf(
                        StyleLayer(
                            id = "line",
                            type = "line",
                            layout = mapOf("line-cap" to JsonPrimitive(cap)),
                            paint = mapOf(
                                "line-color" to JsonPrimitive("#0000ff"),
                                "line-width" to JsonPrimitive(4),
                                "line-dasharray" to JsonArray(dashArray.map(::JsonPrimitive))
                            )
                        )
                    )
                )
            )
            val path = Path().apply {
                moveTo(8f, 16f)
                lineTo(72f, 16f)
            }
            val bitmap = ImageBitmap(80, 32)
            drawLineFeature(Canvas(bitmap), path, emptyMap(), style.layers.single(), 0.0, 1f, 1f, 1f)
            return bitmap
        }

        val round = render("round").toPixelMap()
        val square = render("square").toPixelMap()
        val mixed = render("square", listOf(2, 1, 0, 1)).toPixelMap()
        val leadingGap = render("square", listOf(0, 1, 2, 1)).toPixelMap()
        assertTrue(round[8, 16].alpha > 0.9f)
        assertEquals(0f, round[12, 16].alpha)
        assertTrue(round[16, 16].alpha > 0.9f)
        assertEquals(0f, square[8, 16].alpha)
        assertEquals(0f, square[16, 16].alpha)
        assertTrue(mixed[12, 16].alpha > 0.9f)
        assertEquals(0f, mixed[20, 16].alpha)
        assertTrue(mixed[28, 16].alpha > 0.9f)
        assertEquals(0f, leadingGap[8, 16].alpha)
        assertTrue(leadingGap[12, 16].alpha > 0.9f)
    }

    test("all-zero dashes draw nothing and zero gaps draw a solid line") {
        fun render(intervals: List<Int>): ImageBitmap {
            val style = StyleResolver().resolve(
                Style(
                    version = 8,
                    sources = emptyMap(),
                    layers = listOf(
                        StyleLayer(
                            id = "line",
                            type = "line",
                            paint = mapOf(
                                "line-color" to JsonPrimitive("#0000ff"),
                                "line-width" to JsonPrimitive(4),
                                "line-dasharray" to JsonArray(intervals.map(::JsonPrimitive))
                            )
                        )
                    )
                )
            )
            val path = Path().apply {
                moveTo(8f, 16f)
                lineTo(72f, 16f)
            }
            val bitmap = ImageBitmap(80, 32)
            drawLineFeature(Canvas(bitmap), path, emptyMap(), style.layers.single(), 0.0, 1f, 1f, 1f)
            return bitmap
        }

        assertEquals(0f, render(listOf(0, 0)).toPixelMap()[32, 16].alpha)
        assertTrue(render(listOf(2, 0)).toPixelMap()[32, 16].alpha > 0.9f)
    }
}
