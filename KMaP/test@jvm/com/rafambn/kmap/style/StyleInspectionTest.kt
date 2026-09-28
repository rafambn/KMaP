package com.rafambn.kmap.style

import de.infix.testBalloon.framework.core.testSuite
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive

val StyleInspectionTest by testSuite {
    test("omits vector layers without a source-layer and keeps background") {
        val result = StyleResolver().resolve("""{
            "layers": [
                {"id": "background", "type": "background"},
                {"id": "fill", "type": "fill"},
                {"id": "line", "type": "line", "source-layer": null},
                {"id": "symbol", "type": "symbol"},
                {"id": "valid", "type": "fill", "source-layer": "landuse"}
            ]
        }""")

        assertEquals(listOf("background", "valid"), result.style?.layers?.map { it.id })
        assertEquals(
            setOf("/layers/1/source-layer", "/layers/2/source-layer", "/layers/3/source-layer"),
            result.issues.map { it.path }.toSet()
        )
    }

    test("reports unsupported fields before JSON decoding drops them") {
        val result = StyleResolver().resolve("""{
            "version": 8,
            "sources": {"tiles": {"type": "vector", "url": "mapbox://tiles", "extra": true}},
            "imports": [],
            "layers": [
                {"id": "land", "type": "fill", "source": "tiles", "source-layer": "land",
                 "paint": {"fill-pattern": "dots", "fill-color": ["case", true, ["format", "x"], "blue"]}},
                {"id": "places", "type": "circle", "source": "tiles", "source-layer": "places"}
            ]
        }""")

        assertNotNull(result.style)
        assertEquals(listOf("land"), result.style.layers.map { it.id })
        assertEquals(
            setOf("/imports", "/sources/tiles/url", "/sources/tiles/extra", "/layers/0/paint/fill-pattern", "/layers/0/paint/fill-color/2", "/layers/1/type"),
            result.issues.map { it.path }.toSet()
        )
        assertEquals("land", result.issues.single { it.path == "/layers/0/paint/fill-color/2" }.layerId)
    }

    test("accepts plain colored text and literal arrays") {
        val result = StyleResolver().resolve("""{
            "version": 8,
            "sources": {"tiles": {"type": "vector"}},
            "layers": [
                {"id": "roads", "type": "line", "source": "tiles", "source-layer": "roads",
                 "paint": {"line-dasharray": [2, 1]}},
                {"id": "labels", "type": "symbol", "source": "tiles", "source-layer": "places",
                 "layout": {"text-field": ["match", ["get", "kind"], ["city", "town"], "Place", "Other"], "text-font": ["Noto Sans"]},
                 "paint": {"text-color": "blue"}}
            ]
        }""")

        assertNotNull(result.style)
        assertTrue(result.issues.isEmpty(), result.issues.toString())
    }

    test("serialized styles retain inspectable paint and layout") {
        val style = Style(
            version = 8,
            sources = emptyMap(),
            layers = listOf(StyleLayer(
                id = "land", type = "fill", sourceLayer = "land",
                paint = mapOf("fill-pattern" to JsonPrimitive("dots")),
                layout = mapOf("visibility" to JsonArray(listOf(JsonPrimitive("feature-state"), JsonPrimitive("hidden"))))
            ))
        )

        val result = StyleResolver().resolve(Json.encodeToString(Style.serializer(), style))

        assertNotNull(result.style)
        assertEquals(setOf("/layers/0/paint/fill-pattern", "/layers/0/layout/visibility"), result.issues.map { it.path }.toSet())
    }

    test("finds unsupported expressions in arrays and stop outputs while accepting calculated property names") {
        val result = StyleResolver().resolve("""{
            "version": 8, "sources": {}, "layers": [
                {"id": "roads", "type": "line", "source-layer": "roads",
                 "filter": ["in", "class", "park", "garden"],
                 "paint": {
                    "line-dasharray": ["feature-state", "dash"],
                    "line-width": {"stops": [[0, ["feature-state", "width"]], [10, 4]]}
                 }},
                {"id": "labels", "type": "symbol", "source-layer": "places",
                 "filter": ["has", ["concat", "na", "me"]],
                 "layout": {"text-field": ["get", ["concat", "na", "me"]], "text-font": ["Noto Sans"]}}
            ]
        }""")

        assertNotNull(result.style)
        assertEquals(
            setOf("/layers/0/filter", "/layers/0/paint/line-dasharray", "/layers/0/paint/line-width/stops/0/1"),
            result.issues.map { it.path }.toSet()
        )
    }

    test("reports legacy function options that the evaluator ignores") {
        val result = StyleResolver().resolve("""{
            "version": 8, "sources": {}, "layers": [
                {"id": "roads", "type": "line", "source-layer": "roads",
                 "paint": {"line-width": {"property": "rank", "type": "categorical", "stops": [[1, 2], [2, 8]]}}}
            ]
        }""")

        assertNotNull(result.style)
        assertEquals(setOf("/layers/0/paint/line-width/property", "/layers/0/paint/line-width/type"), result.issues.map { it.path }.toSet())
    }

    test("keeps font lists literal inside legacy stop outputs") {
        val result = StyleResolver().resolve("""{
            "version": 8, "sources": {}, "layers": [
                {"id": "labels", "type": "symbol", "source-layer": "places",
                 "layout": {"text-font": {"type": "exponential", "stops": [[0, ["Noto Sans"]], [10, ["Arial"]]]}}}
            ]
        }""")

        assertNotNull(result.style)
        assertTrue(result.issues.isEmpty(), result.issues.toString())
    }

    test("returns an issue when JSON cannot be decoded") {
        val invalidJson = StyleResolver().resolve("{")
        assertNull(invalidJson.style)
        assertEquals(StyleIssue.Kind.INVALID, invalidJson.issues.single().kind)

        val invalidStyle = StyleResolver().resolve("""{"version":8,"sources":{},"layers":[],"unexpected":true,"center":"wrong"}""")
        assertNull(invalidStyle.style)
        assertTrue(invalidStyle.issues.any { it.path == "/unexpected" })
        assertTrue(invalidStyle.issues.any { it.kind == StyleIssue.Kind.INVALID })
    }
}
