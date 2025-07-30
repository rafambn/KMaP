package com.rafambn.mvtparser

import kotlin.test.*

class MVTSerializationTest {

    @Test
    fun testEncodeDecodeZigZag() {
        val testValues = listOf(0, 1, -1, 2, -2, 15, -15, 16, -16, 100, -100, 1000000, -1000000, 1073741823, -1073741824)

        testValues.forEach { original ->
            val encoded = encodeZigZag(original)
            val decoded = decodeZigZag(encoded)
            assertEquals(original, decoded, "ZigZag encoding/decoding failed for value: $original")
        }
    }

    @Test
    fun testRoundTripSerialization() {
        val originalTile = createTestMVTile()

        val serializedBytes = serializeMVT(originalTile)

        val deserializedTile = deserializeMVT(serializedBytes)

        assertEquals(originalTile.layers.size, deserializedTile.layers.size)

        originalTile.layers.forEachIndexed { layerIndex, originalLayer ->
            val deserializedLayer = deserializedTile.layers[layerIndex]
            assertEquals(originalLayer.name, deserializedLayer.name)
            assertEquals(originalLayer.extent, deserializedLayer.extent)
            assertEquals(originalLayer.keys, deserializedLayer.keys)
            assertEquals(originalLayer.values.size, deserializedLayer.values.size)
            assertEquals(originalLayer.features.size, deserializedLayer.features.size)

            originalLayer.features.forEachIndexed { featureIndex, originalFeature ->
                val deserializedFeature = deserializedLayer.features[featureIndex]
                assertEquals(originalFeature.id, deserializedFeature.id)
                assertEquals(originalFeature.type, deserializedFeature.type)
                assertEquals(originalFeature.tags, deserializedFeature.tags)
                assertEquals(originalFeature.geometry, deserializedFeature.geometry)
            }
        }
    }

    @Test
    fun testRoundTripParsing() {
        val originalTile = createTestMVTile()

        val parsedTile = parseMVT(originalTile)

        val deparsedTile = deparseMVT(parsedTile)

        val reparsedTile = parseMVT(deparsedTile)

        assertEquals(parsedTile.layers.size, reparsedTile.layers.size)

        parsedTile.layers.forEachIndexed { layerIndex, originalLayer ->
            val reparsedLayer = reparsedTile.layers[layerIndex]
            assertEquals(originalLayer.name, reparsedLayer.name)
            assertEquals(originalLayer.extent, reparsedLayer.extent)
            assertEquals(originalLayer.features.size, reparsedLayer.features.size)

            originalLayer.features.forEachIndexed { featureIndex, originalFeature ->
                val reparsedFeature = reparsedLayer.features[featureIndex]
                assertEquals(originalFeature.id, reparsedFeature.id)
                assertEquals(originalFeature.type, reparsedFeature.type)
                assertEquals(originalFeature.geometry, reparsedFeature.geometry)
                assertEquals(originalFeature.properties, reparsedFeature.properties)
            }
        }
    }

    @Test
    fun testCompleteRoundTrip() {
        val originalTile = createTestMVTile()

        val serialized1 = serializeMVT(originalTile)
        val deserialized1 = deserializeMVT(serialized1)
        val parsed = parseMVT(deserialized1)
        val deparsed = deparseMVT(parsed)
        val serialized2 = serializeMVT(deparsed)
        val deserialized2 = deserializeMVT(serialized2)

        assertEquals(originalTile.layers.size, deserialized2.layers.size)

        originalTile.layers.forEachIndexed { layerIndex, originalLayer ->
            val finalLayer = deserialized2.layers[layerIndex]
            assertEquals(originalLayer.name, finalLayer.name)
            assertEquals(originalLayer.extent, finalLayer.extent)
            assertEquals(originalLayer.features.size, finalLayer.features.size)
        }
    }

    @Test
    fun testGeometryEncoding() {
        val testCases = listOf(
            Triple(GeomType.POINT, listOf(listOf(Pair(100, 200))), "Single point"),
            Triple(GeomType.POINT, listOf(listOf(Pair(100, 200)), listOf(Pair(300, 400))), "Multi-point"),
            Triple(GeomType.LINESTRING, listOf(listOf(Pair(0, 0), Pair(100, 100), Pair(200, 0))), "LineString"),
            Triple(GeomType.POLYGON, listOf(listOf(Pair(0, 0), Pair(100, 0), Pair(100, 100), Pair(0, 100))), "Polygon")
        )

        testCases.forEach { (type, coordinates, description) ->
            val encoded = encodeFeatureGeometry(coordinates, type)

            val feature = Feature(type = type, geometry = encoded)
            val decoded = decodeFeatureGeometry(feature)

            assertEquals(type, decoded.type, "Geometry type mismatch for $description")
            assertEquals(coordinates, decoded.coordinates, "Coordinates mismatch for $description")
        }
    }

    @Test
    fun testPropertyEncoding() {
        val properties = mapOf(
            "string_prop" to "test_value",
            "int_prop" to 42,
            "long_prop" to 123L,
            "float_prop" to 3.14f,
            "double_prop" to 2.718,
            "bool_prop" to true,
            "null_prop" to null
        )

        val keys = mutableListOf<String>()
        val values = mutableListOf<Value>()

        val tags = encodeFeatureProperties(properties, keys, values)

        val feature = Feature(tags = tags)
        val layer = Layer(name = "test", keys = keys, values = values)

        val decodedProperties = resolveFeatureProperties(feature, layer)

        assertEquals("test_value", decodedProperties["string_prop"])
        assertEquals(42L, decodedProperties["int_prop"])
        assertEquals(123L, decodedProperties["long_prop"])
        assertEquals(3.14f, decodedProperties["float_prop"])
        assertEquals(2.718, decodedProperties["double_prop"])
        assertEquals(true, decodedProperties["bool_prop"])
        assertFalse(decodedProperties.containsKey("null_prop"))
    }

    @Test
    fun testEmptyTileRoundTrip() {
        val emptyTile = MVTile(layers = emptyList())

        val serialized = serializeMVT(emptyTile)
        val deserialized = deserializeMVT(serialized)

        assertEquals(0, deserialized.layers.size)
    }

    @Test
    fun testEmptyLayerRoundTrip() {
        val emptyLayer = Layer(name = "empty", features = emptyList())
        val tile = MVTile(layers = listOf(emptyLayer))

        val parsed = parseMVT(tile)
        val deparsed = deparseMVT(parsed)

        assertEquals(1, deparsed.layers.size)
        assertEquals("empty", deparsed.layers[0].name)
        assertEquals(0, deparsed.layers[0].features.size)
    }

    private fun createTestMVTile(): MVTile {
        val feature1 = Feature(
            id = 1L,
            type = GeomType.POINT,
            geometry = listOf(
                (CMD_MOVETO or (1 shl 3)),
                2, 4
            ),
            tags = listOf(0, 0, 1, 1)
        )

        val feature2 = Feature(
            id = 2L,
            type = GeomType.LINESTRING,
            geometry = listOf(
                (CMD_MOVETO or (1 shl 3)),
                2, 4,
                (CMD_LINETO or (2 shl 3)),
                2, 2,
                2, 2
            ),
            tags = listOf(0, 2, 2, 3)
        )

        val layer = Layer(
            name = "test_layer",
            extent = 4096,
            keys = listOf("name", "type", "category"),
            values = listOf(
                Value(string_value = "test_point"),
                Value(string_value = "landmark"),
                Value(string_value = "test_line"),
                Value(int_value = 42L)
            ),
            features = listOf(feature1, feature2)
        )

        return MVTile(layers = listOf(layer))
    }
}
