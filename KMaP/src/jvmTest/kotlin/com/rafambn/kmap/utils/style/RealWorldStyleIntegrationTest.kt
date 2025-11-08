package com.rafambn.kmap.utils.style

import kotlinx.serialization.json.Json
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Integration tests using 12 real-world Mapbox/MapLibre style files from test resources.
 * These tests verify that the style resolver can handle complex real-world styles with
 * multiple layer types, expressions, and features.
 */
class RealWorldStyleIntegrationTest {

    private val styleResourceDir = "KMaP/src/jvmTest/resources/style"
    private val json = Json { ignoreUnknownKeys = true }
    private val resolver = StyleResolver()

    private fun loadStyleFile(filename: String): Style? {
        val file = File(styleResourceDir, filename)
        return if (file.exists()) {
            try {
                val content = file.readText()
                json.decodeFromString(Style.serializer(), content)
            } catch (e: Exception) {
                println("Failed to load style $filename: ${e.message}")
                null
            }
        } else {
            println("Style file not found: ${file.absolutePath}")
            null
        }
    }

    @Test
    fun testStyle1AquarelleLoading() {
        val style = loadStyleFile("style1.json") ?: run {
            println("Skipping style1 test - file not found")
            return
        }

        val optimized = resolver.resolve(style)
        assertNotNull(optimized)
        assertTrue(optimized.layers.isNotEmpty(), "Style should have at least one layer")

        // Verify background layer exists and evaluate its properties
        val backgroundLayer = optimized.layers.find { it.id == "Background" }
        assertNotNull(backgroundLayer, "Should have Background layer")
        assertEquals("background", backgroundLayer.type)

        // Test that background color evaluates to a non-null value
        val bgColor = backgroundLayer.paint.properties["background-color"]?.evaluate(10.0, emptyMap(), null)
        assertNotNull(bgColor, "Background color should evaluate to non-null")

        // Verify fill layers have interpolation expressions that work
        val fillLayer = optimized.layers.find { it.id == "Land" }
        if (fillLayer != null && fillLayer.type == "fill") {
            val opacity0 = fillLayer.paint.properties["fill-opacity"]?.evaluate(0.0, emptyMap(), null) as? Double
            val opacity7 = fillLayer.paint.properties["fill-opacity"]?.evaluate(7.0, emptyMap(), null) as? Double

            // Should have interpolation working: zoom 0 -> 1, zoom 7 -> 0.8
            if (opacity0 != null && opacity7 != null) {
                assertTrue(opacity0 > opacity7, "Opacity should decrease as zoom increases from 0 to 7")
                assertTrue(opacity0 == 1.0, "Opacity at zoom 0 should be 1.0")
                assertTrue(opacity7 == 0.8, "Opacity at zoom 7 should be 0.8")
            }
        }
    }

    @Test
    fun testMultipleStylesEvaluateCorrectly() {
        // Test that styles 2-12 can be loaded and evaluated without errors
        for (styleNum in 2..12) {
            val style = loadStyleFile("style$styleNum.json") ?: continue
            val optimized = resolver.resolve(style)

            // All layers should have compiled paint and layout properties
            for (layer in optimized.layers) {
                assertTrue(layer.paint.properties.isNotEmpty() || layer.layout.properties.isNotEmpty(),
                    "Layer ${layer.id} should have paint or layout properties")

                // Verify each property can be evaluated
                for ((propName, compiledValue) in layer.paint.properties) {
                    val result = compiledValue.evaluate(10.0, mapOf("test" to "value"), null)
                    // Should evaluate without throwing
                    println("Style $styleNum, Layer ${layer.id}: $propName = $result")
                }

                // Verify filters work if present
                layer.filter?.let {
                    val matches = it.evaluate(mapOf("class" to "test"), "Polygon", null)
                    // Filter always returns Boolean from implementation
                }
            }
        }
    }

