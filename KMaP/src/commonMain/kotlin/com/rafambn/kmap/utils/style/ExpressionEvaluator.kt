package com.rafambn.kmap.utils.style

import com.rafambn.kmap.utils.style.expressions.evaluateAll
import com.rafambn.kmap.utils.style.expressions.evaluateAny
import com.rafambn.kmap.utils.style.expressions.evaluateAt
import com.rafambn.kmap.utils.style.expressions.evaluateCase
import com.rafambn.kmap.utils.style.expressions.evaluateCoalesce
import com.rafambn.kmap.utils.style.expressions.evaluateComparison
import com.rafambn.kmap.utils.style.expressions.evaluateConcat
import com.rafambn.kmap.utils.style.expressions.evaluateGet
import com.rafambn.kmap.utils.style.expressions.evaluateHas
import com.rafambn.kmap.utils.style.expressions.evaluateHsl
import com.rafambn.kmap.utils.style.expressions.evaluateIn
import com.rafambn.kmap.utils.style.expressions.evaluateIndexOf
import com.rafambn.kmap.utils.style.expressions.evaluateInterpolate
import com.rafambn.kmap.utils.style.expressions.evaluateLength
import com.rafambn.kmap.utils.style.expressions.evaluateLiteral
import com.rafambn.kmap.utils.style.expressions.evaluateMatch
import com.rafambn.kmap.utils.style.expressions.evaluateNot
import com.rafambn.kmap.utils.style.expressions.evaluateNumber
import com.rafambn.kmap.utils.style.expressions.evaluateRgb
import com.rafambn.kmap.utils.style.expressions.evaluateSlice
import com.rafambn.kmap.utils.style.expressions.evaluateStep
import com.rafambn.kmap.utils.style.expressions.evaluateString
import com.rafambn.kmap.utils.style.expressions.evaluateTypeOf
import com.rafambn.kmap.utils.style.expressions.evaluateUpDownCase
import com.rafambn.kmap.utils.style.expressions.parseColor

class ExpressionEvaluator {

    private fun resolveTokens(text: String, context: EvaluationContext): String {
        val regex = "\\{name(?::(.*?))?\\}".toRegex()
        return regex.replace(text) { matchResult ->
            val lang = matchResult.groupValues.getOrNull(1)
            val propertyName = when {
                lang != null && lang.isNotEmpty() -> "name:$lang"
                else -> "name:${context.locale}"
            }
            context.featureProperties[propertyName]?.toString() ?: context.featureProperties["name"]?.toString() ?: ""
        }
    }

    private fun evaluateStringExpression(expression: String, context: EvaluationContext): Any {
        val color = parseColor(expression)
        if (color != null) {
            return color
        }
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
            "get" -> evaluateGet(expression, context)
            "has" -> evaluateHas(expression, context)
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

            // Type
            "literal" -> evaluateLiteral(expression)
            "to-string" -> evaluateString(expression, context, this)
            "typeof" -> evaluateTypeOf(expression, context, this)

            // String
            "concat" -> evaluateConcat(expression, context, this)
            "downcase" -> evaluateUpDownCase(expression, context, this, up = false)
            "upcase" -> evaluateUpDownCase(expression, context, this, up = true)

            // Color
            "rgb" -> evaluateRgb(expression, context, this)
            "rgba" -> evaluateRgb(expression, context, this)
            "hsl" -> evaluateHsl(expression, context, this)
            "hsla" -> evaluateHsl(expression, context, this)

            // Math
            "+", "-", "*", "/" -> evaluateNumber(expression, context, this)

            // Interpolation
            "step" -> evaluateStep(expression, context, this)
            "interpolate" -> evaluateInterpolate(expression, context, this)

            else -> expression
        }
    }

    fun evaluate(expression: Any?, context: EvaluationContext): Any? {
        return when (expression) {
            is String -> evaluateStringExpression(expression, context)
            is List<*> -> {
                val result = evaluateListExpression(expression, context)
                if (result is String) {
                    return evaluateStringExpression(result, context)
                }
                result
            }
            else -> expression
        }
    }

    fun getRequiredProperties(expression: Any?): Set<String> {
        if (expression is String && expression.contains("{") && expression.contains("}")) {
            val properties = mutableSetOf<String>()
            val regex = "\\{name(?::(.*?))?\\}".toRegex()
            regex.findAll(expression).forEach { matchResult ->
                val lang = matchResult.groupValues.getOrNull(1)
                if (lang != null && lang.isNotEmpty()) {
                    properties.add("name:$lang") //TODO the regex must not be only name but the variable name: {number}, {ref}, road_{ref_lenght}
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

        if (operator == "get") {
            (expression.getOrNull(1) as? String)?.let { requiredProperties.add(it) }
            return requiredProperties
        }

        expression.drop(1).forEach { arg ->
            requiredProperties.addAll(getRequiredProperties(arg))
        }

        return requiredProperties
    }
}
