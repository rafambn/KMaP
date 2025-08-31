@file:OptIn(ExperimentalSerializationApi::class)

import com.rafambn.kmap.utils.vectorTile.CMD_MOVETO
import com.rafambn.kmap.utils.vectorTile.RawMVTFeature
import com.rafambn.kmap.utils.vectorTile.RawMVTGeomType
import com.rafambn.kmap.utils.vectorTile.RawMVTLayer
import com.rafambn.kmap.utils.vectorTile.RawMVTile
import com.rafambn.kmap.utils.vectorTile.RawMVTValue
import com.rafambn.kmap.utils.vectorTile.deparse
import com.rafambn.kmap.utils.vectorTile.parse
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RealTileIntegrationTest {

    fun deserializeMVT(decompressedBytes: ByteArray): RawMVTile {
        return ProtoBuf.decodeFromByteArray(RawMVTile.serializer(), decompressedBytes)
    }

    fun serializeMVT(mvtTile: RawMVTile): ByteArray {
        return ProtoBuf.encodeToByteArray(RawMVTile.serializer(), mvtTile)
    }

    private fun loadData(tileName: String): ByteArray {
        val resourcePath = "tiles/$tileName"

        val inputStream: InputStream? = javaClass.classLoader.getResourceAsStream(resourcePath)

        if (inputStream == null) {
            throw IllegalArgumentException("Resource not found: $resourcePath")
        }

        return inputStream.use {
            val buffer = ByteArray(1024)
            val outputStream = ByteArrayOutputStream()
            var bytesRead: Int
            while (it.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }
            outputStream.toByteArray()
        }
    }

    private fun getTileLevel10(): ByteArray = loadData("ohm_10_550_337.pbf")

    private fun getTileLevel14(): ByteArray = loadData("ohm_14_8800_5374.pbf")

    private fun getTileLevel16(): ByteArray = loadData("ohm_16_35200_21496.pbf")
    @Test
    fun testParseVariousZoomLevels() {
        val tile10 = getTileLevel10()
        val parsed10 = deserializeMVT(tile10).parse()
        assertTrue(parsed10.layers.isNotEmpty())

        val tile14 = getTileLevel14()
        val parsed14 = deserializeMVT(tile14).parse()
        assertTrue(parsed14.layers.isNotEmpty())

        val tile16 = getTileLevel16()
        val parsed16 = deserializeMVT(tile16).parse()
        assertTrue(parsed16.layers.isNotEmpty())
    }

    @Test
    fun testRoundTripSerializationForDetailedTile() {
        val originalTileData = getTileLevel14()
        val mvtTile = deserializeMVT(originalTileData)
        val parsedTile = mvtTile.parse()

        val deparsedTile = parsedTile.deparse()
        val reserializedBytes = serializeMVT(deparsedTile)
        val finalTile = deserializeMVT(reserializedBytes)
        val finalParsed = finalTile.parse()

        assertEquals(parsedTile.layers.size, finalParsed.layers.size)

        parsedTile.layers.forEachIndexed { index, originalLayer ->
            val finalLayer = finalParsed.layers[index]
            assertEquals(originalLayer.name, finalLayer.name)
            assertEquals(originalLayer.extent, finalLayer.extent)
            assertEquals(originalLayer.features.size, finalLayer.features.size)
        }
    }

    @Test
    fun testBasicFeatureAndLayerProperties() {
        val testTiles = listOf(getTileLevel10(), getTileLevel14(), getTileLevel16())

        testTiles.forEach { tileData ->
            val parsedTile = deserializeMVT(tileData).parse()
            assertTrue(parsedTile.layers.isNotEmpty())

            parsedTile.layers.forEach { layer ->
                assertTrue(layer.name.isNotEmpty())
                assertTrue(layer.extent > 0)

                layer.features.forEach { feature ->
                    assertTrue(feature.geometry.isNotEmpty() || feature.type == RawMVTGeomType.UNKNOWN)
                    feature.geometry.forEach { part ->
                        assertTrue(part.isNotEmpty())
                    }
                    feature.properties.forEach { (key, value) ->
                        assertTrue(key.isNotEmpty())
                        assertNotNull(value)
                    }
                }
            }
        }
    }

    @Test
    fun testEdgeCases() {
        val emptyTile = RawMVTile(layers = emptyList())
        val emptyTileData = serializeMVT(emptyTile)
        val parsedEmptyTile = deserializeMVT(emptyTileData).parse()
        assertTrue(parsedEmptyTile.layers.isEmpty())

        val singleFeatureTile = RawMVTile(
            layers = listOf(
                RawMVTLayer(
                    name = "single",
                    extent = 4096,
                    keys = listOf("test"),
                    values = listOf(RawMVTValue(string_value = "value")),
                    features = listOf(
                        RawMVTFeature(
                            id = 1L,
                            type = RawMVTGeomType.POINT,
                            geometry = listOf((CMD_MOVETO or (1 shl 3)), 2048, 2048),
                            tags = listOf(0, 0)
                        )
                    )
                )
            )
        )
        val singleFeatureData = serializeMVT(singleFeatureTile)
        val parsedSingleFeature = deserializeMVT(singleFeatureData).parse()
        assertEquals(1, parsedSingleFeature.layers.size)
        assertEquals(1, parsedSingleFeature.layers[0].features.size)
    }
}
