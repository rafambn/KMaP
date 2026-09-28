package com.rafambn.kmap.style

import com.rafambn.kmap.style.compiled.CompiledBackgroundLayer
import com.rafambn.kmap.style.compiled.CompiledFillLayer
import com.rafambn.kmap.style.compiled.CompiledLineLayer
import de.infix.testBalloon.framework.core.testSuite
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

val RealWorldStyleIntegrationTest by testSuite {
    val styleDirectory = File("testResources@jvm/style")
    val resolver = StyleResolver()

    test("loads bundled Mapbox and MapLibre styles") {
        var loaded = 0
        for (number in 1..16) {
            val file = File(styleDirectory, "style$number.json")
            if (!file.exists()) continue
            val result = resolver.resolve(file.readText())
            assertNotNull(result.style, "${file.name}: ${result.issues}")
            loaded++
        }
        assertTrue(loaded > 0)
    }

    test("evaluates typed properties in a real style") {
        val file = File(styleDirectory, "style1.json")
        val style = assertNotNull(resolver.resolve(file.readText()).style)
        val background = style.layers.filterIsInstance<CompiledBackgroundLayer>().single { it.id == "Background" }
        val land = style.layers.filterIsInstance<CompiledFillLayer>().single { it.id == "Land" }
        val line = style.layers.filterIsInstance<CompiledLineLayer>().single { it.id == "Water pattern outline" }

        assertNotNull(background.color?.evaluate(10.0, emptyMap(), null))
        assertEquals(1.0, background.opacity?.evaluate(10.0, emptyMap(), null))
        assertEquals(1.0, land.opacity?.evaluate(0.0, emptyMap(), null))
        assertEquals(0.8, land.opacity?.evaluate(7.0, emptyMap(), null))
        assertEquals(5.0, line.width?.evaluate(13.0, emptyMap(), null))
        assertEquals(20.0, line.width?.evaluate(18.0, emptyMap(), null))
    }
}
