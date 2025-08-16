package com.rafambn.kmapvectorsupport

import com.rafambn.kmapvectorsupport.tileSpec.CMD_MOVETO
import com.rafambn.kmapvectorsupport.tileSpec.Feature
import com.rafambn.kmapvectorsupport.tileSpec.GeomType
import com.rafambn.kmapvectorsupport.tileSpec.Layer
import com.rafambn.kmapvectorsupport.tileSpec.MVTile
import com.rafambn.kmapvectorsupport.tileSpec.Value
import com.rafambn.kmapvectorsupport.tileSpec.deparseMVT
import com.rafambn.kmapvectorsupport.tileSpec.deserializeMVT
import com.rafambn.kmapvectorsupport.tileSpec.parseMVT
import com.rafambn.kmapvectorsupport.tileSpec.serializeMVT
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RealTileIntegrationTest {
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
        val parsed10 = parseMVT(deserializeMVT(tile10))
        assertTrue(parsed10.layers.isNotEmpty())

        val tile14 = getTileLevel14()
        val parsed14 = parseMVT(deserializeMVT(tile14))
        assertTrue(parsed14.layers.isNotEmpty())

        val tile16 = getTileLevel16()
        val parsed16 = parseMVT(deserializeMVT(tile16))
        assertTrue(parsed16.layers.isNotEmpty())
    }

    @Test
    fun testRoundTripSerializationForDetailedTile() {
        val originalTileData = getTileLevel14()
        val mvtTile = deserializeMVT(originalTileData)
        val parsedTile = parseMVT(mvtTile)

        val deparsedTile = deparseMVT(parsedTile)
        val reserializedBytes = serializeMVT(deparsedTile)
        val finalTile = deserializeMVT(reserializedBytes)
        val finalParsed = parseMVT(finalTile)

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
            val parsedTile = parseMVT(deserializeMVT(tileData))
            assertTrue(parsedTile.layers.isNotEmpty())

            parsedTile.layers.forEach { layer ->
                assertTrue(layer.name.isNotEmpty())
                assertTrue(layer.extent > 0)

                layer.features.forEach { feature ->
                    assertTrue(feature.geometry.isNotEmpty() || feature.type == GeomType.UNKNOWN)
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
        val emptyTile = MVTile(layers = emptyList())
        val emptyTileData = serializeMVT(emptyTile)
        val parsedEmptyTile = parseMVT(deserializeMVT(emptyTileData))
        assertTrue(parsedEmptyTile.layers.isEmpty())

        val singleFeatureTile = MVTile(
            layers = listOf(
                Layer(
                    name = "single",
                    extent = 4096,
                    keys = listOf("test"),
                    values = listOf(Value(string_value = "value")),
                    features = listOf(
                        Feature(
                            id = 1L,
                            type = GeomType.POINT,
                            geometry = listOf((CMD_MOVETO or (1 shl 3)), 2048, 2048),
                            tags = listOf(0, 0)
                        )
                    )
                )
            )
        )
        val singleFeatureData = serializeMVT(singleFeatureTile)
        val parsedSingleFeature = parseMVT(deserializeMVT(singleFeatureData))
        assertEquals(1, parsedSingleFeature.layers.size)
        assertEquals(1, parsedSingleFeature.layers[0].features.size)
    }
}