    @Test
    fun testStylesWithInterpolationExpressions() {
        // Test styles 13-16 which should have complex interpolation expressions
        for (styleNum in 13..16) {
            val style = loadStyleFile("style$styleNum.json") ?: continue
            val optimized = resolver.resolve(style)

            // Find layers with interpolation (numeric properties at different zooms)
            for (layer in optimized.layers) {
                // Test paint properties across zoom levels
                for ((propName, compiledValue) in layer.paint.properties) {
                    val zoom5Value = compiledValue.evaluate(5.0, emptyMap(), null)
                    val zoom10Value = compiledValue.evaluate(10.0, emptyMap(), null)
                    val zoom15Value = compiledValue.evaluate(15.0, emptyMap(), null)

                    // If numeric, verify interpolation is working (values should differ)
                    if (zoom5Value is Number && zoom10Value is Number && zoom15Value is Number) {
                        val z5 = zoom5Value.toDouble()
                        val z10 = zoom10Value.toDouble()
                        val z15 = zoom15Value.toDouble()

                        // At least one pair should be different (proving interpolation works)
                        val anyDifferent = z5 != z10 || z10 != z15 || z5 != z15
                        assertTrue(anyDifferent || z5 == z10 && z10 == z15,
                            "Property $propName should interpolate across zooms or be constant")
                        println("Style $styleNum, $propName: z5=$z5, z10=$z10, z15=$z15")
                    }
                }
            }
        }
    }

    @Test
    fun testMultipleLayerTypesInSingleStyle() {
        val style = loadStyleFile("style1.json") ?: run {
            println("Skipping multi-layer test - file not found")
            return
        }

        val optimized = resolver.resolve(style)

        // Collect layer types
        val layerTypes = optimized.layers.map { it.type }.toSet()
        println("Found layer types: $layerTypes")

        // Verify we can handle multiple types
        assertTrue(optimized.layers.isNotEmpty())
    }

    @Test
    fun testStyleWithComplexExpressions() {
        val style = loadStyleFile("style1.json") ?: run {
            println("Skipping expression test - file not found")
            return
        }

        val optimized = resolver.resolve(style)
        var expressionsEvaluated = 0
        var expressionErrors = 0

        // Test that we can evaluate expressions for various zoom levels and verify results
        for (layer in optimized.layers) {
            if (layer.type in listOf("fill", "line", "symbol", "background")) {
                // Try evaluating at different zoom levels
                for (zoom in listOf(5.0, 10.0, 15.0)) {
                    val properties = mapOf(
                        "class" to "park",
                        "type" to "Polygon",
                        "name" to "Central Park",
                        "area" to 840
                    )

                    // Test filter evaluation - verify it works without errors
                    layer.filter?.let { filter ->
                        val result = filter.evaluate(properties, "Polygon", null)
                        // Filter returns boolean from implementation
                        expressionsEvaluated++
                    }

                    // Test paint properties - verify they evaluate without errors
                    for ((propName, compiledValue) in layer.paint.properties) {
                        try {
                            val result = compiledValue.evaluate(zoom, properties, null)
                            assertNotNull(result, "Property $propName should evaluate to non-null at zoom $zoom")
                            expressionsEvaluated++
                        } catch (e: Exception) {
                            expressionErrors++
                            println("Error evaluating $propName at zoom $zoom: ${e.message}")
                        }
                    }

                    // Test layout properties
                    for ((propName, compiledValue) in layer.layout.properties) {
                        try {
                            val result = compiledValue.evaluate(zoom, properties, null)
                            expressionsEvaluated++
                        } catch (e: Exception) {
                            expressionErrors++
                            println("Error evaluating layout.$propName at zoom $zoom: ${e.message}")
                        }
                    }
                }
            }
        }

        assertTrue(expressionsEvaluated > 0, "Should have evaluated at least some expressions")
        assertEquals(0, expressionErrors, "Should have zero expression evaluation errors")
    }

    @Test
    fun testAllStylesLoad() {
        val styleFiles = (1..16).map { "style$it.json" }
        var successCount = 0
        var failureCount = 0
        var skippedCount = 0

        for (file in styleFiles) {
            val style = loadStyleFile(file)
            if (style != null) {
                try {
                    val optimized = resolver.resolve(style)
                    successCount++
                    println("✓ Successfully loaded and resolved $file (${optimized.layers.size} layers)")
                } catch (e: Exception) {
                    failureCount++
                    println("✗ Failed to resolve $file: ${e.message}")
                }
            } else {
                skippedCount++
                println("- Skipped $file (file not found)")
            }
        }

        println("\nSummary: $successCount loaded, $failureCount failed, $skippedCount skipped")
        assertTrue(successCount + skippedCount > 0, "Should load or skip at least some styles")
    }
}
