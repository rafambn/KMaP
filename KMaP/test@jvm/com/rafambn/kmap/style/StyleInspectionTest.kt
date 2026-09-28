package com.rafambn.kmap.style

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

    test("reports invalid values even when a missing source-layer omits the layer") {
        val result = StyleResolver().resolve("""{
            "layers": [
                {"id": "land", "type": "fill", "paint": {"fill-opacity": "half"}},
                {"id": "labels", "type": "symbol", "layout": {"text-font": ["Noto Sans", 42]}}
            ]
        }""")

        assertTrue(result.style?.layers?.isEmpty() == true)
        assertEquals(
            setOf(
                "/layers/0/source-layer", "/layers/0/paint/fill-opacity",
                "/layers/1/source-layer", "/layers/1/layout/text-font"
            ),
            result.issues.map { it.path }.toSet()
        )
        assertEquals(2, result.issues.count { it.kind == StyleIssue.Kind.INVALID })
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

    test("reports invalid literal property values without coercing strings to numbers") {
        val result = StyleResolver().resolve("""{
            "layers": [
                {"id": "land", "type": "fill", "source-layer": "land",
                 "paint": {"fill-opacity": "half", "fill-color": "not-a-color"}},
                {"id": "roads", "type": "line", "source-layer": "roads",
                 "paint": {"line-width": "half", "line-dasharray": [2, "1"]}},
                {"id": "places", "type": "symbol", "source-layer": "places",
                 "layout": {"text-font": ["Noto Sans", 42], "icon-size": ["get", "size"]}}
            ]
        }""")

        assertNotNull(result.style)
        assertEquals(
            setOf(
                "/layers/0/paint/fill-opacity", "/layers/0/paint/fill-color",
                "/layers/1/paint/line-width", "/layers/1/paint/line-dasharray",
                "/layers/2/layout/text-font"
            ),
            result.issues.map { it.path }.toSet()
        )
        assertTrue(result.issues.all { it.kind == StyleIssue.Kind.INVALID })
        val land = result.style.layers.first() as CompiledFillLayer
        assertNull(land.opacity?.evaluate(0.0, emptyMap(), null))
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

    test("finds unsupported expressions in arrays and step outputs while accepting calculated property names") {
        val result = StyleResolver().resolve("""{
            "version": 8, "sources": {}, "layers": [
                {"id": "roads", "type": "line", "source-layer": "roads",
                 "filter": ["in", "class", "park", "garden"],
                 "paint": {
                    "line-dasharray": ["feature-state", "dash"],
                    "line-width": ["step", ["zoom"], ["feature-state", "width"], 10, 4]
                 }},
                {"id": "labels", "type": "symbol", "source-layer": "places",
                 "filter": ["has", ["concat", "na", "me"]],
                 "layout": {"text-field": ["get", ["concat", "na", "me"]], "text-font": ["Noto Sans"]}}
            ]
        }""")

        assertNotNull(result.style)
        assertEquals(
            setOf("/layers/0/filter", "/layers/0/paint/line-dasharray", "/layers/0/paint/line-width/2"),
            result.issues.map { it.path }.toSet()
        )
    }

    test("reports legacy functions and filters as unsupported") {
        val result = StyleResolver().resolve("""{
            "version": 8, "sources": {}, "layers": [
                {"id": "roads", "type": "line", "source-layer": "roads",
                 "filter": ["all", ["!=", "class", "road"], ["has", "name"]],
                 "paint": {"line-width": {"stops": [[1, 2], [2, 8]]}}}
            ]
        }""")

        assertNotNull(result.style)
        assertEquals(setOf("/layers/0/filter/1", "/layers/0/paint/line-width"), result.issues.map { it.path }.toSet())
        assertTrue(result.issues.all { it.kind == StyleIssue.Kind.UNSUPPORTED })
        assertFalse(result.style.layers.single().filter!!.evaluate(0.0, mapOf("class" to "rail"), "LineString", null))
    }

    test("accepts modern filters with a literal left operand") {
        val result = StyleResolver().resolve("""{
            "layers": [{"id": "land", "type": "fill", "source-layer": "land",
                "filter": ["all", ["==", "park", ["get", "class"]], ["in", "park", ["get", "classes"]],
                    ["match", ["get", "class"], ["none", "park"], true, false],
                    ["has", "${'$'}id", ["literal", {"${'$'}id": 1}]]]}]
        }""")

        assertTrue(result.issues.isEmpty(), result.issues.toString())
        assertTrue(result.style!!.layers.single().filter!!.evaluate(
            0.0, mapOf("class" to "park", "classes" to listOf("park", "garden")), "Polygon", null
        ))
    }

    test("keeps font lists literal inside step outputs") {
        val result = StyleResolver().resolve("""{
            "version": 8, "sources": {}, "layers": [
                {"id": "labels", "type": "symbol", "source-layer": "places",
                 "layout": {"text-font": ["step", ["zoom"], ["literal", ["Noto Sans"]], 10, ["literal", ["Arial"]]]}}
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
