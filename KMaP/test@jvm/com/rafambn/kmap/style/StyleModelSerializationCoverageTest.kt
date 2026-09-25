package com.rafambn.kmap.style

import de.infix.testBalloon.framework.core.testSuite
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlinx.serialization.SerializationException
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.protobuf.ProtoBuf

@OptIn(ExperimentalSerializationApi::class)
val StyleModelSerializationCoverageTest by testSuite {
    testFixture { Json { ignoreUnknownKeys = true } } asParameterForEach {
        test("decodes optional style fields and their defaults") { json ->
            val minimalStyle = json.decodeFromString<Style>("""
                {
                    "version": 8,
                    "sources": {},
                    "layers": []
                }
            """.trimIndent())

            assertNull(minimalStyle.name)
            assertNull(minimalStyle.metadata)
            assertNull(minimalStyle.center)
            assertNull(minimalStyle.zoom)
            assertNull(minimalStyle.bearing)
            assertNull(minimalStyle.pitch)
            assertNull(minimalStyle.light)
            assertNull(minimalStyle.sprite)
            assertNull(minimalStyle.glyphs)
            assertNull(minimalStyle.transition)

            val completeStyle = json.decodeFromString<Style>("""
                {
                    "version": 8,
                    "name": "Complete",
                    "metadata": {"owner": "test", "enabled": true},
                    "center": [1.0, 2.0],
                    "zoom": 3.0,
                    "bearing": 4.0,
                    "pitch": 5.0,
                    "light": {
                        "anchor": "map",
                        "position": [1.0, 2.0, 3.0],
                        "color": "#ffffff",
                        "intensity": 0.5
                    },
                    "sources": {
                        "source": {
                            "type": "vector",
                            "url": "https://example.test/tiles.json",
                            "tiles": ["https://example.test/{z}/{x}/{y}.pbf"],
                            "minzoom": 1,
                            "maxzoom": 15,
                            "attribution": "source attribution",
                            "tileSize": 512,
                            "data": "https://example.test/data.json",
                            "buffer": 2,
                            "tolerance": 0.1,
                            "cluster": true,
                            "clusterRadius": 50,
                            "clusterMaxZoom": 10
                        }
                    },
                    "layers": [{
                        "id": "layer",
                        "type": "fill",
                        "source": "source",
                        "source-layer": "landuse",
                        "minzoom": 1.0,
                        "maxzoom": 15.0,
                        "filter": ["==", ["get", "class"], "park"],
                        "layout": {"visibility": "none", "sort-key": 2},
                        "paint": {"fill-color": "#00ff00", "fill-opacity": 0.5}
                    }],
                    "sprite": "https://example.test/sprite",
                    "glyphs": "https://example.test/{fontstack}/{range}.pbf",
                    "transition": {"duration": 300, "delay": 25}
                }
            """.trimIndent())

            assertEquals("Complete", completeStyle.name)
            assertEquals("test", completeStyle.metadata?.get("owner")?.toString()?.trim('"'))
            assertEquals(JsonPrimitive(true), completeStyle.metadata?.get("enabled"))
            assertEquals(listOf(1.0, 2.0), completeStyle.center)
            assertEquals(Light("map", listOf(1.0, 2.0, 3.0), "#ffffff", 0.5), completeStyle.light)
            assertEquals(
                Source(
                    type = "vector",
                    url = "https://example.test/tiles.json",
                    tiles = listOf("https://example.test/{z}/{x}/{y}.pbf"),
                    minzoom = 1,
                    maxzoom = 15,
                    attribution = "source attribution",
                    tileSize = 512,
                    data = "https://example.test/data.json",
                    buffer = 2,
                    tolerance = 0.1,
                    cluster = true,
                    clusterRadius = 50,
                    clusterMaxZoom = 10
                ),
                completeStyle.sources.getValue("source")
            )
            assertEquals("landuse", completeStyle.layers.single().sourceLayer)
            assertEquals("https://example.test/sprite", completeStyle.sprite)
            assertEquals(Transition(duration = 300, delay = 25), completeStyle.transition)

            val encoded = json.encodeToString(Style.serializer(), completeStyle)
            assertEquals(completeStyle, json.decodeFromString<Style>(encoded))
            assertEquals(minimalStyle, json.decodeFromString<Style>(json.encodeToString(Style.serializer(), minimalStyle)))
        }

        test("round trips nested model defaults when default encoding is enabled") { json ->
            val defaultsJson = Json { encodeDefaults = true }

            val light = json.decodeFromString<Light>("{}")
            assertEquals(light, json.decodeFromString<Light>(json.encodeToString(Light.serializer(), light)))
            assertEquals(light, defaultsJson.decodeFromString<Light>(defaultsJson.encodeToString(Light.serializer(), light)))

            val transition = json.decodeFromString<Transition>("{}")
            assertEquals(transition, json.decodeFromString<Transition>(json.encodeToString(Transition.serializer(), transition)))
            assertEquals(transition, defaultsJson.decodeFromString<Transition>(defaultsJson.encodeToString(Transition.serializer(), transition)))

            val source = json.decodeFromString<Source>("""{"type":"vector"}""")
            assertEquals(source, json.decodeFromString<Source>(json.encodeToString(Source.serializer(), source)))
            assertEquals(source, defaultsJson.decodeFromString<Source>(defaultsJson.encodeToString(Source.serializer(), source)))

            val layer = json.decodeFromString<StyleLayer>("""{"id":"layer","type":"fill"}""")
            assertEquals(layer, json.decodeFromString<StyleLayer>(json.encodeToString(StyleLayer.serializer(), layer)))
            assertEquals(layer, defaultsJson.decodeFromString<StyleLayer>(defaultsJson.encodeToString(StyleLayer.serializer(), layer)))

            val style = json.decodeFromString<Style>("""{"version":8,"sources":{},"layers":[]}""")
            assertEquals(style, json.decodeFromString<Style>(json.encodeToString(Style.serializer(), style)))
            assertEquals(style, defaultsJson.decodeFromString<Style>(defaultsJson.encodeToString(Style.serializer(), style)))
        }

        test("decodes explicit nulls for optional model fields") { json ->
            assertEquals(
                Light(),
                json.decodeFromString<Light>("""{"anchor":null,"position":null,"color":null,"intensity":null}""")
            )
            assertEquals(
                Transition(),
                json.decodeFromString<Transition>("""{"duration":null,"delay":null}""")
            )
            assertEquals(
                Source(type = "vector"),
                json.decodeFromString<Source>("""{"type":"vector","url":null,"tiles":null,"minzoom":null,"maxzoom":null,"attribution":null,"tileSize":null,"data":null,"buffer":null,"tolerance":null,"cluster":null,"clusterRadius":null,"clusterMaxZoom":null}""")
            )
            assertEquals(
                StyleLayer("layer", "fill"),
                json.decodeFromString<StyleLayer>("""{"id":"layer","type":"fill","source":null,"source-layer":null,"minzoom":null,"maxzoom":null,"filter":null,"layout":null,"paint":null}""")
            )
            assertEquals(
                Style(version = 8, sources = emptyMap(), layers = emptyList()),
                json.decodeFromString<Style>("""{"version":8,"name":null,"metadata":null,"center":null,"zoom":null,"bearing":null,"pitch":null,"light":null,"sources":{},"layers":[],"sprite":null,"glyphs":null,"transition":null}""")
            )
        }

        test("rejects styles that omit required model fields") { json ->
            assertFailsWith<SerializationException> {
                json.decodeFromString<Style>("""{"sources":{},"layers":[]}""")
            }
            assertFailsWith<SerializationException> {
                json.decodeFromString<Source>("""{"url":"https://example.test/tiles"}""")
            }
            assertFailsWith<SerializationException> {
                json.decodeFromString<StyleLayer>("""{"id":"layer"}""")
            }
        }
    }

    test("serializes style models through the protobuf decoder") {
        val style = Style(
            version = 8,
            name = "Binary",
            center = listOf(1.0, 2.0),
            zoom = 3.0,
            bearing = 4.0,
            pitch = 5.0,
            light = Light("map", listOf(1.0, 2.0, 3.0), "#ffffff", 0.5),
            sources = mapOf("source" to Source(type = "vector", url = "https://example.test/tiles")),
            layers = listOf(StyleLayer("layer", "fill", source = "source", sourceLayer = "landuse", minzoom = 1.0, maxzoom = 15.0)),
            sprite = "https://example.test/sprite",
            glyphs = "https://example.test/glyphs",
            transition = Transition(duration = 300, delay = 25)
        )

        val bytes = ProtoBuf.encodeToByteArray(Style.serializer(), style)
        val decoded = ProtoBuf.decodeFromByteArray(Style.serializer(), bytes)

        assertEquals(style, decoded)
    }
}
