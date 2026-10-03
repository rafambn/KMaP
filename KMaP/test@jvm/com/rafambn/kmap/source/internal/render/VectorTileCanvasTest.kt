package com.rafambn.kmap.source.internal.render

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.rafambn.kmap.geometry.plane.CanvasDrawReference
import com.rafambn.kmap.mapProperties.TileDimension
import com.rafambn.kmap.mvttile.MVTFeature
import com.rafambn.kmap.mvttile.MVTLayer
import com.rafambn.kmap.mvttile.MVTile
import com.rafambn.kmap.mvttile.OptimizedGeometry
import com.rafambn.kmap.mvttile.OptimizedRenderFeature
import com.rafambn.kmap.mvttile.RawMVTGeomType
import com.rafambn.kmap.source.VectorTile
import com.rafambn.kmap.source.internal.ActiveTiles
import com.rafambn.kmap.source.internal.optimizeMVTile
import com.rafambn.kmap.style.StyleResolver
import com.rafambn.kmap.style.SpriteImage
import com.rafambn.kmap.style.compiled.CompiledFillLayer
import com.rafambn.kmap.style.compiled.CompiledBackgroundLayer
import com.rafambn.kmap.style.compiled.CompiledLineLayer
import com.rafambn.kmap.style.model.Style
import com.rafambn.kmap.style.model.StyleLayer
import de.infix.testBalloon.framework.core.testSuite
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlin.math.hypot
import kotlin.math.pow

private fun sdfCircleSprite(): ImageBitmap = ImageBitmap(32, 32).also { image ->
    val canvas = Canvas(image)
    val paint = Paint()
    for (y in 0 until 32) for (x in 0 until 32) {
        val distance = hypot(x - 15.5f, y - 15.5f)
        paint.color = Color.White.copy(alpha = (0.75f + (8.5f - distance) / 8f).coerceIn(0f, 1f))
        canvas.drawRect(Rect(x.toFloat(), y.toFloat(), x + 1f, y + 1f), paint)
    }
}

