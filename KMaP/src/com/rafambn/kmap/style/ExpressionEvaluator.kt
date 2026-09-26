package com.rafambn.kmap.style

import com.rafambn.kmap.style.expression.*
import kotlin.math.E
import kotlin.math.PI
import kotlin.math.ln

class ExpressionEvaluator {

    private fun resolveTokens(text: String, context: EvaluationContext): String {
        val regex = "\\{name(?::(.*?))?\\}".toRegex()
        return regex.replace(text) { matchResult ->
            val lang = matchResult.groupValues[1]
            val propertyName = when {
                lang.isNotEmpty() -> "name:$lang"
                else -> "name:${context.locale}"
            }
            context.featureProperties[propertyName]?.toString() ?: context.featureProperties["name"]?.toString() ?: ""
        }
    }

    private fun evaluateStringExpression(expression: String, context: EvaluationContext): Any {
        if (expression.contains("{") && expression.contains("}")) {
            return resolveTokens(expression, context)
        }
        return expression
    }

    private fun evaluateListExpression(expression: List<*>, context: EvaluationContext): Any? {
        if (expression.isEmpty()) {
            return expression
        }
        val operator = expression[0] as? String
        return when (operator) {
            // Logical
            "all" -> evaluateAll(expression, context, this)
            "any" -> evaluateAny(expression, context, this)
            "!" -> evaluateNot(expression, context, this)

            // Comparison
            "==", "!=", ">", ">=", "<", "<=" -> evaluateComparison(expression, context, this)

            // Feature data
            "get" -> evaluateGet(expression, context, this)
            "has" -> evaluateHas(expression, context, this)
            "geometry-type" -> context.geometryType
            "id" -> context.featureId
            "zoom" -> context.zoomLevel

            // Lookup
            "at" -> evaluateAt(expression, context, this)
            "in" -> evaluateIn(expression, context, this)
            "index-of" -> evaluateIndexOf(expression, context, this)
            "length" -> evaluateLength(expression, context, this)
            "slice" -> evaluateSlice(expression, context, this)

            // Conditional
            "case" -> evaluateCase(expression, context, this)
            "coalesce" -> evaluateCoalesce(expression, context, this)
            "match" -> evaluateMatch(expression, context, this)
            "within" -> evaluateWithin(expression, context, this)

            // Type
            "literal" -> evaluateLiteral(expression)
            "image" -> evaluateImage(expression, context, this)
            "number-format" -> evaluateNumberFormat(expression, context, this)
            "array", "boolean", "number", "object", "string" -> evaluateTypeAssertion(expression, context, this)
            "to-boolean", "to-color", "to-number" -> evaluateConversion(expression, context, this)
            "to-string" -> evaluateString(expression, context, this)
            "typeof" -> evaluateTypeOf(expression, context, this)

            // String
            "concat" -> evaluateConcat(expression, context, this)
            "downcase" -> evaluateUpDownCase(expression, context, this, up = false)
            "upcase" -> evaluateUpDownCase(expression, context, this, up = true)
            "split" -> evaluateSplit(expression, context, this)

            // Color
            "rgb" -> evaluateRgb(expression, context, this)
            "rgba" -> evaluateRgb(expression, context, this)
            "hsl" -> evaluateHsl(expression, context, this)
            "hsla" -> evaluateHsl(expression, context, this)
            "to-hsla", "to-rgba" -> evaluateColorComponents(expression, context, this)

            // Math
            "+", "-", "*", "/" -> evaluateNumber(expression, context, this)
            "acos", "asin", "atan", "cos", "sin", "tan", "ln", "log10", "log2",
            "abs", "ceil", "floor", "round", "sqrt" -> evaluateUnaryMath(expression, context, this)
            "%", "^", "max", "min" -> evaluateBinaryMath(expression, context, this)
            "e" -> if (expression.size == 1) E else null
            "ln2" -> if (expression.size == 1) ln(2.0) else null
            "pi" -> if (expression.size == 1) PI else null

            // Interpolation
            "step" -> evaluateStep(expression, context, this)
            "interpolate" -> evaluateInterpolate(expression, context, this)
            "interpolate-hcl", "interpolate-lab" -> evaluatePerceptualInterpolate(expression, context, this)

            else -> expression
        }
    }

    fun evaluate(expression: Any?, context: EvaluationContext): Any? {
        return when (expression) {
            is String -> evaluateStringExpression(expression, context)
            is List<*> -> evaluateListExpression(expression, context)
            is Map<*, *> -> {
                val stops = expression["stops"] as? List<*>
                if (stops != null) {
                    val base = (expression["base"] as? Number)?.toDouble() ?: 1.0
                    val interpolationType = listOf("exponential", base)
                    val input = listOf("zoom")

                    val transformedExpression = mutableListOf<Any?>("interpolate", interpolationType, input)
                    stops.forEach { stop ->
                        if (stop is List<*> && stop.size == 2) {
                            transformedExpression.add(stop[0])
                            transformedExpression.add(stop[1])
                        }
                    }
                    return evaluate(transformedExpression, context)
                }
                expression
            }
            else -> expression
        }
    }

    fun getRequiredProperties(expression: Any?): Set<String> {
        if (expression is Map<*, *>) {
            return expression.values.flatMap { getRequiredProperties(it) }.toSet()
        }
        if (expression is String && expression.contains("{") && expression.contains("}")) {
            val properties = mutableSetOf<String>()
            val regex = "\\{name(?::(.*?))?\\}".toRegex()
            regex.findAll(expression).forEach { matchResult ->
                val lang = matchResult.groupValues[1]
                if (lang.isNotEmpty()) {
                    properties.add("name:$lang")
                } else {
                    properties.add("name")
                }
            }
            return properties
        }

        if (expression !is List<*> || expression.isEmpty()) {
            return emptySet()
        }

        val requiredProperties = mutableSetOf<String>()
        val operator = expression[0] as? String

        if (operator == "literal") return emptySet()

        if (operator == "get" || operator == "has") {
            if (expression.size == 2 && expression[1] is String) requiredProperties.add(expression[1] as String)
            else expression.drop(1).forEach { requiredProperties.addAll(getRequiredProperties(it)) }
            return requiredProperties
        }

        expression.drop(1).forEach { arg ->
            requiredProperties.addAll(getRequiredProperties(arg))
        }

        return requiredProperties
    }
}
