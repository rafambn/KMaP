package com.rafambn.kmap.style

import de.infix.testBalloon.framework.core.testSuite
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

val StyleResolverCoverageTest by testSuite {
    test("compiles absent and explicit layer values") {
        val emptyLayer = StyleLayer(id = "empty", type = "background")
        val populatedLayer = StyleLayer(
            id = "populated",
            type = "fill",
            minzoom = 2.0,
            maxzoom = 12.0,
            filter = listOf(JsonPrimitive("=="), JsonArray(listOf(JsonPrimitive("get"), JsonPrimitive("class"))), JsonPrimitive("park")),
            layout = mapOf(
                "visibility" to JsonPrimitive("none"),
                "title" to JsonArray(listOf(JsonPrimitive("get"), JsonPrimitive("name"))),
                "nested" to JsonObject(mapOf("enabled" to JsonPrimitive(true)))
            ),
            paint = mapOf(
                "null" to JsonNull,
                "boolean" to JsonPrimitive(true),
                "number" to JsonPrimitive(7),
                "string" to JsonPrimitive("value"),
                "array" to JsonArray(listOf(JsonPrimitive(1), JsonNull, JsonPrimitive(false))),
                "object" to JsonObject(mapOf("count" to JsonPrimitive(2)))
            )
        )
        val rawStyle = Style(version = 8, sources = emptyMap(), layers = listOf(emptyLayer, populatedLayer))

        val optimized = StyleResolver().resolve(rawStyle)

        val empty = optimized.layers[0]
        assertEquals(0.0, empty.minZoom)
        assertEquals(24.0, empty.maxZoom)
        assertNull(empty.filter)
        assertTrue(empty.layout.visibility.evaluate(0.0, emptyMap(), null) == true)
        assertTrue(empty.layout.properties.isEmpty())
        assertTrue(empty.paint.properties.isEmpty())

        val populated = optimized.layers[1]
        assertEquals(2.0, populated.minZoom)
        assertEquals(12.0, populated.maxZoom)
        assertTrue(populated.filter!!.evaluate(mapOf("class" to "park"), "Polygon", null))
        assertFalse(populated.filter.evaluate(mapOf("class" to "water"), "Polygon", null))
        assertEquals(setOf("class"), populated.filter.requiredProperties)
        assertFalse(populated.layout.visibility.evaluate(0.0, emptyMap(), null)!!)
        assertEquals("Place", populated.layout.properties.getValue("title").evaluate(0.0, mapOf("name" to "Place"), null))
        assertEquals(setOf("name"), populated.layout.properties.getValue("title").requiredProperties)
        assertEquals(mapOf("enabled" to true), populated.layout.properties.getValue("nested").evaluate(0.0, emptyMap(), null))
        assertNull(populated.paint.properties.getValue("null").evaluate(0.0, emptyMap(), null))
        assertEquals(true, populated.paint.properties.getValue("boolean").evaluate(0.0, emptyMap(), null))
        assertEquals(7.0, populated.paint.properties.getValue("number").evaluate(0.0, emptyMap(), null))
        assertEquals("value", populated.paint.properties.getValue("string").evaluate(0.0, emptyMap(), null))
        assertEquals(listOf(1.0, null, false), populated.paint.properties.getValue("array").evaluate(0.0, emptyMap(), null))
        assertEquals(mapOf("count" to 2.0), populated.paint.properties.getValue("object").evaluate(0.0, emptyMap(), null))

        val visibleStyle = Style(
            version = 8,
            sources = emptyMap(),
            layers = listOf(
                StyleLayer(
                    id = "visible",
                    type = "fill",
                    layout = mapOf("visibility" to JsonPrimitive("visible"))
                )
            )
        )
        val visible = StyleResolver().resolve(visibleStyle).layers.single()
        assertTrue(visible.layout.visibility.evaluate(0.0, emptyMap(), null)!!)
    }

    test("uses false for filters whose expression result is not boolean") {
        val style = Style(
            version = 8,
            sources = emptyMap(),
            layers = listOf(StyleLayer(id = "filter", type = "fill", filter = listOf(JsonPrimitive("unknown"))))
        )

        val filter = StyleResolver().resolve(style).layers.single().filter

        assertFalse(filter!!.evaluate(emptyMap(), "Polygon", null))
    }

    test("evaluates visibility expressions as booleans") {
        val style = Style(
            version = 8,
            sources = emptyMap(),
            layers = listOf(
                StyleLayer(
                    id = "literal-none",
                    type = "fill",
                    layout = mapOf(
                        "visibility" to JsonArray(listOf(JsonPrimitive("literal"), JsonPrimitive("none")))
                    )
                ),
                StyleLayer(
                    id = "case-none",
                    type = "fill",
                    layout = mapOf(
                        "visibility" to JsonArray(
                            listOf(JsonPrimitive("case"), JsonPrimitive(true), JsonPrimitive("none"), JsonPrimitive("visible"))
                        )
                    )
                ),
                StyleLayer(
                    id = "property-visibility",
                    type = "fill",
                    layout = mapOf(
                        "visibility" to JsonArray(listOf(JsonPrimitive("get"), JsonPrimitive("visibility")))
                    )
                )
            )
        )

        val layers = StyleResolver().resolve(style).layers

        assertFalse(layers[0].layout.visibility.evaluate(0.0, emptyMap(), null)!!)
        assertFalse(layers[1].layout.visibility.evaluate(0.0, emptyMap(), null)!!)
        assertEquals(setOf("visibility"), layers[2].layout.visibility.requiredProperties)
        assertFalse(layers[2].layout.visibility.evaluate(0.0, mapOf("visibility" to "none"), null)!!)
        assertTrue(layers[2].layout.visibility.evaluate(0.0, mapOf("visibility" to "visible"), null)!!)
    }
}