val VectorTileCanvasTest by testSuite {
    test("fill paint reads each MVT feature ID and polygon geometry type") {
        val style = StyleResolver().resolve("""{
            "layers": [{"id": "parks", "type": "fill", "source-layer": "landuse",
                "paint": {"fill-color": ["case",
                    ["all", ["==", ["id"], 17], ["==", ["geometry-type"], "Polygon"]],
                    "#ff0000", "#00ff00"]}}
            ]
        }""").style!!
        val features = listOf(
            MVTFeature(17UL, RawMVTGeomType.POLYGON, listOf(listOf(2 to 2, 14 to 2, 14 to 14, 2 to 14)), emptyMap()),
            MVTFeature(18UL, RawMVTGeomType.POLYGON, listOf(listOf(18 to 2, 30 to 2, 30 to 14, 18 to 14)), emptyMap())
        )
        val tile = VectorTile(0, 0, 0, MVTile(listOf(MVTLayer("landuse", 32, features))))
        val renderFeatures = optimizeMVTile(tile, style).optimizedTile!!.layerFeatures.getValue("parks")
        val bitmap = ImageBitmap(32, 16)
        val canvas = Canvas(bitmap)

        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, Size(32f, 16f)) {
            renderFeatures.forEach { feature ->
                drawRenderFeature(
                    canvas, feature, createFontFamilyResolver(), Density(1f), style.layers.single(),
                    0.0, 1f, 0f, 1f, 1f, 1f
                )
            }
        }

        val pixels = bitmap.toPixelMap()
        assertTrue(pixels[8, 8].red > 0.9f && pixels[8, 8].green < 0.1f)
        assertTrue(pixels[24, 8].green > 0.9f && pixels[24, 8].red < 0.1f)
    }

    test("plain symbol text uses text-color without format") {
        val style = StyleResolver().resolve(
            Json.encodeToString(Style.serializer(), Style(
                version = 8,
                sources = emptyMap(),
                layers = listOf(StyleLayer(
                    id = "label",
                    type = "symbol",
                    sourceLayer = "places",
                    layout = mapOf(
                        "text-field" to JsonPrimitive("A"),
                        "text-size" to JsonPrimitive(24)
                    ),
                    paint = mapOf("text-color" to JsonPrimitive("#0000ff"))
                ))
            ))
        ).style!!
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

    test("symbol layout uses integer zoom and text paint uses fractional zoom") {
        val style = StyleResolver().resolve("""{
            "layers": [{"id": "label", "type": "symbol", "source-layer": "places",
                "layout": {
                    "text-field": ["step", ["zoom"], "", 4, "A", 4.3, ""],
                    "text-size": ["step", ["zoom"], 1, 4, 30]
                },
                "paint": {
                    "text-color": ["interpolate", ["linear"], ["zoom"], 4, "#ff0000", 5, "#0000ff"]
                }}]
        }""").style!!
        val bitmap = ImageBitmap(128, 64)
        val canvas = Canvas(bitmap)

        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, Size(128f, 64f)) {
            drawRenderFeature(
                canvas,
                OptimizedRenderFeature(OptimizedGeometry.Point(listOf(64f to 32f)), emptyMap()),
                createFontFamilyResolver(), Density(1f), style.layers.single(),
                4.5, 1f, 0f, 1f, 1f, 1f
            )
        }

        val pixels = bitmap.toPixelMap()
        assertTrue((0 until 128).any { x -> (0 until 64).any { y ->
            pixels[x, y].let { it.alpha > 0.5f && it.red > 0.3f && it.blue > 0.3f && it.green < 0.1f }
        } })
    }

    test("text opacity fades the halo") {
        val style = StyleResolver().resolve(
            Json.encodeToString(Style.serializer(), Style(
                version = 8,
                sources = emptyMap(),
                layers = listOf(StyleLayer(
                    id = "label",
                    type = "symbol",
                    sourceLayer = "places",
                    layout = mapOf("text-field" to JsonPrimitive("A"), "text-size" to JsonPrimitive(24)),
                    paint = mapOf(
                        "text-color" to JsonPrimitive("transparent"),
                        "text-opacity" to JsonPrimitive(0.25),
                        "text-halo-color" to JsonPrimitive("#ffffff"),
                        "text-halo-width" to JsonPrimitive(2)
                    )
                ))
            ))
        ).style!!
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
        val haloAlpha = (0 until 128).maxOf { x -> (0 until 64).maxOf { y -> pixels[x, y].alpha } }
        assertTrue(haloAlpha in 0.2f..0.55f, "halo alpha was $haloAlpha")
    }

    test("text fill keeps its color after painting a white halo") {
        val style = StyleResolver().resolve("""{
            "layers": [{"id": "continents", "type": "symbol", "source-layer": "places",
                "layout": {"text-field": "A", "text-size": 48},
                "paint": {"text-color": "hsl(0,0%,19%)", "text-halo-color": "hsl(0,0%,100%)",
                    "text-halo-width": 1, "text-halo-blur": 1}}]
        }""").style!!
        val bitmap = ImageBitmap(128, 96)
        val canvas = Canvas(bitmap)

        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, Size(128f, 96f)) {
            drawRenderFeature(
                canvas,
                OptimizedRenderFeature(OptimizedGeometry.Point(listOf(64f to 48f)), emptyMap()),
                createFontFamilyResolver(), Density(1f), style.layers.single(),
                0.0, 1f, 0f, 1f, 1f, 1f
            )
        }

        val pixels = bitmap.toPixelMap()
        assertTrue((0 until 128).any { x -> (0 until 96).any { y ->
            pixels[x, y].let { it.alpha > 0.95f && it.red in 0.15f..0.25f && it.green in 0.15f..0.25f }
        } })
    }

    test("text halo width stays in screen pixels across tile and map scales") {
        fun render(tileScale: Float, mapScale: Float, displayDensity: Float, haloWidth: Int): ImageBitmap {
            val style = StyleResolver().resolve("""{
                "layers": [{"id": "label", "type": "symbol", "source-layer": "places",
                    "layout": {"text-field": "H", "text-size": 36},
                    "paint": {"text-color": "#000000", "text-halo-color": "#ffffff",
                        "text-halo-width": $haloWidth}}]
            }""").style!!
            val bitmap = ImageBitmap(320, 320)
            val canvas = Canvas(bitmap)
            canvas.translate(160f, 160f)
            canvas.scale(mapScale, mapScale)
            canvas.scale(tileScale, tileScale)
            val density = Density(displayDensity)
            CanvasDrawScope().draw(density, LayoutDirection.Ltr, canvas, Size(320f, 320f)) {
                drawRenderFeature(
                    canvas,
                    OptimizedRenderFeature(OptimizedGeometry.Point(listOf(0f to 0f)), emptyMap()),
                    createFontFamilyResolver(), density, style.layers.single(),
                    0.0, 1f / tileScale, 0f, tileScale, tileScale, mapScale
                )
            }
            return bitmap
        }

        fun leftMargin(tileScale: Float, mapScale: Float, displayDensity: Float): Int {
            fun leftEdge(bitmap: ImageBitmap): Int {
                val pixels = bitmap.toPixelMap()
                return (0 until 320).first { x -> (0 until 320).any { y -> pixels[x, y].alpha > 0.5f } }
            }
            return leftEdge(render(tileScale, mapScale, displayDensity, 0)) -
                leftEdge(render(tileScale, mapScale, displayDensity, 2))
        }

        val baseline = leftMargin(1f, 1f, 1f)
        assertTrue(baseline in 1..3, "baseline halo margin was $baseline px")
        assertTrue(kotlin.math.abs(leftMargin(0.125f, 1f, 1f) - baseline) <= 1)
        assertTrue(kotlin.math.abs(leftMargin(0.125f, 1.5f, 1f) - baseline) <= 1)
        assertTrue(kotlin.math.abs(leftMargin(0.125f, 1f, 2f) - baseline * 2) <= 1)
    }

    test("text size follows fractional zoom without jumping at tile zoom boundaries") {
        fun textWidth(zoom: Double, textSize: String): Int {
            val style = StyleResolver().resolve("""{
                "layers": [{"id": "label", "type": "symbol", "source-layer": "places",
                    "layout": {"text-field": "H", "text-size": $textSize},
                    "paint": {"text-color": "#000000"}}]
            }""").style!!
            val bitmap = ImageBitmap(320, 320)
            val canvas = Canvas(bitmap)
            val tileScale = 0.125f
            val mapScale = 2.0.pow(zoom - zoom.toInt()).toFloat()
            canvas.translate(160f, 160f)
            canvas.scale(mapScale, mapScale)
            canvas.scale(tileScale, tileScale)
            CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, Size(320f, 320f)) {
                drawRenderFeature(
                    canvas,
                    OptimizedRenderFeature(OptimizedGeometry.Point(listOf(0f to 0f)), emptyMap()),
                    createFontFamilyResolver(), Density(1f), style.layers.single(),
                    zoom, 1f / tileScale, 0f, tileScale, tileScale, mapScale
                )
            }
            val pixels = bitmap.toPixelMap()
            val columns = (0 until 320).filter { x -> (0 until 320).any { y -> pixels[x, y].alpha > 0.5f } }
            return columns.last() - columns.first() + 1
        }

        val constant = listOf(0.0, 0.5, 0.99, 1.0, 1.5, 1.99, 2.0)
            .map { textWidth(it, "30") }
        assertTrue(constant.max() - constant.min() <= 2, "fixed text size varied: $constant")

        val expression = """["interpolate", ["linear"], ["zoom"], 0, 20, 2, 40]"""
        val widths = listOf(0.0, 0.5, 1.0, 1.5, 1.99, 2.0)
            .map { textWidth(it, expression) }
        assertTrue(widths.zipWithNext().all { (before, after) -> after >= before }, "text did not grow smoothly: $widths")
        assertTrue(widths[2] > widths[0] && widths[2] < widths[5], "zoom 1 was not interpolated: $widths")
        assertTrue(widths[5] - widths[4] <= 2, "text jumped at zoom 2: $widths")
    }

    test("text max width uses font ems at display density") {
        fun lineWidths(text: String, density: Float): List<Int> {
            val style = StyleResolver().resolve("""{
                "layers": [{"id": "ocean", "type": "symbol", "source-layer": "places",
                    "layout": {"text-field": "$text", "text-size": 20, "text-max-width": 6},
                    "paint": {"text-color": "#000000"}}]
            }""").style!!
            val bitmap = ImageBitmap(320, 320)
            val canvas = Canvas(bitmap)
            val tileScale = 0.125f
            canvas.translate(160f, 160f)
            canvas.scale(tileScale, tileScale)
            val displayDensity = Density(density)
            CanvasDrawScope().draw(displayDensity, LayoutDirection.Ltr, canvas, Size(320f, 320f)) {
                drawRenderFeature(
                    canvas,
                    OptimizedRenderFeature(OptimizedGeometry.Point(listOf(0f to 0f)), emptyMap()),
                    createFontFamilyResolver(), displayDensity, style.layers.single(),
                    2.0, 1f / tileScale, 0f, tileScale, tileScale, 1f
                )
            }
            val pixels = bitmap.toPixelMap()
            val rows = (0 until 320).filter { y -> (0 until 320).any { x -> pixels[x, y].alpha > 0.5f } }
            val lines = rows.fold(mutableListOf(mutableListOf<Int>())) { groups, row ->
                if (groups.last().isNotEmpty() && row > groups.last().last() + 1) groups.add(mutableListOf())
                groups.last().add(row)
                groups
            }
            return lines.map { line ->
                val columns = (0 until 320).filter { x -> line.any { y -> pixels[x, y].alpha > 0.5f } }
                columns.last() - columns.first() + 1
            }
        }

        for (density in listOf(1f, 2f)) {
            val atlanticWidth = lineWidths("Atlantic", density).single()
            val oceanWidth = lineWidths("Ocean", density).single()
            val wrapped = lineWidths("Atlantic Ocean", density)
            assertEquals(2, wrapped.size, "expected two lines at density $density: $wrapped")
            assertTrue(kotlin.math.abs(wrapped[0] - atlanticWidth) <= 2,
                "Atlantic was split at density $density: $wrapped vs $atlanticWidth")
            assertTrue(kotlin.math.abs(wrapped[1] - oceanWidth) <= 2,
                "Ocean was split at density $density: $wrapped vs $oceanWidth")
        }
    }

    test("symbol image expression draws a supplied sprite") {
        val sprite = ImageBitmap(4, 4)
        Canvas(sprite).drawRect(Rect(0f, 0f, 4f, 4f), Paint().apply { color = Color.Red })
        val style = StyleResolver().resolve(
            Json.encodeToString(Style.serializer(), Style(
                version = 8,
                sources = emptyMap(),
                layers = listOf(StyleLayer(
                    id = "icon",
                    type = "symbol",
                    sourceLayer = "places",
                    layout = mapOf("icon-image" to JsonArray(listOf(JsonPrimitive("image"), JsonPrimitive("dot")))),
                    paint = mapOf(
                        "icon-opacity" to JsonPrimitive(0.5),
                        "icon-color" to JsonPrimitive("#0000ff"),
                        "icon-halo-color" to JsonPrimitive("#0000ff"),
                        "icon-halo-width" to JsonPrimitive(4),
                        "icon-halo-blur" to JsonPrimitive(2)
                    )
                ))
            )),
            sprites = mapOf("dot" to SpriteImage(sprite))
        ).style!!
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
        assertTrue(pixels[16, 16].red > 0.9f && pixels[16, 16].blue < 0.1f)
        assertEquals(0f, pixels[19, 16].alpha)
        assertEquals(0f, pixels[10, 10].alpha)
    }

    test("sprite pixel ratio controls icon size") {
        val sprite = ImageBitmap(4, 4)
        Canvas(sprite).drawRect(Rect(0f, 0f, 4f, 4f), Paint().apply { color = Color.Red })
        val style = StyleResolver().resolve("""{
            "layers": [{"id": "icons", "type": "symbol", "source-layer": "places",
                "layout": {"icon-image": "dot"}}]
        }""", sprites = mapOf("dot" to SpriteImage(sprite, pixelRatio = 2.0))).style!!
        val bitmap = ImageBitmap(32, 32)
        val canvas = Canvas(bitmap)
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, Size(32f, 32f)) {
            drawRenderFeature(canvas, OptimizedRenderFeature(OptimizedGeometry.Point(listOf(16f to 16f)), emptyMap()),
                createFontFamilyResolver(), Density(1f), style.layers.single(), 0.0, 1f, 0f, 1f, 1f, 1f)
        }
        val pixels = bitmap.toPixelMap()
        assertTrue(pixels[16, 16].red > 0.9f)
        assertEquals(0f, pixels[18, 16].alpha)
    }

    test("sprite size and offset use display density across tile and map scales") {
        val sprite = ImageBitmap(20, 20)
        Canvas(sprite).drawRect(Rect(0f, 0f, 20f, 20f), Paint().apply { color = Color.Red })

        for (pixelRatio in listOf(1.0, 2.0)) {
            val layer = StyleResolver().resolve("""{
                "layers": [{"id": "icons", "type": "symbol", "source-layer": "places",
                    "layout": {"icon-image": "dot", "icon-anchor": "top-left", "icon-offset": [3, -2]}}]
            }""", sprites = mapOf("dot" to SpriteImage(sprite, pixelRatio))).style!!.layers.single()

            for ((tileScale, mapScale, displayDensity) in listOf(
                Triple(1f, 1f, 1f),
                Triple(1f, 1f, 2f),
                Triple(0.125f, 1f, 2f),
                Triple(0.125f, 1.5f, 2f)
            )) {
                val bitmap = ImageBitmap(200, 200)
                val canvas = Canvas(bitmap)
                canvas.translate(80f, 80f)
                canvas.scale(mapScale * tileScale, mapScale * tileScale)
                val density = Density(displayDensity)
                CanvasDrawScope().draw(density, LayoutDirection.Ltr, canvas, Size(200f, 200f)) {
                    drawRenderFeature(canvas, OptimizedRenderFeature(OptimizedGeometry.Point(listOf(0f to 0f)), emptyMap()),
                        createFontFamilyResolver(), density, layer, 0.0, 1f / tileScale, 0f, tileScale, tileScale, mapScale)
                }
                val pixels = bitmap.toPixelMap()
                val columns = (0 until 200).filter { x -> (0 until 200).any { y -> pixels[x, y].alpha > 0.5f } }
                val rows = (0 until 200).filter { y -> (0 until 200).any { x -> pixels[x, y].alpha > 0.5f } }
                val expectedSize = 20 / pixelRatio * displayDensity
                val context = "density=$displayDensity, pixelRatio=$pixelRatio, tileScale=$tileScale, mapScale=$mapScale"
                assertTrue(kotlin.math.abs(columns.size - expectedSize) <= 1, "icon width: $context")
                assertTrue(kotlin.math.abs(rows.size - expectedSize) <= 1, "icon height: $context")
                assertTrue(kotlin.math.abs(columns.first() - (80 + 3 * displayDensity)) <= 1, "icon x offset: $context")
                assertTrue(kotlin.math.abs(rows.first() - (80 - 2 * displayDensity)) <= 1, "icon y offset: $context")
            }
        }
    }

    test("SDF sprite renders with its default black color") {
        val sprite = ImageBitmap(2, 2)
        Canvas(sprite).drawRect(Rect(0f, 0f, 2f, 2f), Paint().apply { color = Color.White.copy(alpha = 0.5f) })
        Canvas(sprite).drawRect(Rect(0f, 0f, 1f, 2f), Paint().apply { color = Color.White })
        val layer = StyleResolver().resolve("""{
            "layers": [{"id": "icons", "type": "symbol", "source-layer": "places",
                "layout": {"icon-image": "dot"}}]
        }""", sprites = mapOf("dot" to SpriteImage(sprite, sdf = true))).style!!.layers.single()
        val bitmap = ImageBitmap(8, 8)
        val canvas = Canvas(bitmap)
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, Size(8f, 8f)) {
            drawRenderFeature(canvas, OptimizedRenderFeature(OptimizedGeometry.Point(listOf(4f to 4f)), emptyMap()),
                createFontFamilyResolver(), Density(1f), layer, 0.0, 1f, 0f, 1f, 1f, 1f)
        }
        val pixels = bitmap.toPixelMap()
        val pixel = pixels[3, 4]
        assertTrue(pixel.alpha > 0.9f && pixel.red < 0.1f && pixel.green < 0.1f && pixel.blue < 0.1f)
        assertEquals(0f, pixels[4, 4].alpha, 0.02f)
    }

    test("SDF icon color evaluates per feature and multiplies icon opacity") {
        val sprite = ImageBitmap(2, 2)
        Canvas(sprite).drawRect(Rect(0f, 0f, 2f, 2f), Paint().apply { color = Color.White })
        val resolution = StyleResolver().resolve("""{
            "layers": [{"id": "icons", "type": "symbol", "source-layer": "places",
                "layout": {"icon-image": "dot"},
                "paint": {
                    "icon-color": ["case", ["==", ["get", "kind"], "blue"], "rgba(0, 0, 255, 0.5)", "#00ff00"],
                    "icon-opacity": 0.5
                }}]
        }""", sprites = mapOf("dot" to SpriteImage(sprite, sdf = true)))
        assertTrue(resolution.issues.isEmpty(), resolution.issues.toString())
        val bitmap = ImageBitmap(32, 16)
        val canvas = Canvas(bitmap)
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, Size(32f, 16f)) {
            listOf(8f to "blue", 24f to "green").forEach { (x, kind) ->
                drawRenderFeature(canvas, OptimizedRenderFeature(
                    OptimizedGeometry.Point(listOf(x to 8f)), mapOf("kind" to kind)
                ), createFontFamilyResolver(), Density(1f), resolution.style!!.layers.single(),
                    0.0, 1f, 0f, 1f, 1f, 1f)
            }
        }
        val pixels = bitmap.toPixelMap()
        assertTrue(pixels[8, 8].blue > 0.9f && pixels[8, 8].green < 0.1f, "blue icon: ${pixels[8, 8]}")
        assertEquals(0.25f, pixels[8, 8].alpha, 0.02f)
        assertTrue(pixels[24, 8].green > 0.9f && pixels[24, 8].blue < 0.1f)
        assertEquals(0.5f, pixels[24, 8].alpha, 0.02f)
    }

    test("SDF icon halo follows width, blur, feature color, and icon opacity") {
        val resolution = StyleResolver().resolve("""{
            "layers": [{"id": "icons", "type": "symbol", "source-layer": "places",
                "layout": {"icon-image": "dot"},
                "paint": {
                    "icon-color": "#ff0000",
                    "icon-opacity": 0.5,
                    "icon-halo-color": ["case", ["==", ["get", "kind"], "blue"], "rgba(0, 0, 255, 0.5)", "#00ff00"],
                    "icon-halo-width": ["get", "width"],
                    "icon-halo-blur": ["get", "blur"]
                }}]
        }""", sprites = mapOf("dot" to SpriteImage(sdfCircleSprite(), sdf = true)))
        assertTrue(resolution.issues.isEmpty(), resolution.issues.toString())
        val bitmap = ImageBitmap(144, 48)
        val canvas = Canvas(bitmap)
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, Size(144f, 48f)) {
            listOf(
                Triple(24f, 0.0, 0.0),
                Triple(72f, 2.0, 0.0),
                Triple(120f, 2.0, 2.0)
            ).forEachIndexed { index, (x, width, blur) ->
                val properties = mapOf("kind" to if (index == 0) "blue" else "green", "width" to width, "blur" to blur)
                drawRenderFeature(canvas, OptimizedRenderFeature(OptimizedGeometry.Point(listOf(x to 24f)), properties),
                    createFontFamilyResolver(), Density(1f), resolution.style!!.layers.single(),
                    0.0, 1f, 0f, 1f, 1f, 1f)
            }
        }
        val pixels = bitmap.toPixelMap()
        assertEquals(0f, pixels[33, 24].alpha, 0.02f)
        assertTrue(pixels[81, 24].green > 0.9f && pixels[81, 24].red < 0.1f)
        assertEquals(0.5f, pixels[81, 24].alpha, 0.1f)
        assertEquals(0f, pixels[83, 24].alpha, 0.02f)
        assertTrue(pixels[131, 24].alpha > 0.05f)
    }

    test("SDF halo remains visible when the icon color is transparent") {
        val style = StyleResolver().resolve("""{
            "layers": [{"id": "icons", "type": "symbol", "source-layer": "places",
                "layout": {"icon-image": "dot"},
                "paint": {"icon-color": "rgba(0, 0, 0, 0)", "icon-halo-color": "#0000ff", "icon-halo-width": 2}}]
        }""", sprites = mapOf("dot" to SpriteImage(sdfCircleSprite(), sdf = true))).style!!
        val bitmap = ImageBitmap(48, 48)
        val canvas = Canvas(bitmap)
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, Size(48f, 48f)) {
            drawRenderFeature(canvas, OptimizedRenderFeature(OptimizedGeometry.Point(listOf(24f to 24f)), emptyMap()),
                createFontFamilyResolver(), Density(1f), style.layers.single(), 0.0, 1f, 0f, 1f, 1f, 1f)
        }
        val pixel = bitmap.toPixelMap()[24, 24]
        assertTrue(pixel.blue > 0.9f && pixel.alpha > 0.9f)
    }

    test("SDF halo with zero or omitted width does not draw") {
        for (haloWidth in listOf("", ", \"icon-halo-width\": 0")) {
            val style = StyleResolver().resolve("""{
                "layers": [{"id": "icons", "type": "symbol", "source-layer": "places",
                    "layout": {"icon-image": "dot"},
                    "paint": {"icon-color": "rgba(0, 0, 0, 0)", "icon-halo-color": "#0000ff"$haloWidth}}]
            }""", sprites = mapOf("dot" to SpriteImage(sdfCircleSprite(), sdf = true))).style!!
            val bitmap = ImageBitmap(48, 48)
            val canvas = Canvas(bitmap)
            CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, Size(48f, 48f)) {
                drawRenderFeature(canvas, OptimizedRenderFeature(OptimizedGeometry.Point(listOf(24f to 24f)), emptyMap()),
                    createFontFamilyResolver(), Density(1f), style.layers.single(), 0.0, 1f, 0f, 1f, 1f, 1f)
            }
            assertEquals(0f, bitmap.toPixelMap()[24, 24].alpha)
        }
    }

    test("SDF halo width stays in screen pixels when icon size changes") {
        val style = StyleResolver().resolve("""{
            "layers": [{"id": "icons", "type": "symbol", "source-layer": "places",
                "layout": {"icon-image": "dot", "icon-size": ["get", "size"]},
                "paint": {"icon-color": "#ff0000", "icon-halo-color": "#00ff00", "icon-halo-width": 2}}]
        }""", sprites = mapOf("dot" to SpriteImage(sdfCircleSprite(), sdf = true))).style!!
        val bitmap = ImageBitmap(144, 64)
        val canvas = Canvas(bitmap)
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, Size(144f, 64f)) {
            listOf(32f to 1.0, 96f to 2.0).forEach { (x, size) ->
                drawRenderFeature(canvas, OptimizedRenderFeature(
                    OptimizedGeometry.Point(listOf(x to 32f)), mapOf("size" to size)
                ), createFontFamilyResolver(), Density(1f), style.layers.single(),
                    0.0, 1f, 0f, 1f, 1f, 1f)
            }
        }
        val pixels = bitmap.toPixelMap()
        assertTrue(pixels[32, 32].red > 0.9f && pixels[32, 32].green < 0.1f)
        assertTrue(pixels[41, 32].green > 0.9f)
        assertTrue(pixels[114, 32].green > 0.9f)
        assertEquals(0f, pixels[117, 32].alpha, 0.02f)
    }

    test("SDF halo width stays in screen pixels when a tile is overzoomed") {
        val style = StyleResolver().resolve("""{
            "layers": [{"id": "icons", "type": "symbol", "source-layer": "places",
                "layout": {"icon-image": "dot"},
                "paint": {"icon-color": "#ff0000", "icon-halo-color": "#00ff00", "icon-halo-width": 2}}]
        }""", sprites = mapOf("dot" to SpriteImage(sdfCircleSprite(), sdf = true))).style!!
        val feature = MVTFeature(1UL, RawMVTGeomType.POINT, listOf(listOf(16 to 16)), emptyMap())
        val tile = optimizeMVTile(VectorTile(0, 0, 0, MVTile(listOf(MVTLayer("places", 32, listOf(feature))))), style)

        fun render(scaleAdjustment: Float): ImageBitmap {
            val bitmap = ImageBitmap(64, 64)
            val canvas = Canvas(bitmap)
            CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, Size(64f, 64f)) {
                drawVectorTileLayerWithClipping(tile, style.layers.single(), emptyMap(), TileDimension(32.dp, 32.dp),
                    CanvasDrawReference.Zero, scaleAdjustment, canvas, createFontFamilyResolver(), Density(1f),
                    0.0, 0f, 1f)
            }
            return bitmap
        }

        val normal = render(1f).toPixelMap()
        val overzoomed = render(2f).toPixelMap()
        assertTrue(normal[25, 16].green > 0.9f)
        assertTrue(overzoomed[41, 32].green > 0.9f)
        assertEquals(0f, normal[27, 16].alpha, 0.02f)
        assertEquals(0f, overzoomed[43, 32].alpha, 0.02f)
    }

    test("fill pattern repeats inside polygon and applies opacity") {
        val sprite = ImageBitmap(4, 2)
        Canvas(sprite).drawRect(Rect(0f, 0f, 2f, 2f), Paint().apply { color = Color.Red })
        Canvas(sprite).drawRect(Rect(2f, 0f, 4f, 2f), Paint().apply { color = Color.Blue })
        val style = StyleResolver().resolve("""{
            "layers": [{"id": "pattern", "type": "fill", "source-layer": "landuse",
                "paint": {"fill-pattern": "stripes", "fill-color": "#00ff00", "fill-opacity": 0.5}}]
        }""", sprites = mapOf("stripes" to SpriteImage(sprite, pixelRatio = 2.0))).style!!
        val bitmap = ImageBitmap(8, 8)
        val canvas = Canvas(bitmap)
        val feature = OptimizedRenderFeature(OptimizedGeometry.Polygon(Path().apply {
            fillType = PathFillType.EvenOdd
            addRect(Rect(0f, 0f, 8f, 8f))
            addRect(Rect(5f, 5f, 7f, 7f))
        }), emptyMap())
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, Size(8f, 8f)) {
            drawRenderFeature(canvas, feature, createFontFamilyResolver(), Density(1f), style.layers.single(),
                0.0, 1f, 0f, 1f, 1f, 1f)
        }
        val pixels = bitmap.toPixelMap()
        assertTrue(pixels[2, 3].red > 0.9f && pixels[2, 3].green < 0.1f)
        assertTrue(pixels[3, 3].blue > 0.9f && pixels[3, 3].green < 0.1f)
        assertEquals(0.5f, pixels[2, 3].alpha, 0.02f)
        assertEquals(0f, pixels[6, 6].alpha)
    }

    test("fill pattern keeps its phase across tiles") {
        val sprite = ImageBitmap(3, 1)
        Canvas(sprite).drawRect(Rect(0f, 0f, 1f, 1f), Paint().apply { color = Color.Red })
        Canvas(sprite).drawRect(Rect(1f, 0f, 2f, 1f), Paint().apply { color = Color.Blue })
        Canvas(sprite).drawRect(Rect(2f, 0f, 3f, 1f), Paint().apply { color = Color.Green })
        val layer = StyleResolver().resolve("""{
            "layers": [{"id": "pattern", "type": "fill", "source-layer": "landuse",
                "paint": {"fill-pattern": "stripes"}}]
        }""", sprites = mapOf("stripes" to SpriteImage(sprite))).style!!.layers.single() as CompiledFillLayer
        val bitmap = ImageBitmap(8, 2)
        val canvas = Canvas(bitmap)
        val geometry = OptimizedGeometry.Polygon(Path().apply { addRect(Rect(0f, 0f, 4f, 2f)) })
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, Size(8f, 2f)) {
            drawFillFeature(canvas, geometry, emptyMap(), layer, 0.0, 1f, 1f, 1f, tileWorldX = 0.0)
            canvas.save()
            canvas.translate(4f, 0f)
            drawFillFeature(canvas, geometry, emptyMap(), layer, 0.0, 1f, 1f, 1f, tileWorldX = 4.0)
            canvas.restore()
        }
        val pixels = bitmap.toPixelMap()
        assertTrue(pixels[3, 0].red > 0.9f)
        assertTrue(pixels[4, 0].blue > 0.9f)
        assertTrue(pixels[5, 0].green > 0.9f)
    }

    test("background pattern keeps its phase across active tiles") {
        val sprite = ImageBitmap(3, 1)
        Canvas(sprite).drawRect(Rect(0f, 0f, 1f, 1f), Paint().apply { color = Color.Red })
        Canvas(sprite).drawRect(Rect(1f, 0f, 2f, 1f), Paint().apply { color = Color.Blue })
        Canvas(sprite).drawRect(Rect(2f, 0f, 3f, 1f), Paint().apply { color = Color.Green })
        val layer = StyleResolver().resolve("""{
            "layers": [{"id": "background", "type": "background",
                "paint": {"background-pattern": "stripes"}}]
        }""", sprites = mapOf("stripes" to SpriteImage(sprite))).style!!.layers.single() as CompiledBackgroundLayer
        val bitmap = ImageBitmap(8, 2)
        val canvas = Canvas(bitmap)
        val tiles = listOf(VectorTile(0, 0, 0, MVTile(emptyList())), VectorTile(0, 0, 1, MVTile(emptyList())))
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, Size(8f, 2f)) {
            drawBackgroundForActiveTiles(layer, canvas, TileDimension(2.dp, 4.dp), CanvasDrawReference.Zero,
                ActiveTiles(0, tiles), 0.0, 1f)
        }
        val pixels = bitmap.toPixelMap()
        assertTrue(pixels[3, 0].red > 0.9f)
        assertTrue(pixels[4, 0].blue > 0.9f)
        assertTrue(pixels[5, 0].green > 0.9f)
    }

    test("fill opacity multiplies color alpha and fades the outline") {
        val path = Path().apply { addRect(Rect(4f, 4f, 28f, 28f)) }
        val geometry = OptimizedGeometry.Polygon(path)

        fun render(opacity: Double): ImageBitmap {
            val style = StyleResolver().resolve(
                Json.encodeToString(Style.serializer(), Style(
                    version = 8,
                    sources = emptyMap(),
                    layers = listOf(
                        StyleLayer(
                            id = "fill",
                            type = "fill",
                            sourceLayer = "landuse",
                            paint = mapOf(
                                "fill-color" to JsonPrimitive("rgba(255, 0, 0, 0.5)"),
                                "fill-opacity" to JsonPrimitive(opacity),
                                "fill-outline-color" to JsonPrimitive("rgba(0, 0, 255, 1)")
                            )
                        )
                    )
                ))
            ).style!!
            val bitmap = ImageBitmap(32, 32)
            drawFillFeature(Canvas(bitmap), geometry, emptyMap(), style.layers.single() as CompiledFillLayer, 0.0, 1f, 1f, 1f)
            return bitmap
        }

        assertEquals(0.25f, render(0.5).toPixelMap()[16, 16].alpha, 0.02f)

        val transparent = render(0.0).toPixelMap()
        assertTrue((0 until 32).all { y -> (0 until 32).all { x -> transparent[x, y].alpha == 0f } })
    }

    test("fill opacity accepts Float feature values") {
        val style = StyleResolver().resolve("""{
            "version": 8, "sources": {}, "layers": [
                {"id": "fill", "type": "fill", "source-layer": "land",
                 "paint": {"fill-color": "#ff0000", "fill-opacity": ["get", "opacity"]}}
            ]
        }""").style!!
        val path = Path().apply { addRect(Rect(4f, 4f, 28f, 28f)) }
        val bitmap = ImageBitmap(32, 32)

        drawFillFeature(Canvas(bitmap), OptimizedGeometry.Polygon(path), mapOf("opacity" to 0.25f), style.layers.single() as CompiledFillLayer, 0.0, 1f, 1f, 1f)

        assertEquals(0.25f, bitmap.toPixelMap()[16, 16].alpha, 0.02f)
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
                Json.encodeToString(Style.serializer(), Style(
                    version = 8,
                    sources = emptyMap(),
                    layers = listOf(StyleLayer(id = "fill", type = "fill", sourceLayer = "landuse", paint = paint))
                ))
            ).style!!
            val bitmap = ImageBitmap(32, 32)
            drawFillFeature(Canvas(bitmap), geometry, emptyMap(), style.layers.single() as CompiledFillLayer, 0.0, 1f, 1f, 1f)
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
            Json.encodeToString(Style.serializer(), Style(
                version = 8,
                sources = emptyMap(),
                layers = listOf(
                    StyleLayer(
                        id = "fill",
                        type = "fill",
                        sourceLayer = "landuse",
                        paint = mapOf(
                            "fill-color" to JsonPrimitive("rgba(255, 0, 0, 0)"),
                            "fill-outline-color" to JsonPrimitive("#0000ff")
                        )
                    )
                )
            ))
        ).style!!
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
                canvas, OptimizedGeometry.Polygon(path), emptyMap(), style.layers.single() as CompiledFillLayer,
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
            Json.encodeToString(Style.serializer(), Style(
                version = 8,
                sources = emptyMap(),
                layers = listOf(
                    StyleLayer(
                        id = "outline",
                        type = "line",
                        sourceLayer = "roads",
                        paint = mapOf(
                            "line-color" to JsonPrimitive("#0000ff"),
                            "line-width" to JsonPrimitive(6)
                        )
                    )
                )
            ))
        ).style!!

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
            drawLineFeature(canvas, path, emptyMap(), style.layers.single() as CompiledLineLayer, 0.0, tileScaleX, tileScaleY, screenScale)
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
            Json.encodeToString(Style.serializer(), Style(
                version = 8,
                sources = emptyMap(),
                layers = listOf(
                    StyleLayer(
                        id = "outline",
                        type = "line",
                        sourceLayer = "roads",
                        paint = mapOf(
                            "line-color" to JsonPrimitive("rgba(255, 0, 0, 0.5)"),
                            "line-opacity" to JsonPrimitive(0.5),
                            "line-width" to JsonPrimitive(4)
                        )
                    )
                )
            ))
        ).style!!
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
                Json.encodeToString(Style.serializer(), Style(
                    version = 8,
                    sources = emptyMap(),
                    layers = listOf(
                        StyleLayer(
                            id = "line",
                            type = "line",
                            sourceLayer = "roads",
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
                ))
            ).style!!
            val bitmap = ImageBitmap(32, 32)
            drawLineFeature(Canvas(bitmap), path, emptyMap(), style.layers.single() as CompiledLineLayer, 0.0, 1f, 1f, 1f)
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
            Json.encodeToString(Style.serializer(), Style(
                version = 8,
                sources = emptyMap(),
                layers = listOf(
                    StyleLayer(
                        id = "line",
                        type = "line",
                        sourceLayer = "roads",
                        layout = mapOf("line-join" to JsonPrimitive("none")),
                        paint = mapOf(
                            "line-color" to JsonPrimitive("#0000ff"),
                            "line-width" to JsonPrimitive(4)
                        )
                    )
                )
            ))
        ).style!!
        val path = Path().apply {
            addRect(Rect(8f, 8f, 56f, 56f))
            addRect(Rect(24f, 24f, 40f, 40f))
        }
        val bitmap = ImageBitmap(64, 64)
        drawLineFeature(Canvas(bitmap), path, emptyMap(), style.layers.single() as CompiledLineLayer, 0.0, 1f, 1f, 1f)
        val pixels = bitmap.toPixelMap()

        assertTrue(pixels[8, 32].alpha > 0.9f)
        assertTrue(pixels[24, 32].alpha > 0.9f)
        assertEquals(0f, pixels[16, 16].alpha)
        assertEquals(0f, pixels[32, 32].alpha)
    }

    test("line dasharray uses line widths and stays in screen pixels") {
        val style = StyleResolver().resolve(
            Json.encodeToString(Style.serializer(), Style(
                version = 8,
                sources = emptyMap(),
                layers = listOf(
                    StyleLayer(
                        id = "line",
                        type = "line",
                        sourceLayer = "roads",
                        paint = mapOf(
                            "line-color" to JsonPrimitive("#0000ff"),
                            "line-width" to JsonPrimitive(4),
                            "line-dasharray" to JsonArray(listOf(JsonPrimitive(2), JsonPrimitive(1)))
                        )
                    )
                )
            ))
        ).style!!

        fun render(tileScaleX: Float, tileScaleY: Float, screenScale: Float): ImageBitmap {
            val path = Path().apply {
                moveTo(8f / (tileScaleX * screenScale), 16f / (tileScaleY * screenScale))
                lineTo(72f / (tileScaleX * screenScale), 16f / (tileScaleY * screenScale))
            }
            val bitmap = ImageBitmap(80, 32)
            val canvas = Canvas(bitmap)
            canvas.scale(screenScale, screenScale)
            canvas.scale(tileScaleX, tileScaleY)
            drawLineFeature(canvas, path, emptyMap(), style.layers.single() as CompiledLineLayer, 0.0, tileScaleX, tileScaleY, screenScale)
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

    test("line widths and dash gaps account for display density") {
        val style = StyleResolver().resolve("""{
            "layers": [{"id": "border", "type": "line", "source-layer": "sub_border",
                "paint": {"line-color": "#000000", "line-width": 2, "line-dasharray": [2, 1]}}]
        }""").style!!
        val path = Path().apply { moveTo(16f, 32f); lineTo(112f, 32f) }

        fun render(displayDensity: Float): ImageBitmap {
            val bitmap = ImageBitmap(128, 64)
            val canvas = Canvas(bitmap)
            val density = Density(displayDensity)
            CanvasDrawScope().draw(density, LayoutDirection.Ltr, canvas, Size(128f, 64f)) {
                drawRenderFeature(
                    canvas,
                    OptimizedRenderFeature(OptimizedGeometry.LineString(path), emptyMap()),
                    createFontFamilyResolver(), density, style.layers.single(),
                    0.0, 1f, 0f, 1f, 1f, 1f
                )
            }
            return bitmap
        }

        val regular = render(1f).toPixelMap()
        val dense = render(2f).toPixelMap()
        assertTrue(regular[18, 32].alpha > 0.9f)
        assertTrue(regular[21, 32].alpha < 0.2f)
        assertTrue(dense[21, 32].alpha > 0.9f)
        assertTrue(dense[26, 32].alpha < 0.2f)
        assertTrue(dense[21, 30].alpha > regular[18, 30].alpha)
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
                Json.encodeToString(Style.serializer(), Style(
                    version = 8,
                    sources = emptyMap(),
                    layers = listOf(StyleLayer(id = "line", type = "line", sourceLayer = "roads", paint = paint))
                ))
            ).style!!
            val path = Path().apply {
                moveTo(8f, 16f)
                lineTo(72f, 16f)
            }
            val bitmap = ImageBitmap(80, 32)
            drawLineFeature(Canvas(bitmap), path, emptyMap(), style.layers.single() as CompiledLineLayer, 0.0, 1f, 1f, 1f)
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
                Json.encodeToString(Style.serializer(), Style(
                    version = 8,
                    sources = emptyMap(),
                    layers = listOf(
                        StyleLayer(
                            id = "line",
                            type = "line",
                            sourceLayer = "roads",
                            layout = mapOf("line-cap" to JsonPrimitive(cap)),
                            paint = mapOf(
                                "line-color" to JsonPrimitive("#0000ff"),
                                "line-width" to JsonPrimitive(4),
                                "line-dasharray" to JsonArray(dashArray.map(::JsonPrimitive))
                            )
                        )
                    )
                ))
            ).style!!
            val path = Path().apply {
                moveTo(8f, 16f)
                lineTo(72f, 16f)
            }
            val bitmap = ImageBitmap(80, 32)
            drawLineFeature(Canvas(bitmap), path, emptyMap(), style.layers.single() as CompiledLineLayer, 0.0, 1f, 1f, 1f)
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
                Json.encodeToString(Style.serializer(), Style(
                    version = 8,
                    sources = emptyMap(),
                    layers = listOf(
                        StyleLayer(
                            id = "line",
                            type = "line",
                            sourceLayer = "roads",
                            paint = mapOf(
                                "line-color" to JsonPrimitive("#0000ff"),
                                "line-width" to JsonPrimitive(4),
                                "line-dasharray" to JsonArray(intervals.map(::JsonPrimitive))
                            )
                        )
                    )
                ))
            ).style!!
            val path = Path().apply {
                moveTo(8f, 16f)
                lineTo(72f, 16f)
            }
            val bitmap = ImageBitmap(80, 32)
            drawLineFeature(Canvas(bitmap), path, emptyMap(), style.layers.single() as CompiledLineLayer, 0.0, 1f, 1f, 1f)
            return bitmap
        }

        assertEquals(0f, render(listOf(0, 0)).toPixelMap()[32, 16].alpha)
        assertTrue(render(listOf(2, 0)).toPixelMap()[32, 16].alpha > 0.9f)
    }
}
