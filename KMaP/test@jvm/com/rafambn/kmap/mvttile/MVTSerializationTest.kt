@file:OptIn(ExperimentalSerializationApi::class)

package com.rafambn.kmap.mvttile

import de.infix.testBalloon.framework.core.testSuite
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf

private fun deserializeMVT(decompressedBytes: ByteArray): RawMVTile {
    return ProtoBuf.decodeFromByteArray(RawMVTile.serializer(), decompressedBytes)
}

private fun serializeMVT(mvtTile: RawMVTile): ByteArray {
    return ProtoBuf.encodeToByteArray(RawMVTile.serializer(), mvtTile)
}

private fun createTestMVTile(): RawMVTile {
    val feature1 = RawMVTFeature(
        id = 1L,
        type = RawMVTGeomType.POINT,
        geometry = listOf(
            (CMD_MOVETO or (1 shl 3)),
            2, 4
        ),
        tags = listOf(0, 0, 1, 1)
    )

    val feature2 = RawMVTFeature(
        id = 2L,
        type = RawMVTGeomType.LINESTRING,
        geometry = listOf(
            (CMD_MOVETO or (1 shl 3)),
            2, 4,
            (CMD_LINETO or (2 shl 3)),
            2, 2,
            2, 2
        ),
        tags = listOf(0, 2, 2, 3)
    )

    val layer = RawMVTLayer(
        name = "test_layer",
        extent = 4096,
        keys = listOf("name", "type", "category"),
        values = listOf(
            RawMVTValue(string_value = "test_point"),
            RawMVTValue(string_value = "landmark"),
            RawMVTValue(string_value = "test_line"),
            RawMVTValue(int_value = 42L)
        ),
        features = listOf(feature1, feature2)
    )

    return RawMVTile(layers = listOf(layer))
}

