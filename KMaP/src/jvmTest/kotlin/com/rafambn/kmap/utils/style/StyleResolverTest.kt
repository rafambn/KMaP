package com.rafambn.kmap.utils.style

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class StyleResolverTest {

    @Test
    fun testResolveSimpleStyle() {
        val rawStyleJson = """
            {
                "version": 8,
                "name": "Test Style",
                "sources": {
                    "test-source": {
                        "type": "vector",
                        "url": "http://example.com"
                    }
                },
                "layers": [
                    {
                        "id": "test-layer",
                        "type": "fill",
                        "source": "test-source",
                        "source-layer": "test-layer",
                        "filter": ["==", ["get", "class"], "park"],
                        "paint": {
                            "fill-color": ["rgba", 0, 128, 0, 0.5]
                        }
                    }
                ]
            }
        """.trimIndent()

        val rawStyle = Json{ ignoreUnknownKeys = true }.decodeFromString(Style.serializer(), rawStyleJson)
        val resolver = StyleResolver()
        val optimizedStyle = resolver.resolve(rawStyle)

        assertEquals(1, optimizedStyle.layers.size)
        val layer = optimizedStyle.layers[0]
        assertEquals("test-layer", layer.id)

        assertNotNull(layer.filter)
        val featureProperties = mapOf("class" to "park")
        assertTrue(layer.filter!!.evaluate(featureProperties, "Polygon", null))

        val featureProperties2 = mapOf("class" to "street")
        assertFalse(layer.filter!!.evaluate(featureProperties2, "Polygon", null))

        assertNotNull(layer.paint.properties["fill-color"])
        val color = layer.paint.properties["fill-color"]!!.evaluate(0.0, emptyMap(), null) as Color
        assertEquals(Color(0, 128, 0, 127), color)
    }

    @Test
    fun testResolveColorString() {
        val rawStyleJson = """
            {
                "version": 8,
                "name": "Test Style",
                "sources": {
                    "test-source": {
                        "type": "vector",
                        "url": "http://example.com"
                    }
                },
                "layers": [
                    {
                        "id": "test-layer",
                        "type": "fill",
                        "source": "test-source",
                        "source-layer": "test-layer",
                        "paint": {
                            "fill-color": "#ff0000"
                        }
                    }
                ]
            }
        """.trimIndent()

        val rawStyle = Json{ ignoreUnknownKeys = true }.decodeFromString(Style.serializer(), rawStyleJson)
        val resolver = StyleResolver()
        val optimizedStyle = resolver.resolve(rawStyle)

        val layer = optimizedStyle.layers[0]
        assertNotNull(layer.paint.properties["fill-color"])
        val color = layer.paint.properties["fill-color"]!!.evaluate(0.0, emptyMap(), null) as Color
        assertEquals(Color(255, 0, 0, 255), color)
    }
}