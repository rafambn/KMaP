import com.rafambn.kmap.utils.vectorTile.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MVTParserTest {

    @Test
    fun testDecodeZigZag() {
        assertEquals(0, decodeZigZag(0))
        assertEquals(1, decodeZigZag(2))
        assertEquals(2, decodeZigZag(4))
        assertEquals(15, decodeZigZag(30))

        assertEquals(-1, decodeZigZag(1))
        assertEquals(-2, decodeZigZag(3))
        assertEquals(-15, decodeZigZag(29))
        assertEquals(-16, decodeZigZag(31))
    }

    @Test
    fun testResolveFeaturePropertiesEmpty() {
        val feature = RawMVTFeature(tags = emptyList())
        val layer = RawMVTLayer(name = "test", keys = emptyList(), values = emptyList())

        val properties = resolveFeatureProperties(feature, layer)

        assertTrue(properties.isEmpty())
    }

    @Test
    fun testResolveFeaturePropertiesWithStringValue() {
        val feature = RawMVTFeature(tags = listOf(0, 0))
        val layer = RawMVTLayer(
            name = "test",
            keys = listOf("name"),
            values = listOf(RawMVTValue(string_value = "test_value"))
        )

        val properties = resolveFeatureProperties(feature, layer)

        assertEquals(1, properties.size)
        assertEquals("test_value", properties["name"])
    }

    @Test
    fun testResolveFeaturePropertiesWithMultipleTypes() {
        val feature = RawMVTFeature(tags = listOf(0, 0, 1, 1, 2, 2, 3, 3))
        val layer = RawMVTLayer(
            name = "test",
            keys = listOf("string_prop", "int_prop", "float_prop", "bool_prop"),
            values = listOf(
                RawMVTValue(string_value = "hello"),
                RawMVTValue(int_value = 42L),
                RawMVTValue(float_value = 3.14f),
                RawMVTValue(bool_value = true)
            )
        )

        val properties = resolveFeatureProperties(feature, layer)

        assertEquals(4, properties.size)
        assertEquals("hello", properties["string_prop"])
        assertEquals(42L, properties["int_prop"])
        assertEquals(3.14f, properties["float_prop"])
        assertEquals(true, properties["bool_prop"])
    }

    @Test
    fun testResolveFeaturePropertiesWithInvalidIndices() {
        val feature = RawMVTFeature(tags = listOf(0, 5, 10, 0))
        val layer = RawMVTLayer(
            name = "test",
            keys = listOf("valid_key"),
            values = listOf(RawMVTValue(string_value = "valid_value"))
        )

        val properties = resolveFeatureProperties(feature, layer)

        assertEquals(0, properties.size)
    }

    @Test
    fun testResolveFeaturePropertiesWithOddTagsCount() {
        val feature = RawMVTFeature(tags = listOf(0, 0, 1))
        val layer = RawMVTLayer(
            name = "test",
            keys = listOf("key1", "key2"),
            values = listOf(RawMVTValue(string_value = "value1"), RawMVTValue(string_value = "value2"))
        )

        val properties = resolveFeatureProperties(feature, layer)

        assertEquals(1, properties.size)
        assertEquals("value1", properties["key1"])
    }

    @Test
    fun testDecodeFeatureGeometryPoint() {
        val feature = RawMVTFeature(
            type = RawMVTGeomType.POINT,
            geometry = listOf(
                (CMD_MOVETO or (1 shl 3)),
                2, 4
            )
        )

        val decodedCoordinates = decodeFeatureGeometry(feature)

        assertEquals(1, decodedCoordinates.size)
        assertEquals(1, decodedCoordinates[0].size)
        assertEquals(Pair(1, 2), decodedCoordinates[0][0])
    }

    @Test
    fun testDecodeFeatureGeometryLineString() {
        val feature = RawMVTFeature(
            type = RawMVTGeomType.LINESTRING,
            geometry = listOf(
                (CMD_MOVETO or (1 shl 3)),
                2, 4,
                (CMD_LINETO or (2 shl 3)),
                2, 2,
                2, 2
            )
        )

        val decodedCoordinates = decodeFeatureGeometry(feature)

        assertEquals(1, decodedCoordinates.size)
        assertEquals(3, decodedCoordinates[0].size)
        assertEquals(Pair(1, 2), decodedCoordinates[0][0])
        assertEquals(Pair(2, 3), decodedCoordinates[0][1])
        assertEquals(Pair(3, 4), decodedCoordinates[0][2])
    }

    @Test
    fun testDecodeFeatureGeometryPolygon() {
        val feature = RawMVTFeature(
            type = RawMVTGeomType.POLYGON,
            geometry = listOf(
                (CMD_MOVETO or (1 shl 3)),
                2, 2,
                (CMD_LINETO or (3 shl 3)),
                2, 0,
                0, 2,
                1, 1,
                CMD_CLOSEPATH
            )
        )

        val decodedCoordinates = decodeFeatureGeometry(feature)

        assertEquals(1, decodedCoordinates.size)
        assertEquals(4, decodedCoordinates[0].size)
        assertEquals(Pair(1, 1), decodedCoordinates[0][0])
        assertEquals(Pair(2, 1), decodedCoordinates[0][1])
        assertEquals(Pair(2, 2), decodedCoordinates[0][2])
        assertEquals(Pair(1, 1), decodedCoordinates[0][3])
    }

    @Test
    fun testDecodeFeatureGeometryMultiPart() {
        val feature = RawMVTFeature(
            type = RawMVTGeomType.POINT,
            geometry = listOf(
                (CMD_MOVETO or (1 shl 3)),
                2, 4,
                (CMD_MOVETO or (1 shl 3)),
                4, 6
            )
        )

        val decodedCoordinates = decodeFeatureGeometry(feature)

        assertEquals(2, decodedCoordinates.size)
        assertEquals(1, decodedCoordinates[0].size)
        assertEquals(1, decodedCoordinates[1].size)
        assertEquals(Pair(1, 2), decodedCoordinates[0][0])
        assertEquals(Pair(3, 5), decodedCoordinates[1][0])
    }

    @Test
    fun testDecodeFeatureGeometryEmptyGeometry() {
        val feature = RawMVTFeature(
            type = RawMVTGeomType.UNKNOWN,
            geometry = emptyList()
        )

        val decodedCoordinates = decodeFeatureGeometry(feature)

        assertTrue(decodedCoordinates.isEmpty())
    }

    @Test
    fun testParseMVTWithEmptyTile() {
        val mvtTile = RawMVTile(layers = emptyList())

        val parsed = mvtTile.parse()

        assertTrue(parsed.layers.isEmpty())
    }

    @Test
    fun testParseMVTWithSingleLayer() {
        val feature = RawMVTFeature(
            id = 123L,
            type = RawMVTGeomType.POINT,
            geometry = listOf(
                (CMD_MOVETO or (1 shl 3)),
                2, 4
            ),
            tags = listOf(0, 0)
        )

        val layer = RawMVTLayer(
            name = "test_layer",
            extent = 4096,
            keys = listOf("name"),
            values = listOf(RawMVTValue(string_value = "test_feature")),
            features = listOf(feature)
        )

        val mvtTile = RawMVTile(layers = listOf(layer))

        val parsed = mvtTile.parse()

        assertEquals(1, parsed.layers.size)
        val parsedLayer = parsed.layers[0]
        assertEquals("test_layer", parsedLayer.name)
        assertEquals(4096, parsedLayer.extent)
        assertEquals(1, parsedLayer.features.size)

        val parsedFeature = parsedLayer.features[0]
        assertEquals(123L, parsedFeature.id)
        assertEquals(RawMVTGeomType.POINT, parsedFeature.type)
        assertEquals(1, parsedFeature.geometry.size)
        assertEquals(Pair(1, 2), parsedFeature.geometry[0][0])
        assertEquals("test_feature", parsedFeature.properties["name"])
    }

    @Test
    fun testParseMVTWithFeatureWithoutId() {
        val feature = RawMVTFeature(
            id = 0L,
            type = RawMVTGeomType.POINT,
            geometry = listOf(
                (CMD_MOVETO or (1 shl 3)),
                2, 4
            )
        )

        val layer = RawMVTLayer(
            name = "test_layer",
            features = listOf(feature)
        )

        val mvtTile = RawMVTile(layers = listOf(layer))
        val parsed = mvtTile.parse()

        assertNull(parsed.layers[0].features[0].id)
    }
}
