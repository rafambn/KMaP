package com.rafambn.kmap.utils.style

import com.rafambn.kmap.utils.style.expressions.evaluateAt
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
        assertEquals(255, color.red)
        assertEquals(0, color.green)
        assertEquals(0, color.blue)
        assertEquals(255, color.alpha)
    }

    @Test
    fun testRgba() {
        val context = EvaluationContext()
        val color = evaluator.evaluate(listOf("rgba", 0, 255, 0, 0.5), context) as Color
        assertEquals(0, color.red)
        assertEquals(255, color.green)
        assertEquals(0, color.blue)
        assertEquals(127, color.alpha)
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
}
