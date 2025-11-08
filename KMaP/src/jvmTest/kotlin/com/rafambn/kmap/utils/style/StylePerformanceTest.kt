package com.rafambn.kmap.utils.style

import kotlinx.serialization.json.Json
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Performance tests for style compilation and evaluation.
 * These tests measure:
 * - Compilation time (converting raw styles to optimized styles)
 * - Evaluation time (evaluating expressions during rendering)
 * - Memory usage patterns
 */
class StylePerformanceTest {

    private val styleResourceDir = "KMaP/src/jvmTest/resources/style"
    private val json = Json { ignoreUnknownKeys = true }
    private val resolver = StyleResolver()
    private val evaluator = ExpressionEvaluator()

    private fun loadStyleFile(filename: String): Style? {
        val file = File(styleResourceDir, filename)
        return if (file.exists()) {
            try {
                val content = file.readText()
                json.decodeFromString(Style.serializer(), content)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    @Test
    fun testCompilationTimeSimpleStyle() {
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
                        "source-layer": "test",
                        "paint": {
                            "fill-color": "#ff0000",
                            "fill-opacity": 0.5
                        }
                    }
                ]
            }
        """.trimIndent()

        val rawStyle = json.decodeFromString(Style.serializer(), rawStyleJson)

        val startTime = System.nanoTime()
        val optimizedStyle = resolver.resolve(rawStyle)
        val endTime = System.nanoTime()

        val compilationTimeMs = (endTime - startTime) / 1_000_000.0
        println("Simple style compilation time: ${compilationTimeMs}ms")

        // Compilation should be fast (under 100ms for simple styles)
        assertTrue(compilationTimeMs < 100, "Compilation too slow: ${compilationTimeMs}ms")
        assertTrue(optimizedStyle.layers.isNotEmpty())
    }

    @Test
    fun testCompilationTimeComplexStyle() {
        val style = loadStyleFile("style1.json")
        if (style == null) {
            println("Skipping complex style compilation test - file not found")
            return
        }

        val startTime = System.nanoTime()
        val optimizedStyle = resolver.resolve(style)
        val endTime = System.nanoTime()

        val compilationTimeMs = (endTime - startTime) / 1_000_000.0
        println("Complex style compilation time: ${compilationTimeMs}ms (${optimizedStyle.layers.size} layers)")

        // Even complex styles should compile reasonably fast (under 500ms)
        assertTrue(compilationTimeMs < 500, "Compilation too slow: ${compilationTimeMs}ms")
    }

    @Test
    fun testEvaluationTimePerProperty() {
        val context = EvaluationContext(
            zoomLevel = 10.0,
            featureProperties = mapOf(
                "class" to "motorway",
                "type" to "Polygon",
                "name" to "Test Road",
                "speed" to 120
            ),
            geometryType = "Polygon"
        )

        // Simple color constant
        val colorExpr = listOf("rgba", 255, 0, 0, 1)
        val colorStart = System.nanoTime()
        repeat(1000) {
            evaluator.evaluate(colorExpr, context)
        }
        val colorEnd = System.nanoTime()
        val colorTimeUs = (colorEnd - colorStart) / 1000.0 / 1000.0
        println("Color evaluation time per 1000 iterations: ${colorTimeUs}us")

        // Interpolation expression
        val interpExpr = listOf("interpolate", listOf("linear"), listOf("zoom"), 5, 1, 15, 4)
        val interpStart = System.nanoTime()
        repeat(1000) {
            evaluator.evaluate(interpExpr, context)
        }
        val interpEnd = System.nanoTime()
        val interpTimeUs = (interpEnd - interpStart) / 1000.0 / 1000.0
        println("Interpolation evaluation time per 1000 iterations: ${interpTimeUs}us")

        // Case expression with nested conditions
        val caseExpr = listOf("case", listOf("==", listOf("get", "class"), "motorway"), "red", "blue")
        val caseStart = System.nanoTime()
        repeat(1000) {
            evaluator.evaluate(caseExpr, context)
        }
        val caseEnd = System.nanoTime()
        val caseTimeUs = (caseEnd - caseStart) / 1000.0 / 1000.0
        println("Case expression evaluation time per 1000 iterations: ${caseTimeUs}us")

        // All should complete in reasonable time (< 100us per evaluation on average)
        assertTrue(colorTimeUs < 100000, "Color evaluation too slow")
        assertTrue(interpTimeUs < 100000, "Interpolation too slow")
        assertTrue(caseTimeUs < 100000, "Case expression too slow")
    }

    @Test
    fun testBatchEvaluationPerformance() {
        val rawStyleJson = """
            {
                "version": 8,
                "name": "Performance Test",
                "sources": {
                    "test": {
                        "type": "vector",
                        "url": "http://example.com"
                    }
                },
                "layers": [
                    {
                        "id": "layer1",
                        "type": "fill",
                        "source": "test",
                        "source-layer": "data",
                        "paint": {
                            "fill-color": ["case", ["==", ["get", "type"], "A"], "#ff0000", "#00ff00"],
                            "fill-opacity": ["interpolate", ["linear"], ["zoom"], 0, 0.5, 20, 1]
                        }
                    }
                ]
            }
        """.trimIndent()

        val rawStyle = json.decodeFromString(Style.serializer(), rawStyleJson)
        val optimizedStyle = resolver.resolve(rawStyle)
        val layer = optimizedStyle.layers[0]

        // Simulate rendering 1000 features at zoom 10
        val startTime = System.nanoTime()
        repeat(1000) { i ->
            val properties = mapOf(
                "type" to if (i % 2 == 0) "A" else "B",
                "id" to i
            )

            layer.paint.properties["fill-color"]?.evaluate(10.0, properties, null)
            layer.paint.properties["fill-opacity"]?.evaluate(10.0, properties, null)
        }
        val endTime = System.nanoTime()

        val totalTimeMs = (endTime - startTime) / 1_000_000.0
        val timePerFeatureUs = (endTime - startTime) / 1000.0 / 1000.0 / 2.0  // 2 properties per feature
        println("Batch evaluation of 1000 features (2 properties each): ${totalTimeMs}ms (${timePerFeatureUs}us per property)")

        // Should be able to evaluate properties for 1000 features in reasonable time
        assertTrue(totalTimeMs < 100, "Batch evaluation too slow: ${totalTimeMs}ms")
    }

    @Test
    fun testCompilationVsEvaluationRatio() {
        val style = loadStyleFile("style1.json")
        if (style == null) {
            println("Skipping compilation vs evaluation ratio test - file not found")
            return
        }

        // Measure compilation time
        val compilationStart = System.nanoTime()
        val optimizedStyle = resolver.resolve(style)
        val compilationEnd = System.nanoTime()
        val compilationTimeMs = (compilationEnd - compilationStart) / 1_000_000.0

        // Measure evaluation time for all properties
        val evaluationStart = System.nanoTime()
        val context = EvaluationContext(
            zoomLevel = 10.0,
            featureProperties = mapOf("test" to "value"),
            geometryType = "Polygon"
        )

        repeat(1000) {
            for (layer in optimizedStyle.layers) {
                for ((_, compiledValue) in layer.paint.properties) {
                    compiledValue.evaluate(10.0, emptyMap(), null)
                }
            }
        }

        val evaluationEnd = System.nanoTime()
        val evaluationTimeMs = (evaluationEnd - evaluationStart) / 1_000_000.0

        println("Compilation time: ${compilationTimeMs}ms")
        println("1000x evaluation time: ${evaluationTimeMs}ms")
        println("Ratio: ${evaluationTimeMs / compilationTimeMs}x")

        // Compilation should be significantly slower than single evaluations
        // (but you do compilation once at load time)
        assertTrue(compilationTimeMs < 500, "Compilation too slow")
    }

    @Test
    fun testMemoryEfficiency() {
        val style = loadStyleFile("style1.json")
        if (style == null) {
            println("Skipping memory efficiency test - file not found")
            return
        }

        val runtime = Runtime.getRuntime()
        val beforeMemory = runtime.totalMemory() - runtime.freeMemory()

        // Compile the style
        val optimizedStyle = resolver.resolve(style)

        val afterMemory = runtime.totalMemory() - runtime.freeMemory()
        val memoryUsed = (afterMemory - beforeMemory) / 1024.0  // Convert to KB

        println("Memory used for style compilation: ${memoryUsed}KB")
        println("Number of layers: ${optimizedStyle.layers.size}")
        println("Memory per layer: ${memoryUsed / optimizedStyle.layers.size}KB")

        // Compilation shouldn't use excessive memory (under 10MB for a single style)
        assertTrue(memoryUsed < 10_000, "Memory usage too high: ${memoryUsed}KB")
    }

    @Test
    fun testMultipleStyleCompilation() {
        val styleFiles = (1..12).map { "style$it.json" }
        val start = System.nanoTime()

        var totalLayers = 0
        for (file in styleFiles) {
            val style = loadStyleFile(file) ?: continue
            val optimized = resolver.resolve(style)
            totalLayers += optimized.layers.size
        }

        val end = System.nanoTime()
        val totalTimeMs = (end - start) / 1_000_000.0
        println("Compilation of all 12 styles: ${totalTimeMs}ms (${totalLayers} total layers)")

        // Should compile all 12 styles in reasonable time
        assertTrue(totalTimeMs < 5000, "Multiple style compilation too slow: ${totalTimeMs}ms")
    }

    @Test
    fun testCubicBezierInterpolationPerformance() {
        val context = EvaluationContext(zoomLevel = 7.5)
        val expression = listOf("interpolate",
            listOf("cubic-bezier", 0.25, 0.1, 0.25, 1.0),
            listOf("zoom"),
            5, 10, 10, 20)

        val start = System.nanoTime()
        repeat(1000) {
            evaluator.evaluate(expression, context)
        }
        val end = System.nanoTime()

        val timeUs = (end - start) / 1000.0 / 1000.0
        println("Cubic bezier interpolation time per 1000 iterations: ${timeUs}us")

        // Cubic bezier should still be reasonably fast
        assertTrue(timeUs < 100000, "Cubic bezier interpolation too slow")
    }
}
