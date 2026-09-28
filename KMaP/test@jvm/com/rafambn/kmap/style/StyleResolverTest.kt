package com.rafambn.kmap.style

import androidx.compose.ui.graphics.Color
import de.infix.testBalloon.framework.core.testSuite
import kotlin.test.*

val StyleResolverTest by testSuite {
    test("expands exact feature tokens in text and icon names") {
        val rawStyleJson = """
            {
                "version": 8,
                "sources": {"tiles": {"type": "vector"}},
                "layers": [
                    {
                        "id": "tokens",
                        "type": "symbol",
                        "source": "tiles",
                        "source-layer": "places",
                        "layout": {
                            "text-field": "{name}/{name:pt}/{ref}/{missing}",
                            "icon-image": "road_{ref_length}"
                        }
                    },
                    {
                        "id": "expression",
                        "type": "symbol",
                        "source": "tiles",
                        "source-layer": "places",
                        "layout": {"text-field": ["concat", "{ref}", " ", ["get", "ref"]]}
                    },
                    {
                        "id": "zoom-stops",
                        "type": "symbol",
                        "source": "tiles",
                        "source-layer": "places",
                        "layout": {
                            "text-field": {"stops": [[0, "{name}"], [10, "{ref}"]]},
                            "icon-image": {"stops": [[0, "road_{ref_length}"], [10, "road_{ref_length}"]]}
                        }
                    }
                ]
            }
        """.trimIndent()

        val layers = StyleResolver().resolve(rawStyleJson, locale = "pt").style!!.layers
        val properties = mapOf("name" to "Avenida", "name:pt" to "Nome", "ref" to "A12", "ref_length" to 3.0)
        val text = layers[0].layout.properties.getValue("text-field")
        val icon = layers[0].layout.properties.getValue("icon-image")

        assertEquals("Avenida/Nome/A12/", text.evaluate(16.0, properties, null))
        assertEquals("Avenida//A12/", text.evaluate(16.0, properties - "name:pt", null))
        assertEquals("road_3", icon.evaluate(16.0, properties, null))
        assertEquals("road_3", icon.evaluate(16.0, properties + ("ref_length" to 3f), null))
        assertEquals("road_2.5", icon.evaluate(16.0, properties + ("ref_length" to 2.5), null))
        assertEquals("{ref} A12", layers[1].layout.properties.getValue("text-field").evaluate(16.0, properties, null))
        val zoomText = layers[2].layout.properties.getValue("text-field")
        val zoomIcon = layers[2].layout.properties.getValue("icon-image")
        assertEquals("Avenida", zoomText.evaluate(0.0, properties, null))
        assertEquals("A12", zoomText.evaluate(10.0, properties, null))
        assertEquals("road_3", zoomIcon.evaluate(0.0, properties, null))
    }

    test("testResolveSimpleStyle") {
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

        val resolver = StyleResolver()
        val optimizedStyle = resolver.resolve(rawStyleJson).style!!

        assertEquals(1, optimizedStyle.layers.size)
        val layer = optimizedStyle.layers[0]
        assertEquals("test-layer", layer.id)

        assertNotNull(layer.filter)
        val featureProperties = mapOf("class" to "park")
        assertTrue(layer.filter.evaluate(0.0, featureProperties, "Polygon", null))

        val featureProperties2 = mapOf("class" to "street")
        assertFalse(layer.filter.evaluate(0.0, featureProperties2, "Polygon", null))

        assertNotNull(layer.paint.properties["fill-color"])
        val color = layer.paint.properties["fill-color"]!!.evaluate(0.0, emptyMap(), null) as Color
        assertEquals(0.5f, color.alpha, 0.003f)
        assertEquals(128 / 255f, color.green, 0.001f)
    }

    test("testResolveColorString") {
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

        val resolver = StyleResolver()
        val optimizedStyle = resolver.resolve(rawStyleJson).style!!

        val layer = optimizedStyle.layers[0]
        assertNotNull(layer.paint.properties["fill-color"])
        val color = layer.paint.properties["fill-color"]!!.evaluate(0.0, emptyMap(), null) as Color
        assertEquals(Color(255, 0, 0, 255), color)
    }

    test("testLineLayerWithExpressions") {
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
                        "id": "line-layer",
                        "type": "line",
                        "source": "test-source",
                        "source-layer": "roads",
                        "paint": {
                            "line-color": ["case", ["==", ["get", "class"], "motorway"], "#ff0000", "#00ff00"],
                            "line-width": ["interpolate", ["linear"], ["zoom"], 5, 1, 15, 4],
                            "line-opacity": ["case", ["==", ["get", "class"], "motorway"], 1, 0.7]
                        },
                        "layout": {
                            "line-cap": "round",
                            "line-join": "round"
                        }
                    }
                ]
            }
        """.trimIndent()

        val resolver = StyleResolver()
        val optimizedStyle = resolver.resolve(rawStyleJson).style!!

        assertEquals(1, optimizedStyle.layers.size)
        val layer = optimizedStyle.layers[0]
        assertEquals("line-layer", layer.id)
        assertEquals(CompiledLayerType.LINE, layer.type)

        // Test line-color expression
        val motorwayProps = mapOf("class" to "motorway")
        val color1 = layer.paint.properties["line-color"]!!.evaluate(10.0, motorwayProps, null) as Color
        assertEquals(Color(255, 0, 0, 255), color1)

        val normalProps = mapOf("class" to "secondary")
        val color2 = layer.paint.properties["line-color"]!!.evaluate(10.0, normalProps, null) as Color
        assertEquals(Color(0, 255, 0, 255), color2)

        // Test line-width interpolation
        val width1 = layer.paint.properties["line-width"]!!.evaluate(5.0, emptyMap(), null) as Double
        assertEquals(1.0, width1, 0.1)

        val width2 = layer.paint.properties["line-width"]!!.evaluate(15.0, emptyMap(), null) as Double
        assertEquals(4.0, width2, 0.1)

        val width3 = layer.paint.properties["line-width"]!!.evaluate(10.0, emptyMap(), null) as Double
        assertTrue(width3 > 1.0 && width3 < 4.0)

        // Test line-opacity
        val opacity1 = layer.paint.properties["line-opacity"]!!.evaluate(10.0, motorwayProps, null) as Double
        assertEquals(1.0, opacity1, 0.01)

        val opacity2 = layer.paint.properties["line-opacity"]!!.evaluate(10.0, normalProps, null) as Double
        assertEquals(0.7, opacity2, 0.01)
    }

    test("testBackgroundLayerWithExpressions") {
        val rawStyleJson = """
            {
                "version": 8,
                "name": "Test Style",
                "sources": {},
                "layers": [
                    {
                        "id": "background-layer",
                        "type": "background",
                        "paint": {
                            "background-color": "hsl(0, 100%, 50%)",
                            "background-opacity": ["interpolate", ["linear"], ["zoom"], 0, 0.5, 10, 1]
                        },
                        "layout": {
                            "visibility": "visible"
                        }
                    }
                ]
            }
        """.trimIndent()

        val resolver = StyleResolver()
        val optimizedStyle = resolver.resolve(rawStyleJson).style!!

        assertEquals(1, optimizedStyle.layers.size)
        val layer = optimizedStyle.layers[0]
        assertEquals("background-layer", layer.id)
        assertEquals(CompiledLayerType.BACKGROUND, layer.type)

        // Test background-color - HSL should parse to Color
        val color = layer.paint.properties["background-color"]!!.evaluate(0.0, emptyMap(), null) as? Color
        assertNotNull(color, "Background color should parse HSL to Color")
        // hsl(0, 100%, 50%) = red = rgb(255, 0, 0)
        assertEquals(Color(255, 0, 0, 255), color)

        // Test background-opacity
        val opacity0 = layer.paint.properties["background-opacity"]!!.evaluate(0.0, emptyMap(), null) as Double
        assertEquals(0.5, opacity0, 0.01)

        val opacity10 = layer.paint.properties["background-opacity"]!!.evaluate(10.0, emptyMap(), null) as Double
        assertEquals(1.0, opacity10, 0.01)

        val opacity5 = layer.paint.properties["background-opacity"]!!.evaluate(5.0, emptyMap(), null) as Double
        assertTrue(opacity5 > 0.5 && opacity5 < 1.0)
    }

    test("testSymbolLayerWithTextField") {
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
                        "id": "symbol-layer",
                        "type": "symbol",
                        "source": "test-source",
                        "source-layer": "poi",
                        "layout": {
                            "text-field": ["get", "name"],
                            "text-size": ["interpolate", ["linear"], ["zoom"], 10, 10, 18, 14],
                            "text-offset": [0, 10],
                            "visibility": "visible"
                        },
                        "paint": {
                            "text-opacity": 1,
                            "text-color": ["case", ["==", ["get", "type"], "restaurant"], "#ff0000", "#000000"],
                            "text-halo-color": "#ffffff",
                            "text-halo-width": 1
                        }
                    }
                ]
            }
        """.trimIndent()

        val resolver = StyleResolver()
        val optimizedStyle = resolver.resolve(rawStyleJson).style!!

        assertEquals(1, optimizedStyle.layers.size)
        val layer = optimizedStyle.layers[0]
        assertEquals("symbol-layer", layer.id)
        assertEquals(CompiledLayerType.SYMBOL, layer.type)

        // Test text-field
        val textField = layer.layout.properties["text-field"]?.evaluate(10.0, mapOf("name" to "Pizza Place"), null)
        assertEquals("Pizza Place", textField?.toString())

        // Test text-size interpolation
        val size10 = layer.layout.properties["text-size"]?.evaluate(10.0, emptyMap(), null) as? Double
        assertEquals(10.0, size10!!, 0.1)

        val size18 = layer.layout.properties["text-size"]?.evaluate(18.0, emptyMap(), null) as? Double
        assertEquals(14.0, size18!!, 0.1)

        // Test text-color
        val colorRest = layer.paint.properties["text-color"]?.evaluate(10.0, mapOf("type" to "restaurant"), null) as? Color
        assertEquals(Color(255, 0, 0, 255), colorRest)

        val colorOther = layer.paint.properties["text-color"]?.evaluate(10.0, mapOf("type" to "shop"), null) as? Color
        assertEquals(Color(0, 0, 0, 255), colorOther)
    }

    test("testSymbolLayerWithIconSize") {
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
                        "id": "icon-layer",
                        "type": "symbol",
                        "source": "test-source",
                        "source-layer": "poi",
                        "layout": {
                            "icon-image": "pin",
                            "icon-size": ["case", ["==", ["get", "class"], "major"], 1.5, 1],
                            "visibility": "visible"
                        },
                        "paint": {
                            "icon-opacity": ["interpolate", ["linear"], ["zoom"], 12, 0.5, 15, 1],
                            "icon-color": "#000000"
                        }
                    }
                ]
            }
        """.trimIndent()

        val resolver = StyleResolver()
        val optimizedStyle = resolver.resolve(rawStyleJson).style!!

        val layer = optimizedStyle.layers[0]
        assertEquals("icon-layer", layer.id)

        // Test icon-size
        val sizeMajor = layer.layout.properties["icon-size"]?.evaluate(10.0, mapOf("class" to "major"), null) as? Double
        assertEquals(1.5, sizeMajor!!, 0.01)

        val sizeNormal = layer.layout.properties["icon-size"]?.evaluate(10.0, mapOf("class" to "minor"), null) as? Double
        assertEquals(1.0, sizeNormal!!, 0.01)

        // Test icon-opacity
        val opacity12 = layer.paint.properties["icon-opacity"]?.evaluate(12.0, emptyMap(), null) as? Double
        assertEquals(0.5, opacity12!!, 0.01)

        val opacity15 = layer.paint.properties["icon-opacity"]?.evaluate(15.0, emptyMap(), null) as? Double
        assertEquals(1.0, opacity15!!, 0.01)
    }

    test("testFillLayerWithFilter") {
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
                        "id": "fill-layer",
                        "type": "fill",
                        "source": "test-source",
                        "source-layer": "landuse",
                        "filter": ["all", ["==", ["get", "class"], "park"], ["==", ["geometry-type"], "Polygon"]],
                        "paint": {
                            "fill-color": "#00ff00",
                            "fill-opacity": 0.5
                        }
                    }
                ]
            }
        """.trimIndent()

        val resolver = StyleResolver()
        val optimizedStyle = resolver.resolve(rawStyleJson).style!!

        val layer = optimizedStyle.layers[0]
        assertNotNull(layer.filter)

        // Test matching filter
        assertTrue(layer.filter.evaluate(0.0, mapOf("class" to "park"), "Polygon", null))

        // Test non-matching filter - different class
        assertFalse(layer.filter.evaluate(0.0, mapOf("class" to "street"), "Polygon", null))
    }
}