val MVTSerializationTest by testSuite {
    test("MVT arrays use packed Protobuf fields") {
        val feature = RawMVTFeature(tags = listOf(0, 1), geometry = listOf(9, 0))

        assertContentEquals(
            byteArrayOf(0x12, 0x02, 0x00, 0x01, 0x22, 0x02, 0x09, 0x00),
            ProtoBuf.encodeToByteArray(RawMVTFeature.serializer(), feature)
        )
    }

    test("MVT layers include the required version field") {
        val layer = RawMVTLayer(name = "x")

        assertContentEquals(
            byteArrayOf(0x78, 0x01, 0x0a, 0x01, 0x78),
            ProtoBuf.encodeToByteArray(RawMVTLayer.serializer(), layer)
        )
    }

    test("absent and zero feature IDs stay distinct") {
        val absent = RawMVTFeature()
        val zero = RawMVTFeature(id = 0L)

        assertContentEquals(byteArrayOf(), ProtoBuf.encodeToByteArray(RawMVTFeature.serializer(), absent))
        assertContentEquals(byteArrayOf(0x08, 0x00), ProtoBuf.encodeToByteArray(RawMVTFeature.serializer(), zero))
        assertEquals(0L, ProtoBuf.decodeFromByteArray(RawMVTFeature.serializer(), byteArrayOf(0x08, 0x00)).id)

        val features = RawMVTile(listOf(RawMVTLayer(name = "test", features = listOf(absent, zero))))
            .parse().layers.single().features
        assertNull(features[0].id)
        assertEquals(0UL, features[1].id)
    }

    test("unsigned IDs and values retain all 64 bits") {
        val highIdBytes = byteArrayOf(0x08, -1, -1, -1, -1, -1, -1, -1, -1, -1, 0x01)
        val highValueBytes = byteArrayOf(0x28, -1, -1, -1, -1, -1, -1, -1, -1, -1, 0x01)
        val rawId = ProtoBuf.decodeFromByteArray(RawMVTFeature.serializer(), highIdBytes)
        val rawValue = ProtoBuf.decodeFromByteArray(RawMVTValue.serializer(), highValueBytes)
        assertEquals(-1L, rawId.id)
        assertEquals(-1L, rawValue.uint_value)

        val layer = RawMVTLayer(name = "high", keys = listOf("value"), values = listOf(rawValue),
            features = listOf(rawId.copy(tags = listOf(0, 0))))
        val parsed = RawMVTile(listOf(layer)).parse()
        val feature = parsed.layers.single().features.single()
        assertEquals(ULong.MAX_VALUE, feature.id)
        assertEquals(ULong.MAX_VALUE, feature.properties["value"])

        val reencoded = parsed.deparse().layers.single()
        assertEquals(ULong.MAX_VALUE, reencoded.features.single().id?.toULong())
        assertEquals(ULong.MAX_VALUE, reencoded.values.single().uint_value?.toULong())
        assertContentEquals(highValueBytes, ProtoBuf.encodeToByteArray(RawMVTValue.serializer(), reencoded.values.single()))
        assertContentEquals(highIdBytes, ProtoBuf.encodeToByteArray(RawMVTFeature.serializer(), rawId))
    }

    test("signed vector tile properties use ZigZag encoding") {
        val positiveBytes = byteArrayOf(0x30, 0x3c)
        val negativeBytes = byteArrayOf(0x30, 0x05)

        val positive = ProtoBuf.decodeFromByteArray(RawMVTValue.serializer(), positiveBytes)
        val negative = ProtoBuf.decodeFromByteArray(RawMVTValue.serializer(), negativeBytes)

        assertEquals(30L, positive.sint_value)
        assertEquals(-3L, negative.sint_value)
        assertContentEquals(positiveBytes, ProtoBuf.encodeToByteArray(RawMVTValue.serializer(), positive))
        assertContentEquals(negativeBytes, ProtoBuf.encodeToByteArray(RawMVTValue.serializer(), negative))

        val layer = RawMVTLayer(name = "state_label", keys = listOf("admin_level"), values = listOf(positive),
            features = listOf(RawMVTFeature(tags = listOf(0, 0))))
        assertEquals(30L, RawMVTile(listOf(layer)).parse().layers.single().features.single().properties["admin_level"])
    }

    test("testEncodeDecodeZigZag") {
        val testValues = listOf(0, 1, -1, 2, -2, 15, -15, 16, -16, 100, -100, 1000000, -1000000,
            1073741823, -1073741824, 1073741824, -1073741825, Int.MAX_VALUE, Int.MIN_VALUE)

        testValues.forEach { original ->
            val encoded = encodeZigZag(original)
            val decoded = decodeZigZag(encoded)
            assertEquals(original, decoded, "ZigZag encoding/decoding failed for value: $original")
        }
    }

    test("testRoundTripSerialization") {
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

    test("testRoundTripParsing") {
        val originalTile = createTestMVTile()

        val parsedTile = originalTile.parse()

        val deparsedTile = parsedTile.deparse()

        val reparsedTile = deparsedTile.parse()

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

    test("testCompleteRoundTrip") {
        val originalTile = createTestMVTile()

        val serialized1 = serializeMVT(originalTile)
        val deserialized1 = deserializeMVT(serialized1)
        val parsed = deserialized1.parse()
        val deparsed = parsed.deparse()
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

    test("testGeometryEncoding") {
        val testCases = listOf(
            Triple(RawMVTGeomType.POINT, listOf(listOf(Pair(100, 200))), "Single point"),
            Triple(RawMVTGeomType.POINT, listOf(listOf(Pair(100, 200)), listOf(Pair(300, 400))), "Multi-point"),
            Triple(RawMVTGeomType.LINESTRING, listOf(listOf(Pair(0, 0), Pair(100, 100), Pair(200, 0))), "LineString"),
            Triple(RawMVTGeomType.POLYGON, listOf(listOf(Pair(0, 0), Pair(100, 0), Pair(100, 100), Pair(0, 100))), "Polygon")
        )

        testCases.forEach { (type, coordinates, description) ->
            val encoded = encodeFeatureGeometry(coordinates, type)

            val feature = RawMVTFeature(type = type, geometry = encoded)
            val decodedCoordinates = decodeFeatureGeometry(feature)

            assertEquals(coordinates, decodedCoordinates, "Coordinates mismatch for $description")
            if (type == RawMVTGeomType.POLYGON) {
                assertEquals(CMD_CLOSEPATH or (1 shl 3), encoded.last())
            }
        }
    }

    test("testPropertyEncoding") {
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
        val values = mutableListOf<RawMVTValue>()

        val tags = encodeFeatureProperties(properties, keys, values)

        val feature = RawMVTFeature(tags = tags)
        val layer = RawMVTLayer(name = "test", keys = keys, values = values)

        val decodedProperties = resolveFeatureProperties(feature, layer)

        assertEquals("test_value", decodedProperties["string_prop"])
        assertEquals(42L, decodedProperties["int_prop"])
        assertEquals(123L, decodedProperties["long_prop"])
        assertEquals(3.14f, decodedProperties["float_prop"])
        assertEquals(2.718, decodedProperties["double_prop"])
        assertEquals(true, decodedProperties["bool_prop"])
        assertFalse(decodedProperties.containsKey("null_prop"))
    }

    test("testEmptyTileRoundTrip") {
        val emptyTile = RawMVTile(layers = emptyList())

        val serialized = serializeMVT(emptyTile)
        val deserialized = deserializeMVT(serialized)

        assertEquals(0, deserialized.layers.size)
    }

    test("testEmptyLayerRoundTrip") {
        val emptyLayer = RawMVTLayer(name = "empty", features = emptyList())
        val tile = RawMVTile(layers = listOf(emptyLayer))

        val parsed = tile.parse()
        val deparsed = parsed.deparse()

        assertEquals(1, deparsed.layers.size)
        assertEquals("empty", deparsed.layers[0].name)
        assertEquals(0, deparsed.layers[0].features.size)
    }
}
