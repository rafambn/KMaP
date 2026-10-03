package com.rafambn.kmap.style

import com.rafambn.kmap.style.compiled.CompiledBackgroundLayer
import com.rafambn.kmap.style.compiled.CompiledFillLayer
import com.rafambn.kmap.style.model.Style
import com.rafambn.kmap.style.model.StyleLayer
import de.infix.testBalloon.framework.core.testSuite
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive

val StyleResolverCoverageTest by testSuite {
    test("compiles absent and explicit layer values") {
        val emptyLayer = StyleLayer(id = "empty", type = "background")
        val populatedLayer = StyleLayer(
            id = "populated",
            type = "fill",
            sourceLayer = "landuse",
            minzoom = 2.0,
            maxzoom = 12.0,
            filter = listOf(JsonPrimitive("=="), JsonArray(listOf(JsonPrimitive("get"), JsonPrimitive("class"))), JsonPrimitive("park")),
            layout = mapOf(
                "visibility" to JsonPrimitive("none"),
                "title" to JsonPrimitive("ignored")
            ),
            paint = mapOf(
                "fill-color" to JsonPrimitive("#00ff00"),
                "fill-opacity" to JsonPrimitive(0.5),
                "unused" to JsonPrimitive(true)
            )
        )
        val rawStyle = Style(version = 8, sources = emptyMap(), layers = listOf(emptyLayer, populatedLayer))

        val resolution = StyleResolver().resolve(Json.encodeToString(Style.serializer(), rawStyle))
        val optimized = resolution.style!!
        assertTrue(resolution.issues.any { it.path == "/layers/1/paint/unused" && it.kind == StyleIssue.Kind.UNSUPPORTED })
        assertTrue(resolution.issues.any { it.path == "/layers/1/layout/title" && it.kind == StyleIssue.Kind.UNSUPPORTED })

        val empty = optimized.layers[0] as CompiledBackgroundLayer
        assertEquals(0.0, empty.minZoom)
        assertEquals(Double.POSITIVE_INFINITY, empty.maxZoom)
        assertNull(empty.filter)
        assertTrue(empty.visibility.evaluate(0.0, emptyMap(), null) == true)
        assertNull(empty.color)
        assertNull(empty.opacity)

        val populated = optimized.layers[1] as CompiledFillLayer
        assertEquals(2.0, populated.minZoom)
        assertEquals(12.0, populated.maxZoom)
        assertTrue(populated.filter!!.evaluate(0.0, mapOf("class" to "park"), "Polygon", null))
        assertFalse(populated.filter.evaluate(0.0, mapOf("class" to "water"), "Polygon", null))
        assertFalse(populated.visibility.evaluate(0.0, emptyMap(), null)!!)
        assertEquals(0.5, populated.opacity?.evaluate(0.0, emptyMap(), null))
        assertNotNull(populated.color?.evaluate(0.0, emptyMap(), null))
        assertNull(populated.outlineColor)
        assertNull(populated.antialias)

        val visibleStyle = Style(
            version = 8,
            sources = emptyMap(),
            layers = listOf(
                StyleLayer(
                    id = "visible",
                    type = "fill",
                    sourceLayer = "landuse",
                    layout = mapOf("visibility" to JsonPrimitive("visible"))
                )
            )
        )
        val visible = StyleResolver().resolve(Json.encodeToString(Style.serializer(), visibleStyle)).style!!.layers.single()
        assertTrue(visible.visibility.evaluate(0.0, emptyMap(), null)!!)
    }

    test("uses false for filters whose expression result is not boolean") {
        val style = Style(
            version = 8,
            sources = emptyMap(),
            layers = listOf(StyleLayer(id = "filter", type = "fill", sourceLayer = "landuse", filter = listOf(JsonPrimitive("unknown"))))
        )

        val filter = StyleResolver().resolve(Json.encodeToString(Style.serializer(), style)).style!!.layers.single().filter

        assertFalse(filter!!.evaluate(0.0, emptyMap(), "Polygon", null))
    }

    test("converts expression results to the property type") {
        val rawJson = """{"layers":[{"id":"dynamic","type":"fill","source-layer":"landuse","paint":{"fill-color":["get","color"],"fill-opacity":["get","opacity"],"fill-antialias":["get","antialias"]}}]}"""
        val layer = StyleResolver().resolve(rawJson).style!!.layers.single() as CompiledFillLayer

        assertNull(layer.color?.evaluate(0.0, mapOf("color" to 42), null))
        assertNull(layer.opacity?.evaluate(0.0, mapOf("opacity" to "half"), null))
        assertNull(layer.antialias?.evaluate(0.0, mapOf("antialias" to "false"), null))
        assertEquals(0.5, layer.opacity?.evaluate(0.0, mapOf("opacity" to 0.5f), null))
        assertEquals(false, layer.antialias?.evaluate(0.0, mapOf("antialias" to false), null))
        assertNotNull(layer.color?.evaluate(0.0, mapOf("color" to "#123456"), null))
    }

    test("evaluates visibility expressions as booleans") {
        val style = Style(
            version = 8,
            sources = emptyMap(),
            layers = listOf(
                StyleLayer(
                    id = "literal-none",
                    type = "fill",
                    sourceLayer = "landuse",
                    layout = mapOf(
                        "visibility" to JsonArray(listOf(JsonPrimitive("literal"), JsonPrimitive("none")))
                    )
                ),
                StyleLayer(
                    id = "case-none",
                    type = "fill",
                    sourceLayer = "landuse",
                    layout = mapOf(
                        "visibility" to JsonArray(
                            listOf(JsonPrimitive("case"), JsonPrimitive(true), JsonPrimitive("none"), JsonPrimitive("visible"))
                        )
                    )
                ),
                StyleLayer(
                    id = "property-visibility",
                    type = "fill",
                    sourceLayer = "landuse",
                    layout = mapOf(
                        "visibility" to JsonArray(listOf(JsonPrimitive("get"), JsonPrimitive("visibility")))
                    )
                )
            )
        )

        val layers = StyleResolver().resolve(Json.encodeToString(Style.serializer(), style)).style!!.layers

        assertFalse(layers[0].visibility.evaluate(0.0, emptyMap(), null)!!)
        assertFalse(layers[1].visibility.evaluate(0.0, emptyMap(), null)!!)
        assertFalse(layers[2].visibility.evaluate(0.0, mapOf("visibility" to "none"), null)!!)
        assertTrue(layers[2].visibility.evaluate(0.0, mapOf("visibility" to "visible"), null)!!)
    }
}
