package com.rafambn.kmap.style

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import com.rafambn.kmap.style.evaluation.EvaluationContext
import com.rafambn.kmap.style.evaluation.ExpressionEvaluator
import com.rafambn.kmap.style.evaluation.FeatureGeometryContext
import de.infix.testBalloon.framework.core.testSuite
import kotlin.math.E
import kotlin.math.PI
import kotlin.math.ln
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

val ExpandedExpressionTest by testSuite {
    val evaluator = ExpressionEvaluator()
    val context = EvaluationContext()

    test("trigonometric operators use radians and compose with other expressions") {
        assertEquals(1.0, evaluator.evaluate(listOf("cos", 0), context) as Double, 1e-12)
        assertEquals(1.0, evaluator.evaluate(listOf("sin", listOf("/", listOf("pi"), 2)), context) as Double, 1e-12)
        assertEquals(1.0, evaluator.evaluate(listOf("tan", listOf("/", listOf("pi"), 4)), context) as Double, 1e-12)
        assertEquals(PI, evaluator.evaluate(listOf("acos", -1), context) as Double, 1e-12)
        assertEquals(PI / 2, evaluator.evaluate(listOf("asin", 1), context) as Double, 1e-12)
        assertEquals(PI / 4, evaluator.evaluate(listOf("atan", 1), context) as Double, 1e-12)
        assertNull(evaluator.evaluate(listOf("acos", 2), context))
        assertNull(evaluator.evaluate(listOf("sin", "1"), context))
        assertNull(evaluator.evaluate(listOf("cos", 0, 1), context))
    }

    test("constants and logarithms enforce their arity and domain") {
        assertEquals(E, evaluator.evaluate(listOf("e"), context))
        assertEquals(PI, evaluator.evaluate(listOf("pi"), context))
        assertEquals(ln(2.0), evaluator.evaluate(listOf("ln2"), context))
        assertEquals(1.0, evaluator.evaluate(listOf("ln", listOf("e")), context) as Double, 1e-12)
        assertEquals(3.0, evaluator.evaluate(listOf("log10", 1000), context) as Double, 1e-12)
        assertEquals(4.0, evaluator.evaluate(listOf("log2", 16), context) as Double, 1e-12)
        assertNull(evaluator.evaluate(listOf("pi", 1), context))
        assertNull(evaluator.evaluate(listOf("ln", 0), context))
        assertNull(evaluator.evaluate(listOf("log2", -1), context))
    }

    test("type assertions and conversions keep types distinct") {
        assertEquals(false, evaluator.evaluate(listOf("boolean", 1, false), context))
        assertEquals(4, evaluator.evaluate(listOf("number", "4", 4), context))
        assertEquals("#ff0000", evaluator.evaluate(listOf("string", "#ff0000"), context))
        assertEquals(mapOf("key" to 1), evaluator.evaluate(listOf("object", mapOf("key" to 1)), context))
        assertEquals(listOf(1, 2), evaluator.evaluate(listOf("array", "number", 2, listOf("literal", listOf(1, 2))), context))
        assertNull(evaluator.evaluate(listOf("array", "string", 2, listOf("literal", listOf(1, 2))), context))
        assertEquals(0.0, evaluator.evaluate(listOf("to-number", null), context))
        assertEquals(1.0, evaluator.evaluate(listOf("to-number", true), context))
        assertEquals(12.5, evaluator.evaluate(listOf("to-number", "bad", "12.5"), context))
        assertEquals(false, evaluator.evaluate(listOf("to-boolean", 0), context))
        assertEquals(true, evaluator.evaluate(listOf("to-boolean", "false"), context))
        assertEquals(Color.Red, evaluator.evaluate(listOf("to-color", "invalid", "#ff0000"), context))
        assertEquals(listOf("a", "b", "c"), evaluator.evaluate(listOf("split", "a,b,c", ","), context))
        val colorProperty = context.copy(featureProperties = mapOf("hex" to "#ff0000"))
        assertEquals("#ff0000", evaluator.evaluate(listOf("get", "hex"), colorProperty))
        assertEquals("#ff0000", evaluator.evaluate(listOf("string", listOf("get", "hex")), colorProperty))
        assertEquals(true, evaluator.evaluate(listOf("==", listOf("get", "hex"), "#ff0000"), colorProperty))
        assertEquals(Color.Red, evaluator.evaluate(listOf("to-color", listOf("get", "hex")), colorProperty))
        assertNull(evaluator.evaluate(listOf("array", "number", 1.5, listOf("literal", listOf(1, 2))), context))
    }

    test("additional math operators handle sign and invalid domains") {
        assertEquals(-5.0, evaluator.evaluate(listOf("-", 5), context))
        assertEquals(-3.0, evaluator.evaluate(listOf("-", 5, 8), context))
        assertEquals(-1.0, evaluator.evaluate(listOf("%", -5, 2), context))
        assertEquals(8.0, evaluator.evaluate(listOf("^", 2, 3), context))
        assertEquals(3.0, evaluator.evaluate(listOf("abs", -3), context))
        assertEquals(3.0, evaluator.evaluate(listOf("ceil", 2.1), context))
        assertEquals(2.0, evaluator.evaluate(listOf("floor", 2.9), context))
        assertEquals(-2.0, evaluator.evaluate(listOf("round", -1.5), context))
        assertEquals(5.0, evaluator.evaluate(listOf("max", 1, 5, 2), context))
        assertEquals(1.0, evaluator.evaluate(listOf("min", 1, 5, 2), context))
        assertEquals(3.0, evaluator.evaluate(listOf("sqrt", 9), context))
        assertNull(evaluator.evaluate(listOf("%", 5, 0), context))
        assertNull(evaluator.evaluate(listOf("sqrt", -1), context))
    }

    test("arithmetic requires numeric operands and valid argument counts") {
        assertEquals(5.0, evaluator.evaluate(listOf("+", 2, 3), context))
        assertEquals(5.0, evaluator.evaluate(listOf("+", listOf("to-number", "3"), 2), context))
        assertEquals(-3.0, evaluator.evaluate(listOf("-", 2, 5), context))
        assertEquals(4.0, evaluator.evaluate(listOf("/", 8, 2), context))
        assertNull(evaluator.evaluate(listOf("+", "3", 2), context))
        assertNull(evaluator.evaluate(listOf("-", "3"), context))
        assertNull(evaluator.evaluate(listOf("*", 2, "3"), context))
        assertNull(evaluator.evaluate(listOf("/", 8, "2"), context))
        assertNull(evaluator.evaluate(listOf("-", 10, 3, 2), context))
        assertNull(evaluator.evaluate(listOf("/", 20, 2, 2), context))
    }

    test("numeric feature values compare across MVT and JSON number types") {
        val feature = context.copy(featureProperties = mapOf("rank" to 2L, "opacity" to 0.5f))
        assertEquals(true, evaluator.evaluate(listOf("==", listOf("get", "rank"), 2.0), feature))
        assertEquals(false, evaluator.evaluate(listOf("!=", listOf("get", "opacity"), 0.5), feature))
        assertEquals("selected", evaluator.evaluate(listOf("match", listOf("get", "rank"), 2.0, "selected", "other"), feature))
        assertEquals(true, evaluator.evaluate(listOf("in", listOf("get", "rank"), listOf("literal", listOf(1.0, 2.0))), feature))
        assertEquals(1, evaluator.evaluate(listOf("index-of", 2.0, listOf("literal", listOf(1L, 2L))), feature))
    }

    test("get and has evaluate calculated keys and nested objects") {
        val feature = context.copy(featureProperties = mapOf("key" to "rank", "rank" to 2L, "nested" to mapOf("rank" to 3), "{name}" to "literal"))
        assertEquals(2L, evaluator.evaluate(listOf("get", listOf("get", "key")), feature))
        assertEquals(3, evaluator.evaluate(listOf("get", "rank", listOf("get", "nested")), feature))
        assertEquals(true, evaluator.evaluate(listOf("has", listOf("get", "key"), listOf("get", "nested")), feature))
        assertEquals(false, evaluator.evaluate(listOf("has", "missing", listOf("get", "nested")), feature))
        assertEquals("literal", evaluator.evaluate(listOf("get", "{name}"), feature))
    }

    test("index-of honors the start index and slice stays within bounds") {
        assertEquals(3, evaluator.evaluate(listOf("index-of", "a", "banana", 3), context))
        assertEquals(1, evaluator.evaluate(listOf("index-of", "a", "banana", -2), context))
        assertEquals(2, evaluator.evaluate(listOf("index-of", 3, listOf("literal", listOf(1, 2, 3)), -1), context))
        assertEquals("bc", evaluator.evaluate(listOf("slice", "abc", 1, 10), context))
        assertEquals("bc", evaluator.evaluate(listOf("slice", "abc", -2), context))
        assertEquals("", evaluator.evaluate(listOf("slice", "abc", 3, 1), context))
        assertEquals(listOf(2, 3), evaluator.evaluate(listOf("slice", listOf("literal", listOf(1, 2, 3)), -2, 10), context))
    }

    test("interpolate blends every numeric array component") {
        val expression = listOf("interpolate", listOf("linear"), listOf("zoom"),
            0, listOf("literal", listOf(0, 0)), 10, listOf("literal", listOf(10, 20)))
        assertEquals(listOf(5.0, 10.0), evaluator.evaluate(expression, context.copy(zoomLevel = 5.0)))
        assertEquals(listOf(0, 0), evaluator.evaluate(expression, context.copy(zoomLevel = 0.0)))
    }

    test("image resolves an available sprite and coalesce skips missing names") {
        val fallback = ImageBitmap(2, 2)
        val spriteContext = context.copy(sprites = mapOf("dot" to fallback))
        assertNull(evaluator.evaluate(listOf("image", "missing"), spriteContext))
        assertSame(fallback, evaluator.evaluate(listOf("coalesce", listOf("image", "missing"), listOf("image", "dot")), spriteContext))
    }

    test("number-format applies decimal options") {
        assertEquals("1,234.5", evaluator.evaluate(listOf("number-format", 1234.5, emptyMap<String, Any>()), context))
        assertEquals("R$\u00a01.234,50", evaluator.evaluate(
            listOf("number-format", 1234.5, mapOf("locale" to "pt-BR", "currency" to "BRL")), context
        ))
        assertEquals("-$1,234.50", evaluator.evaluate(
            listOf("number-format", -1234.5, mapOf("locale" to "en-US", "currency" to "USD")), context
        ))
    }

    test("within handles points, line crossings, and polygon holes") {
        fun ring(west: Int, south: Int, east: Int, north: Int) = listOf(
            listOf(west, south), listOf(east, south), listOf(east, north), listOf(west, north), listOf(west, south)
        )
        val polygon = mapOf("type" to "Polygon", "coordinates" to listOf(ring(-20, -20, 20, 20)))
        val withHole = mapOf("type" to "Polygon", "coordinates" to listOf(ring(-20, -20, 20, 20), ring(-5, -5, 5, 5)))
        fun geometry(paths: List<List<Pair<Int, Int>>>) = FeatureGeometryContext(paths, 0, 0, 0, 4096)
        assertTrue(evaluator.evaluate(listOf("within", polygon), context.copy(
            geometryType = "Point", featureGeometry = geometry(listOf(listOf(2048 to 2048)))
        )) as Boolean)
        assertFalse(evaluator.evaluate(listOf("within", polygon), context.copy(
            geometryType = "Point", featureGeometry = geometry(listOf(listOf(100 to 100)))
        )) as Boolean)
        assertFalse(evaluator.evaluate(listOf("within", withHole), context.copy(
            geometryType = "Point", featureGeometry = geometry(listOf(listOf(2048 to 2048)))
        )) as Boolean)
        assertFalse(evaluator.evaluate(listOf("within", withHole), context.copy(
            geometryType = "LineString", featureGeometry = geometry(listOf(listOf(1960 to 2048, 2140 to 2048)))
        )) as Boolean)
    }

    test("color component conversion and Lab interpolation keep meaningful colors") {
        val rgba = evaluator.evaluate(listOf("to-rgba", "#ff000080"), context) as List<*>
        assertEquals(255.0, rgba[0] as Double, 0.01)
        assertEquals(0.0, rgba[1] as Double, 0.01)
        assertEquals(0.5, rgba[3] as Double, 0.01)
        val hsla = evaluator.evaluate(listOf("to-hsla", "#ff0000"), context) as List<*>
        assertEquals(0.0, hsla[0] as Double, 0.01)
        assertEquals(100.0, hsla[1] as Double, 0.01)
        assertEquals(50.0, hsla[2] as Double, 0.01)
        val lab = evaluator.evaluate(
            listOf("interpolate-lab", listOf("linear"), 5, 0, "#ff0000", 10, "#00ff00"), context
        ) as Color
        assertTrue(lab.red > 0.5f && lab.green > 0.5f)
        assertEquals(Color.Red, evaluator.evaluate(
            listOf("interpolate-hcl", listOf("linear"), -1, 0, "#ff0000", 10, "#00ff00"), context
        ))
    }
}
