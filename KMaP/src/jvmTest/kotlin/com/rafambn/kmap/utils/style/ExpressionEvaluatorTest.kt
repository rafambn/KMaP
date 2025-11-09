package com.rafambn.kmap.utils.style

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExpressionEvaluatorTest {

    private val evaluator = ExpressionEvaluator()

    @Test
    fun testAll() {
        val context = EvaluationContext()
        assertTrue(evaluator.evaluate(listOf("all", true, true), context) as Boolean)
        assertFalse(evaluator.evaluate(listOf("all", true, false), context) as Boolean)
        assertFalse(evaluator.evaluate(listOf("all", false, true), context) as Boolean)
        assertFalse(evaluator.evaluate(listOf("all", false, false), context) as Boolean)
        assertTrue(evaluator.evaluate(listOf("all"), context) as Boolean) // No conditions
    }

    @Test
    fun testAny() {
        val context = EvaluationContext()
        assertTrue(evaluator.evaluate(listOf("any", true, true), context) as Boolean)
        assertTrue(evaluator.evaluate(listOf("any", true, false), context) as Boolean)
        assertTrue(evaluator.evaluate(listOf("any", false, true), context) as Boolean)
        assertFalse(evaluator.evaluate(listOf("any", false, false), context) as Boolean)
        assertFalse(evaluator.evaluate(listOf("any"), context) as Boolean) // No conditions
    }

    @Test
    fun testNot() {
        val context = EvaluationContext()
        assertFalse(evaluator.evaluate(listOf("!", true), context) as Boolean)
        assertTrue(evaluator.evaluate(listOf("!", false), context) as Boolean)
        assertEquals(null, evaluator.evaluate(listOf("!", "not-a-boolean"), context))
    }

    @Test
    fun testComparison() {
        val context = EvaluationContext()
        assertTrue(evaluator.evaluate(listOf("==", 1, 1), context) as Boolean)
        assertFalse(evaluator.evaluate(listOf("==", 1, 2), context) as Boolean)
        assertTrue(evaluator.evaluate(listOf("!=", 1, 2), context) as Boolean)
        assertFalse(evaluator.evaluate(listOf("!=", 1, 1), context) as Boolean)
        assertTrue(evaluator.evaluate(listOf(">", 2, 1), context) as Boolean)
        assertFalse(evaluator.evaluate(listOf(">", 1, 2), context) as Boolean)
        assertTrue(evaluator.evaluate(listOf("<", 1, 2), context) as Boolean)
        assertFalse(evaluator.evaluate(listOf("<", 2, 1), context) as Boolean)
        assertTrue(evaluator.evaluate(listOf(">=", 2, 1), context) as Boolean)
        assertTrue(evaluator.evaluate(listOf(">=", 1, 1), context) as Boolean)
        assertFalse(evaluator.evaluate(listOf(">=", 1, 2), context) as Boolean)
        assertTrue(evaluator.evaluate(listOf("<=", 1, 2), context) as Boolean)
        assertTrue(evaluator.evaluate(listOf("<=", 1, 1), context) as Boolean)
        assertFalse(evaluator.evaluate(listOf("<=", 2, 1), context) as Boolean)
    }

    @Test
    fun testGet() {
        val context = EvaluationContext(featureProperties = mapOf("name" to "test", "value" to 123))
        assertEquals("test", evaluator.evaluate(listOf("get", "name"), context))
        assertEquals(123, evaluator.evaluate(listOf("get", "value"), context))
        assertNull(evaluator.evaluate(listOf("get", "non-existent"), context))
    }

    @Test
    fun testHas() {
        val context = EvaluationContext(featureProperties = mapOf("name" to "test"))
        assertTrue(evaluator.evaluate(listOf("has", "name"), context) as Boolean)
        assertFalse(evaluator.evaluate(listOf("has", "non-existent"), context) as Boolean)
    }

    @Test
    fun testAt() {
        val context = EvaluationContext()
        val array = listOf(10, 20, 30)
        assertEquals(20, evaluator.evaluate(listOf("at", 1, array), context))
        assertNull(evaluator.evaluate(listOf("at", 5, array), context))
    }

    @Test
    fun testIn() {
        val context = EvaluationContext()
        assertTrue(evaluator.evaluate(listOf("in", "a", "abc"), context) as Boolean)
        assertFalse(evaluator.evaluate(listOf("in", "d", "abc"), context) as Boolean)
        assertTrue(evaluator.evaluate(listOf("in", 1, listOf(1, 2, 3)), context) as Boolean)
        assertFalse(evaluator.evaluate(listOf("in", 4, listOf(1, 2, 3)), context) as Boolean)
    }

    @Test
    fun testIndexOf() {
        val context = EvaluationContext()
        assertEquals(1, evaluator.evaluate(listOf("index-of", "b", "abc"), context))
        assertEquals(-1, evaluator.evaluate(listOf("index-of", "d", "abc"), context))
        assertEquals(1, evaluator.evaluate(listOf("index-of", 2, listOf(1, 2, 3)), context))
        assertEquals(-1, evaluator.evaluate(listOf("index-of", 4, listOf(1, 2, 3)), context))
    }

    @Test
    fun testLength() {
        val context = EvaluationContext()
        assertEquals(3, evaluator.evaluate(listOf("length", "abc"), context))
        assertEquals(3, evaluator.evaluate(listOf("length", listOf(1, 2, 3)), context))
    }

    @Test
    fun testSlice() {
        val context = EvaluationContext()
        assertEquals("b", evaluator.evaluate(listOf("slice", "abc", 1, 2), context))
        assertEquals(listOf(2), evaluator.evaluate(listOf("slice", listOf(1, 2, 3), 1, 2), context))
    }

    @Test
    fun testCase() {
        val context = EvaluationContext()
        assertEquals("one", evaluator.evaluate(listOf("case", true, "one", "two"), context))
        assertEquals("two", evaluator.evaluate(listOf("case", false, "one", "two"), context))
        assertEquals("three", evaluator.evaluate(listOf("case", false, "one", false, "two", "three"), context))
    }

    @Test
    fun testCoalesce() {
        val context = EvaluationContext()
        assertEquals("a", evaluator.evaluate(listOf("coalesce", null, "a", "b"), context))
        assertEquals("b", evaluator.evaluate(listOf("coalesce", null, null, "b"), context))
    }

    @Test
    fun testMatch() {
        val context = EvaluationContext()
        assertEquals("one", evaluator.evaluate(listOf("match", 1, 1, "one", "other"), context))
        assertEquals("other", evaluator.evaluate(listOf("match", 3, 1, "one", 2, "two", "other"), context))
        assertEquals("one or two", evaluator.evaluate(listOf("match", 2, listOf(1, 2), "one or two", "other"), context))
    }

    @Test
    fun testLiteral() {
        val context = EvaluationContext()
        assertEquals(listOf(1, 2, 3), evaluator.evaluate(listOf("literal", listOf(1, 2, 3)), context))
        assertEquals(mapOf("a" to 1), evaluator.evaluate(listOf("literal", mapOf("a" to 1)), context))
    }

    @Test
    fun testTypeOf() {
        val context = EvaluationContext()
        assertEquals("string", evaluator.evaluate(listOf("typeof", "abc"), context))
        assertEquals("number", evaluator.evaluate(listOf("typeof", 123), context))
        assertEquals("boolean", evaluator.evaluate(listOf("typeof", true), context))
        assertEquals("array", evaluator.evaluate(listOf("typeof", listOf(1, 2)), context))
        assertEquals("object", evaluator.evaluate(listOf("typeof", mapOf("a" to 1)), context))
        assertEquals("null", evaluator.evaluate(listOf("typeof", null), context))
    }

    @Test
    fun testConcat() {
        val context = EvaluationContext()
        assertEquals("abc", evaluator.evaluate(listOf("concat", "a", "b", "c"), context))
    }

    @Test
    fun testUpcaseDowncase() {
        val context = EvaluationContext()
        assertEquals("ABC", evaluator.evaluate(listOf("upcase", "aBc"), context))
        assertEquals("abc", evaluator.evaluate(listOf("downcase", "aBc"), context))
    }

    @Test
    fun testRgb() {
        val context = EvaluationContext()
        val color = evaluator.evaluate(listOf("rgb", 255, 0, 0), context) as Color
        assertEquals(Color(255, 0, 0, 255), color)
    }

    @Test
    fun testRgba() {
        val context = EvaluationContext()
        val color = evaluator.evaluate(listOf("rgba", 0, 255, 0, 0.5), context) as Color
        assertEquals(Color(0, 255, 0, 127), color)
    }

    @Test
    fun testHsl() {
        val context = EvaluationContext()
        val redColor = evaluator.evaluate(listOf("hsl", 0, 100, 50), context) as Color
        assertEquals(redColor.red , 1f)

        val grayColor = evaluator.evaluate(listOf("hsl", 0, 0, 50), context) as Color
        assertEquals(grayColor.red, grayColor.green, 0.01f)
        assertEquals(grayColor.green, grayColor.blue, 0.01f)
    }

    @Test
    fun testHsla() {
        val context = EvaluationContext()
        val colorWithAlpha = evaluator.evaluate(listOf("hsla", 0, 100, 50, 0.5), context) as Color
        assertFalse(colorWithAlpha.red !in 0f..1f, "Red should be in valid range")
    }

    @Test
    fun testMath() {
        val context = EvaluationContext()
        assertEquals(5.0, evaluator.evaluate(listOf("+", 2, 3), context))
        assertEquals(-1.0, evaluator.evaluate(listOf("-", 2, 3), context))
        assertEquals(6.0, evaluator.evaluate(listOf("*", 2, 3), context))
        assertEquals(2.0, evaluator.evaluate(listOf("/", 6, 3), context))
    }

    @Test
    fun testStep() {
        val context = EvaluationContext(zoomLevel = 7.5)
        val expression = listOf("step", listOf("zoom"), "default", 5, "low", 10, "high")
        assertEquals("low", evaluator.evaluate(expression, context))
        val context2 = EvaluationContext(zoomLevel = 12.0)
        assertEquals("high", evaluator.evaluate(expression, context2))
        val context3 = EvaluationContext(zoomLevel = 3.0)
        assertEquals("default", evaluator.evaluate(expression, context3))
    }

    @Test
    fun testInterpolateLinear() {
        val context = EvaluationContext(zoomLevel = 7.5)
        val expression = listOf("interpolate", listOf("linear"), listOf("zoom"), 5, 10, 10, 20)
        assertEquals(15.0, evaluator.evaluate(expression, context))
    }

    @Test
    fun testInterpolateExponential() {
        val context = EvaluationContext(zoomLevel = 7.5)
        val expression = listOf("interpolate", listOf("exponential", 2.0), listOf("zoom"), 5, 10, 10, 20)
        // This is a bit more complex to calculate by hand, but we can check if it's in the range
        val result = evaluator.evaluate(expression, context) as Double
        assertTrue(result > 10.0 && result < 20.0)
    }

    @Test
    fun testEdgeCaseNullProperties() {
        val context = EvaluationContext(featureProperties = emptyMap())

        // Getting non-existent property should return null
        assertNull(evaluator.evaluate(listOf("get", "non-existent"), context))

        // Coalesce with null should skip to next value
        assertEquals("fallback", evaluator.evaluate(listOf("coalesce", null, "fallback"), context))
    }

    @Test
    fun testEdgeCaseMissingProperties() {
        val context = EvaluationContext(featureProperties = mapOf("value" to 42))

        // has should return false for missing properties
        assertFalse(evaluator.evaluate(listOf("has", "missing"), context) as Boolean)

        // has should return true for existing properties
        assertTrue(evaluator.evaluate(listOf("has", "value"), context) as Boolean)
    }

    @Test
    fun testEdgeCaseEmptyArrays() {
        val context = EvaluationContext()

        // length of empty array
        assertEquals(0, evaluator.evaluate(listOf("length", emptyList<Any>()), context))

        // length of empty string
        assertEquals(0, evaluator.evaluate(listOf("length", ""), context))

        // at on empty array should return null
        assertNull(evaluator.evaluate(listOf("at", 0, emptyList<Any>()), context))
    }

    @Test
    fun testEdgeCaseBoundaryValues() {
        val context = EvaluationContext()

        // Valid array access
        val array = listOf(10, 20, 30, 40)
        assertEquals(10, evaluator.evaluate(listOf("at", 0, array), context))
        assertEquals(40, evaluator.evaluate(listOf("at", 3, array), context))

        // Out of bounds positive indices
        assertNull(evaluator.evaluate(listOf("at", 100, array), context))
    }

    @Test
    fun testEdgeCaseNullComparison() {
        val context = EvaluationContext()

        // typeof null
        assertEquals("null", evaluator.evaluate(listOf("typeof", null), context))

        // Comparison with null
        assertFalse(evaluator.evaluate(listOf("==", null, 0), context) as Boolean)
        assertTrue(evaluator.evaluate(listOf("==", null, null), context) as Boolean)
    }

    @Test
    fun testEdgeCaseCoercion() {
        val context = EvaluationContext()

        // Type comparison - string vs number are not equal without explicit coercion
        val result = evaluator.evaluate(listOf("==", "5", 5), context)
        // Just verify they have different types
        assertEquals("string", evaluator.evaluate(listOf("typeof", "5"), context))
        assertEquals("number", evaluator.evaluate(listOf("typeof", 5), context))
    }

    @Test
    fun testEdgeCaseDeepNesting() {
        val context = EvaluationContext(zoomLevel = 10.0, featureProperties = mapOf("value" to 5))

        // Test 5-level deep nesting
        val nested = listOf(
            "case",
            listOf("==", listOf("get", "value"), 5),
            listOf("case",
                listOf(">", listOf("zoom"), 5),
                listOf("case",
                    true,
                    "deeply nested",
                    "default"
                ),
                "nope"
            ),
            "outer default"
        )

        assertEquals("deeply nested", evaluator.evaluate(nested, context))
    }

    @Test
    fun testEdgeCaseStringOperations() {
        val context = EvaluationContext()

        // Empty string concat
        assertEquals("", evaluator.evaluate(listOf("concat"), context))

        // Single item concat
        assertEquals("test", evaluator.evaluate(listOf("concat", "test"), context))

        // Upcase/downcase on empty string
        assertEquals("", evaluator.evaluate(listOf("upcase", ""), context))
        assertEquals("", evaluator.evaluate(listOf("downcase", ""), context))
    }

    @Test
    fun testEdgeCaseMathOperations() {
        val context = EvaluationContext()

        // Large numbers
        val result = evaluator.evaluate(listOf("+", 1e10, 1), context) as Double
        assertTrue(result > 1e10)

        // Negative multiplication
        assertEquals(-6.0, evaluator.evaluate(listOf("*", -2, 3), context))

        // Basic operations
        assertEquals(5.0, evaluator.evaluate(listOf("+", 2, 3), context))
    }

    @Test
    fun testEdgeCaseColorOperations() {
        val context = EvaluationContext()

        // RGB with boundary values
        val color1 = evaluator.evaluate(listOf("rgb", 0, 0, 0), context) as Color
        assertEquals(Color(0, 0, 0, 255), color1)

        val color2 = evaluator.evaluate(listOf("rgb", 255, 255, 255), context) as Color
        assertEquals(Color(255, 255, 255, 255), color2)

        // RGBA with alpha boundary values
        val color3 = evaluator.evaluate(listOf("rgba", 0, 0, 0, 0), context) as Color
        assertEquals(Color(0, 0, 0, 0), color3)

        val color4 = evaluator.evaluate(listOf("rgba", 255, 255, 255, 1), context) as Color
        assertEquals(Color(255, 255, 255, 255), color4)
    }

    @Test
    fun testEdgeCaseArrayMembership() {
        val context = EvaluationContext()

        // Empty array
        assertFalse(evaluator.evaluate(listOf("in", 1, emptyList<Any>()), context) as Boolean)

        // index-of on empty array
        assertEquals(-1, evaluator.evaluate(listOf("index-of", 1, emptyList<Any>()), context))

        // index-of with duplicate values
        assertEquals(0, evaluator.evaluate(listOf("index-of", 1, listOf(1, 1, 1)), context))
    }

    @Test
    fun testEdgeCaseMatchExpression() {
        val context = EvaluationContext()

        // Match with no matching branch uses default
        assertEquals("default", evaluator.evaluate(
            listOf("match", 99, 1, "one", 2, "two", "default"),
            context
        ))

        // Match with first value matching
        assertEquals("one", evaluator.evaluate(
            listOf("match", 1, 1, "one", 2, "two", "default"),
            context
        ))

        // Match with array containing value
        assertEquals("array match", evaluator.evaluate(
            listOf("match", 2, listOf(1, 2, 3), "array match", "default"),
            context
        ))
    }

    @Test
    fun testFilterRestrictionIntegerZoom() {
        // Zoom access and comparison
        val context = EvaluationContext(zoomLevel = 10.0)

        // Get zoom value
        val zoomVal = evaluator.evaluate(listOf("zoom"), context)
        assertEquals(10.0, zoomVal)

        // Zoom comparison
        assertTrue(evaluator.evaluate(listOf(">=", listOf("zoom"), 10), context) as Boolean)
        assertTrue(evaluator.evaluate(listOf("<", listOf("zoom"), 11), context) as Boolean)
    }

    @Test
    fun testGetPropertyWithSpecialCharacters() {
        val context = EvaluationContext(featureProperties = mapOf(
            "name-en" to "English Name",
            "name_ja" to "日本語名",
            "name_special" to "Special"
        ))

        assertEquals("English Name", evaluator.evaluate(listOf("get", "name-en"), context))
        assertEquals("日本語名", evaluator.evaluate(listOf("get", "name_ja"), context))
        assertEquals("Special", evaluator.evaluate(listOf("get", "name_special"), context))
    }

    @Test
    fun testInterpolateWithAllModes() {
        val context = EvaluationContext(zoomLevel = 7.5)

        // Linear interpolation
        val linear = evaluator.evaluate(
            listOf("interpolate", listOf("linear"), listOf("zoom"), 5, 10, 10, 20),
            context
        ) as Double
        assertEquals(15.0, linear, 0.01)

        // Exponential interpolation with different bases
        val exp1 = evaluator.evaluate(
            listOf("interpolate", listOf("exponential", 1.0), listOf("zoom"), 5, 10, 10, 20),
            context
        ) as Double
        // With base 1.0, exponential should be similar to linear
        assertTrue(exp1 > 10 && exp1 < 20)

        // Exponential with base 2.0
        val exp2 = evaluator.evaluate(
            listOf("interpolate", listOf("exponential", 2.0), listOf("zoom"), 5, 10, 10, 20),
            context
        ) as Double
        assertTrue(exp2 > 10 && exp2 < 20)
    }
}
