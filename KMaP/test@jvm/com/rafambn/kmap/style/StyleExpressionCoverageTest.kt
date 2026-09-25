package com.rafambn.kmap.style

import androidx.compose.ui.graphics.Color
import com.rafambn.kmap.style.expression.compare
import com.rafambn.kmap.style.expression.evaluateComparison
import com.rafambn.kmap.style.expression.evaluateNumber
import com.rafambn.kmap.style.expression.parseColor
import com.rafambn.kmap.style.expression.toDouble
import de.infix.testBalloon.framework.core.testSuite
import java.lang.reflect.Proxy
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

val StyleExpressionCoverageTest by testSuite {
    testFixture { ExpressionEvaluator() } asParameterForEach {
        test("resolves name tokens and collects their feature properties") { evaluator ->
            val context = EvaluationContext(
                featureProperties = mapOf("name" to "Default", "name:pt" to "Nome"),
                locale = "pt"
            )

            assertEquals("Nome", evaluator.evaluate("{name}", context))
            assertEquals("Default", evaluator.evaluate("{name:fr}", context))
            assertEquals("Nome", evaluator.evaluate("{name:pt}", context))
            assertEquals("", evaluator.evaluate("{name}", EvaluationContext()))
            assertEquals("{", evaluator.evaluate("{", context))
            assertEquals("}", evaluator.evaluate("}", context))
            assertEquals("{ref}", evaluator.evaluate("{ref}", context))
            assertEquals("Nome Default", evaluator.evaluate(listOf("concat", "{name}", " ", listOf("get", "name")), context))

            assertEquals(setOf("name", "name:pt"), evaluator.getRequiredProperties("{name} {name:pt} {ref}"))
            assertEquals(setOf("name"), evaluator.getRequiredProperties("{name:}"))
            assertEquals(emptySet(), evaluator.getRequiredProperties("{name"))
            assertEquals(emptySet(), evaluator.getRequiredProperties("plain text"))
            assertEquals(setOf("class"), evaluator.getRequiredProperties(listOf("all", listOf("get", "class"), listOf("get", 1))))
            assertEquals(emptySet(), evaluator.getRequiredProperties(listOf("get", 1)))
            assertEquals(emptySet(), evaluator.getRequiredProperties(listOf("get")))
            assertEquals(emptySet(), evaluator.getRequiredProperties(emptyList<Any>()))
        }

        test("dispatches context, map, and unsupported expressions") { evaluator ->
            val context = EvaluationContext(geometryType = "LineString", zoomLevel = 4.0, featureId = 42)

            assertEquals("LineString", evaluator.evaluate(listOf("geometry-type"), context))
            assertEquals(42, evaluator.evaluate(listOf("id"), context))
            assertEquals(4.0, evaluator.evaluate(listOf("zoom"), context))
            assertEquals(listOf("future-op", true), evaluator.evaluate(listOf("future-op", true), context))
            assertEquals(listOf(1, 2), evaluator.evaluate(listOf(1, 2), context))
            assertEquals(emptyList<Any>(), evaluator.evaluate(emptyList<Any>(), context) as List<*>)
            assertEquals(7, evaluator.evaluate(7, context))

            val literalMap = mapOf("value" to 7)
            assertEquals(literalMap, evaluator.evaluate(literalMap, context))
            assertEquals(4.0, evaluator.evaluate(
                mapOf("base" to "invalid", "stops" to listOf("ignored", listOf(0), listOf(0, 0), listOf(10, 10))),
                context
            ))
            assertTrue((evaluator.evaluate(
                mapOf("base" to 2.0, "stops" to listOf(listOf(0, 0), listOf(10, 10))),
                context
            ) as Double) in 3.0..4.0)
        }

        test("handles malformed feature and lookup expressions") { evaluator ->
            val context = EvaluationContext(featureProperties = mapOf("name" to "Place", "missing" to ""))

            assertNull(evaluator.evaluate(listOf("!"), context))
            assertNull(evaluator.evaluate(listOf("==", 1), context))
            assertNull(evaluator.evaluate(listOf("<", null, 1), context))
            assertNull(evaluator.evaluate(listOf("get"), context))
            assertNull(evaluator.evaluate(listOf("get", 1), context))
            assertNull(evaluator.evaluate(listOf("get", "name", "extra"), context))
            assertFalse(evaluator.evaluate(listOf("has"), context) as Boolean)
            assertFalse(evaluator.evaluate(listOf("has", 1), context) as Boolean)
            assertFalse(evaluator.evaluate(listOf("has", "absent"), context) as Boolean)
            assertTrue(evaluator.evaluate(listOf("has", "missing"), context) as Boolean)

            assertNull(evaluator.evaluate(listOf("at", 0), context))
            assertNull(evaluator.evaluate(listOf("at", "bad", listOf(1)), context))
            assertNull(evaluator.evaluate(listOf("at", 0, "not an array"), context))
            assertNull(evaluator.evaluate(listOf("at", -1, listOf(1)), context))
            assertFalse(evaluator.evaluate(listOf("in", "a"), context) as Boolean)
            assertFalse(evaluator.evaluate(listOf("in", 1, "abc"), context) as Boolean)
            assertFalse(evaluator.evaluate(listOf("in", 1, 2), context) as Boolean)
            assertTrue(evaluator.evaluate(listOf("in", 1, listOf(1, 2)), context) as Boolean)

            assertNull(evaluator.evaluate(listOf("index-of", "a"), context))
            assertNull(evaluator.evaluate(listOf("index-of", 1, "abc"), context))
            assertNull(evaluator.evaluate(listOf("index-of", "a", 2), context))
            assertEquals(1, evaluator.evaluate(listOf("index-of", 2, listOf(1, 2)), context))
            assertNull(evaluator.evaluate(listOf("length"), context))
            assertNull(evaluator.evaluate(listOf("length", true), context))
            assertNull(evaluator.evaluate(listOf("slice", "abc"), context))
            assertNull(evaluator.evaluate(listOf("slice", "abc", "bad"), context))
            assertNull(evaluator.evaluate(listOf("slice", 2, 0), context))
            assertEquals("bcd", evaluator.evaluate(listOf("slice", "abcd", 1), context))
            assertEquals(listOf(2, 3), evaluator.evaluate(listOf("slice", listOf(1, 2, 3), 1), context))
            assertEquals("bcd", evaluator.evaluate(listOf("slice", "abcd", 1, "bad"), context))
        }

        test("covers conditional and type expression fallbacks") { evaluator ->
            val context = EvaluationContext()

            assertNull(evaluator.evaluate(listOf("case", true, "value"), context))
            assertEquals("second", evaluator.evaluate(listOf("case", false, "first", true, "second", "default"), context))
            assertEquals("default", evaluator.evaluate(listOf("case", false, "first", false, "second", false, "third", "default"), context))
            assertEquals("third", evaluator.evaluate(listOf("case", false, "first", false, "second", true, "third", "default"), context))
            assertNull(evaluator.evaluate(listOf("coalesce"), context))
            assertNull(evaluator.evaluate(listOf("coalesce", null, null), context))
            assertNull(evaluator.evaluate(listOf("match", 1, 1), context))
            assertEquals("default", evaluator.evaluate(listOf("match", 5, listOf(1, 2), "array", 1, "one", "default"), context))
            assertEquals("default", evaluator.evaluate(listOf("match", 5, listOf(1, 2), "array", 1, "one", 3, "three", "default"), context))
            assertNull(evaluator.evaluate(listOf("literal"), context))
            assertEquals("42", evaluator.evaluate(listOf("to-string", 42), context))
            assertNull(evaluator.evaluate(listOf("to-string"), context))
            assertNull(evaluator.evaluate(listOf("to-string", null), context))
            assertEquals("object", evaluator.evaluate(listOf("typeof", Any()), context))
            assertEquals("4", evaluator.evaluate(listOf("concat", null, listOf("get", "absent"), 4), context))
            assertEquals("", evaluator.evaluate(listOf("concat"), context))
            assertNull(evaluator.evaluate(listOf("upcase"), context))
            assertNull(evaluator.evaluate(listOf("downcase", 1), context))
        }

        test("rejects invalid colors and math while clamping valid values") { evaluator ->
            val context = EvaluationContext()

            assertNull(evaluator.evaluate(listOf("rgb", 1, 2), context))
            assertNull(evaluator.evaluate(listOf("rgb", 1, 2, 3, 0.5, 6), context))
            assertNull(evaluator.evaluate(listOf("rgb", "bad", 2, 3), context))
            assertNull(evaluator.evaluate(listOf("rgb", 1, "bad", 3), context))
            assertNull(evaluator.evaluate(listOf("rgb", 1, 2, "bad"), context))
            assertEquals(Color(0, 255, 3, 255), evaluator.evaluate(listOf("rgba", -1, 300, 3, "bad"), context))
            assertEquals(Color(0, 0, 0, 0), evaluator.evaluate(listOf("rgba", 0, 0, 0, -1), context))

            assertNull(evaluator.evaluate(listOf("hsl", 1, 2), context))
            assertNull(evaluator.evaluate(listOf("hsl", 1, 2, 3, 0.5, 6), context))
            assertNull(evaluator.evaluate(listOf("hsl", "bad", 2, 3), context))
            assertNull(evaluator.evaluate(listOf("hsl", 1, "bad", 3), context))
            assertNull(evaluator.evaluate(listOf("hsl", 1, 2, "bad"), context))
            assertEquals(Color(255, 0, 0, 255), evaluator.evaluate(listOf("hsla", 0, 120, 50, "bad"), context))

            assertNull(evaluator.evaluate(listOf("+", 1), context))
            assertNull(evaluator.evaluate(listOf("+", "bad", 1), context))
            assertNull(evaluator.evaluate(listOf("+", 1, "bad"), context))
            assertNull(evaluator.evaluate(listOf("/", 1, 0), context))
        }

        test("covers step and interpolation boundaries and output types") { evaluator ->
            assertNull(evaluator.evaluate(listOf("step", listOf("zoom"), "default"), EvaluationContext()))
            assertNull(evaluator.evaluate(listOf("step", "bad", "default", 1, "high"), EvaluationContext()))
            assertEquals("hit", evaluator.evaluate(
                listOf("step", listOf("zoom"), "default", "bad", "ignored", 5, "hit"),
                EvaluationContext(zoomLevel = 5.0)
            ))

            val lowZoom = EvaluationContext(zoomLevel = -1.0)
            val highZoom = EvaluationContext(zoomLevel = 11.0)
            val middleZoom = EvaluationContext(zoomLevel = 5.0)
            assertNull(evaluator.evaluate(listOf("interpolate", listOf("linear")), middleZoom))
            assertNull(evaluator.evaluate(listOf("interpolate", "linear", listOf("zoom"), 0, 0), middleZoom))
            assertNull(evaluator.evaluate(listOf("interpolate", listOf("linear"), "bad", 0, 0, 10, 10), middleZoom))
            assertNull(evaluator.evaluate(listOf("interpolate", listOf("linear"), listOf("zoom"), 0, 0, 10), middleZoom))
            assertEquals(0, evaluator.evaluate(listOf("interpolate", listOf("linear"), listOf("zoom"), 0, 0, 10, 10), lowZoom))
            assertEquals(10, evaluator.evaluate(listOf("interpolate", listOf("linear"), listOf("zoom"), 0, 0, 10, 10), highZoom))
            assertEquals(5.0, evaluator.evaluate(listOf("interpolate", listOf("cubic-bezier"), listOf("zoom"), 0, 0, 10, 10), middleZoom))
            assertEquals(5.0, evaluator.evaluate(listOf("interpolate", listOf(1), listOf("zoom"), 0, 0, 10, 10), middleZoom))
            assertEquals(5.0, evaluator.evaluate(listOf("interpolate", listOf("exponential"), listOf("zoom"), 0, 0, 10, 10), middleZoom))
            assertTrue((evaluator.evaluate(listOf("interpolate", listOf("exponential", 2.0), listOf("zoom"), 0, 0, 10, 10), middleZoom) as Double) in 4.0..4.2)
            assertEquals(0.5, evaluator.evaluate(listOf("interpolate", listOf("linear"), listOf("zoom"), 0, 0, "bad", 1, 10, 3), middleZoom))
            assertTrue(evaluator.evaluate(
                listOf("interpolate", listOf("linear"), listOf("zoom"), 0, "#000000", 10, "#ffffff"),
                middleZoom
            ) is Color)
            assertEquals("low", evaluator.evaluate(
                listOf("interpolate", listOf("linear"), listOf("zoom"), 0, "low", 10, "high"),
                middleZoom
            ))
            assertEquals(0, evaluator.evaluate(
                listOf("interpolate", listOf("linear"), listOf("zoom"), 0, 0, 10, "high"),
                middleZoom
            ))
            assertEquals(Color(0, 0, 0, 255), evaluator.evaluate(
                listOf("interpolate", listOf("linear"), listOf("zoom"), 0, "#000000", 10, "high"),
                middleZoom
            ))
            assertEquals("first", evaluator.evaluate(
                listOf("interpolate", listOf("linear"), listOf("zoom"), Double.NaN, "first", Double.NaN, "last"),
                middleZoom
            ))
        }
    }

    test("covers expression utility conversions and comparison fallbacks") {
        val evaluator = ExpressionEvaluator()
        val context = EvaluationContext()
        val javaNullString = Proxy.newProxyInstance(
            Runnable::class.java.classLoader,
            arrayOf(Runnable::class.java)
        ) { _, method, _ -> if (method.name == "toString") null else Unit }

        // Java proxies can violate the nonnull toString contract, so keep this fallback covered.
        assertEquals("", evaluator.evaluate("{name}", EvaluationContext(featureProperties = mapOf("name:en" to javaNullString))))
        assertEquals("", evaluator.evaluate(listOf("concat", listOf("get", "name:en")), EvaluationContext(featureProperties = mapOf("name:en" to javaNullString))))

        assertEquals(2.0, toDouble(2))
        assertEquals(2.5, toDouble("2.5"))
        assertNull(toDouble("not a number"))
        assertNull(toDouble(true))
        assertEquals(0, compare("a", "a"))
        assertTrue(compare("b", "a")!! > 0)
        assertEquals(0, compare(1, 1.0))
        assertNull(compare(null, 1))
        assertNull(compare("a", null))
        assertNull(compare("one", 1))
        assertNull(compare(1, "one"))
        assertNull(evaluateComparison(listOf("unknown", 1, 2), context, evaluator))
        assertNull(evaluateNumber(listOf("%", 5, 2), context, evaluator))
        assertNull(evaluateNumber(listOf(",", 5, 2), context, evaluator))
        assertNull(evaluateNumber(listOf(".", 5, 2), context, evaluator))

        assertNull(parseColor("plain"))
        assertEquals(Color(170, 187, 204, 255), parseColor("#abc"))
        assertEquals(Color(170, 187, 204, 221), parseColor("#abcd"))
        assertEquals(Color(17, 34, 51, 255), parseColor("#112233"))
        assertEquals(Color(17, 34, 51, 68), parseColor("#11223344"))
        assertNull(parseColor("#12"))
        assertNull(parseColor("#12345"))
        assertNull(parseColor("#1234567"))
        assertNull(parseColor("#123456789"))
        assertNull(parseColor("#ggg"))
        assertNull(parseColor("#0g0"))
        assertNull(parseColor("#0000gg"))
        assertEquals(Color(17, 34, 51, 255), parseColor("#112233zz"))

        assertEquals(Color(1, 2, 3, 255), parseColor("rgb(1, 2, 3)"))
        assertEquals(Color(1, 2, 3, 127), parseColor("rgb(1, 2, 3, 0.5)"))
        assertEquals(Color(0, 255, 3, 127), parseColor("rgba(-1, 300, 3, 0.5)"))
        assertNull(parseColor("rgb(1, 2)"))
        assertNull(parseColor("rgb(x, 2, 3)"))
        assertNull(parseColor("rgb(1, x, 3)"))
        assertNull(parseColor("rgb(1, 2, x)"))
        assertEquals(Color(1, 2, 3, 255), parseColor("rgb(1, 2, 3, x)"))
        assertEquals(Color(1, 2, 3, 255), parseColor("rgb(1, 2, 3, 0.5, extra)"))

        assertNull(parseColor("hsl(1, 2)"))
        assertNull(parseColor("hsl(x, 2, 3)"))
        assertNull(parseColor("hsl(1, x%, 3%)"))
        assertNull(parseColor("hsl(1, 2%, x%)"))
        assertEquals(Color(255, 0, 0, 255), parseColor("hsl(0, 100%, 50%)"))
        assertEquals(Color(255, 0, 0, 127), parseColor("hsla(360, 120%, 50%, 0.5)"))
        assertEquals(Color(255, 0, 0, 255), parseColor("hsl(0, 100%, 50%, x)"))
        listOf(30, 90, 150, 210, 270, 330).forEach { hue -> assertTrue(parseColor("hsl($hue, 100%, 50%)") is Color) }
    }
}
