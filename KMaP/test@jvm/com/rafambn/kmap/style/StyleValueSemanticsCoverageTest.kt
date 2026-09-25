package com.rafambn.kmap.style

import de.infix.testBalloon.framework.core.testSuite
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlinx.serialization.json.JsonPrimitive

val StyleValueSemanticsCoverageTest by testSuite {
    test("covers style model copies and equality checks") {
        val light = Light("map", listOf(1.0, 2.0, 3.0), "#ffffff", 0.5)
        val transition = Transition(300, 25)
        val source = Source(
            type = "vector",
            url = "url",
            tiles = listOf("tile"),
            minzoom = 1,
            maxzoom = 15,
            attribution = "attribution",
            tileSize = 512,
            data = "data",
            buffer = 2,
            tolerance = 0.1,
            cluster = true,
            clusterRadius = 50,
            clusterMaxZoom = 10
        )
        val layer = StyleLayer(
            id = "layer",
            type = "fill",
            source = "source",
            sourceLayer = "landuse",
            minzoom = 1.0,
            maxzoom = 15.0,
            filter = listOf(JsonPrimitive("==")),
            layout = mapOf("visibility" to JsonPrimitive("visible")),
            paint = mapOf("opacity" to JsonPrimitive(0.5))
        )
        val style = Style(
            version = 8,
            name = "style",
            metadata = mapOf("owner" to JsonPrimitive("test")),
            center = listOf(1.0, 2.0),
            zoom = 3.0,
            bearing = 4.0,
            pitch = 5.0,
            light = light,
            sources = mapOf("source" to source),
            layers = listOf(layer),
            sprite = "sprite",
            glyphs = "glyphs",
            transition = transition
        )

        assertEquals(light, light.copy())
        assertEquals(true, light.equals(light))
        listOf(
            light.copy(anchor = "viewport"),
            light.copy(position = listOf(3.0, 2.0, 1.0)),
            light.copy(color = "#000000"),
            light.copy(intensity = 1.0)
        ).forEach { assertNotEquals(light, it) }
        assertEquals(false, light.equals(null))
        assertNotEquals(light, Any())

        assertEquals(transition, transition.copy())
        assertEquals(true, transition.equals(transition))
        assertNotEquals(transition, transition.copy(duration = 100))
        assertNotEquals(transition, transition.copy(delay = 10))
        assertEquals(false, transition.equals(null))
        assertNotEquals(transition, Any())

        assertEquals(source, source.copy())
        assertEquals(true, source.equals(source))
        listOf(
            source.copy(type = "raster"),
            source.copy(url = "other"),
            source.copy(tiles = listOf("other")),
            source.copy(minzoom = 2),
            source.copy(maxzoom = 16),
            source.copy(attribution = "other"),
            source.copy(tileSize = 256),
            source.copy(data = "other"),
            source.copy(buffer = 3),
            source.copy(tolerance = 0.2),
            source.copy(cluster = false),
            source.copy(clusterRadius = 60),
            source.copy(clusterMaxZoom = 11)
        ).forEach { assertNotEquals(source, it) }
        assertEquals(false, source.equals(null))
        assertNotEquals(source, Any())

        assertEquals(layer, layer.copy())
        assertEquals(true, layer.equals(layer))
        listOf(
            layer.copy(id = "other"),
            layer.copy(type = "line"),
            layer.copy(source = "other"),
            layer.copy(sourceLayer = "other"),
            layer.copy(minzoom = 2.0),
            layer.copy(maxzoom = 16.0),
            layer.copy(filter = listOf(JsonPrimitive("!"))),
            layer.copy(layout = mapOf("visibility" to JsonPrimitive("none"))),
            layer.copy(paint = mapOf("opacity" to JsonPrimitive(1.0)))
        ).forEach { assertNotEquals(layer, it) }
        assertEquals(false, layer.equals(null))
        assertNotEquals(layer, Any())

        assertEquals(style, style.copy())
        assertEquals(true, style.equals(style))
        listOf(
            style.copy(version = 9),
            style.copy(name = "other"),
            style.copy(metadata = mapOf("owner" to JsonPrimitive("other"))),
            style.copy(center = listOf(2.0, 1.0)),
            style.copy(zoom = 4.0),
            style.copy(bearing = 5.0),
            style.copy(pitch = 6.0),
            style.copy(light = light.copy(intensity = 1.0)),
            style.copy(sources = emptyMap()),
            style.copy(layers = emptyList()),
            style.copy(sprite = "other"),
            style.copy(glyphs = "other"),
            style.copy(transition = transition.copy(delay = 10))
        ).forEach { assertNotEquals(style, it) }
        assertEquals(false, style.equals(null))
        assertNotEquals(style, Any())
    }
}
