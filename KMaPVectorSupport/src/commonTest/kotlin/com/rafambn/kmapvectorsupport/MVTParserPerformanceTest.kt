package com.rafambn.kmapvectorsupport

import com.rafambn.kmapvectorsupport.tileSpec.*
import kotlin.test.*
import kotlin.time.measureTime

class MVTParserPerformanceTest {

    private fun createLargeTestTile(layerCount: Int = 5, featuresPerLayer: Int = 1000): MVTile {
        val layers = (1..layerCount).map { layerIndex ->
            val features = (1..featuresPerLayer).map { featureIndex ->
                Feature(
                    id = featureIndex.toLong(),
                    type = GeomType.POINT,
                    geometry = listOf(
                        (CMD_MOVETO or (1 shl 3)),
                        featureIndex * 2, featureIndex * 4
                    ),
                    tags = listOf(0, 0)
                )
            }

            Layer(
                name = "layer_$layerIndex",
                extent = 4096,
                keys = listOf("name", "type", "id"),
                values = listOf(
                    Value(string_value = "feature_name"),
                    Value(string_value = "point"),
                    Value(int_value = 123L)
                ),
                features = features
            )
        }

        return MVTile(layers = layers)
    }

    private fun createComplexGeometryFeature(pointCount: Int): Feature {
        val geometry = mutableListOf<Int>()

        geometry.add(CMD_MOVETO or (1 shl 3))
        geometry.add(2)
        geometry.add(4)

        if (pointCount > 1) {
            geometry.add(CMD_LINETO or ((pointCount - 1) shl 3))
            for (i in 1 until pointCount) {
                geometry.add(2)
                geometry.add(2)
            }
        }

        return Feature(
            id = 1L,
            type = GeomType.LINESTRING,
            geometry = geometry,
            tags = listOf(0, 0)
        )
    }

    @Test
    fun benchmarkParseMVTPerformance() {
        println("\n=== MVT Parsing Performance Benchmark ===")

        val testSizes = listOf(
            Triple(1, 100, "Small"),
            Triple(3, 500, "Medium"),
            Triple(5, 1000, "Large"),
            Triple(10, 2000, "Extra Large")
        )

        testSizes.forEach { (layers, features, size) ->
            val testTile = createLargeTestTile(layers, features)

            val parseTime = measureTime {
                repeat(10) {
                    parseMVT(testTile)
                }
            }

            val avgTime = parseTime / 10
            val totalFeatures = layers * features

            println("$size tile ($layers layers, $totalFeatures features): ${avgTime.inWholeMicroseconds}ms avg")
        }
    }

    @Test
    fun benchmarkGeometryDecodingPerformance() {
        println("\n=== Geometry Decoding Performance Benchmark ===")

        val pointCounts = listOf(10, 50, 100, 500, 1000)

        pointCounts.forEach { pointCount ->
            val feature = createComplexGeometryFeature(pointCount)

            val decodeTime = measureTime {
                repeat(100) {
                    decodeFeatureGeometry(feature)
                }
            }

            val avgTime = decodeTime / 100
            println("$pointCount points: ${avgTime.inWholeMicroseconds}μs avg")
        }
    }

    @Test
    fun benchmarkPropertyResolutionPerformance() {
        println("\n=== Property Resolution Performance Benchmark ===")

        val propertyCounts = listOf(5, 20, 50, 100)

        propertyCounts.forEach { propCount ->
            val tags = (0 until propCount * 2).toList()
            val keys = (0 until propCount).map { "key_$it" }
            val values = (0 until propCount).map { Value(string_value = "value_$it") }

            val feature = Feature(tags = tags)
            val layer = Layer(name = "test", keys = keys, values = values)

            val resolveTime = measureTime {
                repeat(1000) {
                    resolveFeatureProperties(feature, layer)
                }
            }

            val avgTime = resolveTime / 1000
            println("$propCount properties: ${avgTime.inWholeMicroseconds}μs avg")
        }
    }

    @Test
    fun benchmarkZigZagDecodingPerformance() {
        println("\n=== ZigZag Decoding Performance Benchmark ===")

        val testValues = listOf(
            0, 1, 2, 3, 4, 5, 10, 15, 29, 30, 31, 100, 1000, 10000
        )

        val decodeTime = measureTime {
            repeat(100000) {
                testValues.forEach { value ->
                    decodeZigZag(value)
                }
            }
        }

        val avgTimePerValue = decodeTime / (100000 * testValues.size)
        println("ZigZag decoding: ${avgTimePerValue.inWholeNanoseconds}ns avg per value")
    }

    @Test
    fun benchmarkEndToEndPerformance() {
        println("\n=== End-to-End Performance Benchmark ===")

        val testTile = createLargeTestTile(layerCount = 8, featuresPerLayer = 1500)

        val fullParseTime = measureTime {
            repeat(5) {
                val parsed = parseMVT(testTile)

                var totalFeatures = 0
                var totalCoordinates = 0
                parsed.layers.forEach { layer ->
                    totalFeatures += layer.features.size
                    layer.features.forEach { feature ->
                        totalCoordinates += feature.geometry.sumOf { it.size }
                    }
                }

                require(totalFeatures > 0) { "No features parsed" }
                require(totalCoordinates > 0) { "No coordinates parsed" }
            }
        }

        val avgFullTime = fullParseTime / 5
        val totalFeatures = 8 * 1500

        println("Full parsing pipeline ($totalFeatures features): ${avgFullTime.inWholeMilliseconds}ms avg")
        val featuresPerSecond = if (avgFullTime.inWholeMilliseconds > 0) {
            (totalFeatures * 1000.0) / avgFullTime.inWholeMilliseconds
        } else {
            Double.POSITIVE_INFINITY
        }
        println("Performance: ${featuresPerSecond.toInt()} features/second")
    }

    @Test
    fun memoryUsageEstimation() {
        println("\n=== Memory Usage Estimation ===")

        val testTile = createLargeTestTile(layerCount = 3, featuresPerLayer = 1000)
        val parsed = parseMVT(testTile)

        var totalFeatures = 0
        var totalCoordinates = 0
        var totalProperties = 0

        parsed.layers.forEach { layer ->
            totalFeatures += layer.features.size
            layer.features.forEach { feature ->
                totalCoordinates += feature.geometry.sumOf { it.size }
                totalProperties += feature.properties.size
            }
        }

        val coordinateMemory = totalCoordinates * 8
        val propertyMemory = totalProperties * 32
        val featureOverhead = totalFeatures * 64

        val totalEstimatedMemory = coordinateMemory + propertyMemory + featureOverhead

        println("Estimated memory usage:")
        println("  Features: $totalFeatures")
        println("  Coordinates: $totalCoordinates")
        println("  Properties: $totalProperties")
        println("  Estimated total memory: ${totalEstimatedMemory / 1024}KB")
    }
}
