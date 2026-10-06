@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
package com.rafambn.kmap.source.internal

import com.rafambn.kmap.mvttile.*
import com.rafambn.kmap.source.*
import com.rafambn.kmap.source.preparation.*
import com.rafambn.kmap.style.StyleResolver
import de.infix.testBalloon.framework.core.testSuite
import kotlinx.coroutines.*
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.test.*

val PortablePreparationTest by testSuite {
    test("stored sparse and dense MVT fixtures retain geometry, properties and layer membership") {
        for (name in listOf("ohm_10_550_337.pbf", "ohm_14_8800_5374.pbf", "ohm_16_35200_21496.pbf")) {
            val bytes = checkNotNull(Thread.currentThread().contextClassLoader.getResourceAsStream("tiles/$name"))
                .use { it.readBytes() }
            val decoded = ProtoBuf.decodeFromByteArray(RawMVTile.serializer(), bytes).parse()
            val sourceLayers = decoded.layers.distinctBy { it.name }
            val layers = sourceLayers.flatMap { layer ->
                listOf("fill", "line", "symbol").map { type -> PreparationLayer("${layer.name}-$type", type, layer.name) }
            }
            val style = PreparationStyle(layers)
            val prepared = TilePreparation(style).prepare(bytes, 14, 5374, 8800)
            for (layer in sourceLayers) for (type in listOf("fill", "line", "symbol")) {
                val expected = layer.features.filter {
                    when (type) {
                        "fill" -> it.type == RawMVTGeomType.POLYGON
                        "line" -> it.type == RawMVTGeomType.LINESTRING || it.type == RawMVTGeomType.POLYGON
                        else -> it.type == RawMVTGeomType.POINT
                    }
                }
                val actual = prepared.layerFeatures.getValue("${layer.name}-$type").map { prepared.features[it] }
                assertEquals(expected.size, actual.size)
                expected.zip(actual).forEach { (old, new) ->
                    assertEquals(old.id?.toString(), new.id)
                    assertEquals(old.properties, new.properties.mapValues { it.value.toValue() })
                    assertEquals(layer.extent, new.extent)
                    assertContentEquals(old.geometry.flatMap { part -> part.flatMap { listOf(it.first, it.second) } }.toIntArray(), new.coordinates)
                    assertContentEquals(old.geometry.runningFold(0) { count, part -> count + part.size }.toIntArray(), new.parts)
                }
            }
        }
    }
    test("transport preserves every MVT scalar type and full unsigned IDs") {
        val properties = mapOf<String, Any>(
            "string" to "road", "bool" to true, "long" to Long.MIN_VALUE,
            "ulong" to ULong.MAX_VALUE, "float" to 1.25F, "double" to 2.5,
        )
        val tile = MVTile(listOf(MVTLayer("places", 4096, listOf(
            MVTFeature(ULong.MAX_VALUE, RawMVTGeomType.POINT, listOf(listOf(4 to 8)), properties),
        ))))
        val bytes = ProtoBuf.encodeToByteArray(RawMVTile.serializer(), tile.deparse())
        val prepared = TilePreparation(pipelineStyle().preparation!!).prepare(bytes, 0, 0, 0)
        val transported = ProtoBuf.decodeFromByteArray(PreparedTile.serializer(),
            ProtoBuf.encodeToByteArray(PreparedTile.serializer(), prepared))
        val feature = transported.features.single()
        assertEquals(ULong.MAX_VALUE.toString(), feature.id)
        assertEquals(properties, feature.properties.mapValues { it.value.toValue() })
        assertContentEquals(intArrayOf(4, 8), feature.coordinates)
        assertContentEquals(intArrayOf(0, 1), feature.parts)
    }
    test("multiple style layers share one geometry and one Compose feature instance") {
        val style = StyleResolver().resolve("""{"layers":[
            {"id":"fill","type":"fill","source-layer":"land"},
            {"id":"outline","type":"line","source-layer":"land"}
        ]}""").style!!
        val feature = MVTFeature(7UL, RawMVTGeomType.POLYGON,
            listOf(listOf(0 to 0, 10 to 0, 10 to 10, 0 to 10), listOf(3 to 3, 3 to 7, 7 to 7, 7 to 3)),
            mapOf("kind" to "park"))
        val tile = MVTile(listOf(MVTLayer("land", 4096, listOf(feature))))
        val prepared = TilePreparation(style.preparation!!).prepare(tile, 0, 0, 0)
        assertEquals(1, prepared.features.size)
        assertEquals(listOf(0), prepared.layerFeatures.getValue("fill"))
        assertEquals(listOf(0), prepared.layerFeatures.getValue("outline"))
        val converted = toOptimizedTile(TileSpecs(0, 0, 0), prepared).optimizedTile!!
        assertSame(converted.layerFeatures.getValue("fill").single(), converted.layerFeatures.getValue("outline").single())
        val original = optimizeMVTile(VectorTile(0, 0, 0, tile), style).optimizedTile!!
        for (id in listOf("fill", "outline")) {
            val oldFeature = original.layerFeatures.getValue(id).single()
            val newFeature = converted.layerFeatures.getValue(id).single()
            assertEquals(oldFeature.id, newFeature.id)
            assertEquals(oldFeature.properties, newFeature.properties)
            assertEquals(assertIs<OptimizedGeometry.Polygon>(oldFeature.geometry).path.getBounds(),
                assertIs<OptimizedGeometry.Polygon>(newFeature.geometry).path.getBounds())
        }
    }
    test("geometry context and integer tile zoom filters retain legacy membership") {
        val style = StyleResolver().resolve("""{"layers":[
            {"id":"places","type":"symbol","source-layer":"places",
             "filter":["all",[">=",["zoom"],5],["==",["id"],["get","id_copy"]],
              ["within",{"type":"Polygon","coordinates":[[[-180,-85],[180,-85],[180,85],[-180,85],[-180,-85]]]}]]}
        ]}""").style!!
        val tile = MVTile(listOf(MVTLayer("places", 4096, listOf(
            MVTFeature(ULong.MAX_VALUE, RawMVTGeomType.POINT, listOf(listOf(4 to 8)), mapOf("id_copy" to ULong.MAX_VALUE)),
        ))))
        for (zoom in listOf(4, 5, 9)) {
            val old = optimizeMVTile(VectorTile(zoom, 0, 0, tile), style).optimizedTile!!
            val prepared = TilePreparation(style.preparation!!).prepare(tile, zoom, 0, 0)
            assertEquals(old.layerFeatures.getValue("places").map { it.id },
                prepared.layerFeatures.getValue("places").map { prepared.features[it].id?.toULong() })
        }
    }
    test("each feature extent is retained and converted to one rendering extent") {
        val style = StyleResolver().resolve("""{"layers":[
            {"id":"a","type":"symbol","source-layer":"a"},{"id":"b","type":"symbol","source-layer":"b"}
        ]}""").style!!
        val tile = MVTile(listOf(
            MVTLayer("a", 4096, listOf(MVTFeature(null, RawMVTGeomType.POINT, listOf(listOf(2048 to 2048)), emptyMap()))),
            MVTLayer("b", 8192, listOf(MVTFeature(null, RawMVTGeomType.POINT, listOf(listOf(4096 to 4096)), emptyMap()))),
        ))
        val prepared = TilePreparation(style.preparation!!).prepare(tile, 0, 0, 0)
        assertEquals(listOf(4096, 8192), prepared.features.map { it.extent })
        val optimized = toOptimizedTile(TileSpecs(0, 0, 0), prepared).optimizedTile!!
        assertEquals(optimized.layerFeatures.getValue("a").single().geometry, optimized.layerFeatures.getValue("b").single().geometry)
    }
    test("manually decoded properties retain the compatibility contract") {
        val data = MVTile(listOf(MVTLayer("places", 4096, listOf(MVTFeature(
            1UL, RawMVTGeomType.POINT, listOf(listOf(4 to 8)), mapOf("custom" to listOf(1, 2, 3)),
        )))))
        val optimized = optimizeMVTile(VectorTile(0, 0, 0, data), pipelineStyle()).optimizedTile!!
        assertEquals(listOf(1, 2, 3), optimized.layerFeatures.getValue("places").single().properties["custom"])
    }
    test("real JVM workers prepare protobuf on a bounded owned thread pool and preserve input bytes") {
        val pool = PreparationPool()
        val input = pipelineBytes(ULong.MAX_VALUE)
        val original = input.copyOf()
        val style = pipelineStyle().preparation!!
        try {
            coroutineScope {
                (0..3).map { index ->
                    async {
                        pool.prepare(PreparationRequest(index.toLong(), 1, TileSpecs(0, 0, 0), 1, style, input))
                    }
                }.awaitAll().forEach { assertEquals(ULong.MAX_VALUE.toString(), it.features.single().id) }
            }
            assertContentEquals(original, input)
            assertEquals(2, Thread.getAllStackTraces().keys.count { it.name == "KMaP-tile-preparation" })
        } finally { pool.close() }
        withTimeout(5_000) {
            while (Thread.getAllStackTraces().keys.any { it.name == "KMaP-tile-preparation" }) delay(1)
        }
    }
    test("malformed protobuf produces a terminal failure without poisoning the JVM worker") {
        val worker = createPreparationWorker()
        val style = pipelineStyle().preparation!!
        try {
            assertFailsWith<Exception> {
                worker.prepare(PreparationRequest(1, 1, TileSpecs(0, 0, 0), 1, style, byteArrayOf(0x1a, 0xff.toByte())))
            }
            val result = worker.prepare(PreparationRequest(2, 1, TileSpecs(0, 0, 0), 1, style, pipelineBytes()))
            assertEquals("1", result.features.single().id)
        } finally { worker.close() }
    }
}
