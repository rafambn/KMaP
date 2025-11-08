package com.rafambn.kmap.utils.style.expressions

import com.rafambn.kmap.utils.style.Color
import com.rafambn.kmap.utils.style.EvaluationContext
import com.rafambn.kmap.utils.style.ExpressionEvaluator
import kotlin.math.floor

// Logical
internal fun evaluateAll(expression: List<*>, context: EvaluationContext, evaluator: ExpressionEvaluator): Boolean {
    for (i in 1 until expression.size) {
        if (evaluator.evaluate(expression[i], context) != true) return false
    }
    return true
}

internal fun evaluateAny(expression: List<*>, context: EvaluationContext, evaluator: ExpressionEvaluator): Boolean {
    for (i in 1 until expression.size) {
        if (evaluator.evaluate(expression[i], context) == true) return true
    }
    return false
}

internal fun evaluateNot(expression: List<*>, context: EvaluationContext, evaluator: ExpressionEvaluator): Boolean? {
    if (expression.size != 2) return null
    return (evaluator.evaluate(expression[1], context) as? Boolean)?.not()
}

// Comparison
internal fun evaluateComparison(expression: List<*>, context: EvaluationContext, evaluator: ExpressionEvaluator): Boolean? {
    if (expression.size != 3) return null
    val op = expression[0] as String
    val left = evaluator.evaluate(expression[1], context)
    val right = evaluator.evaluate(expression[2], context)

    return when (op) {
        "==" -> left == right
        "!=" -> left != right
        ">" -> compare(left, right)?.let { it > 0 }
        "<" -> compare(left, right)?.let { it < 0 }
        ">=" -> compare(left, right)?.let { it >= 0 }
        "<=" -> compare(left, right)?.let { it <= 0 }
        else -> null
    }
}

// Feature data
internal fun evaluateGet(expression: List<*>, context: EvaluationContext): Any? {
    if (expression.size != 2) return null
    val key = expression[1] as? String ?: return null
    return context.featureProperties[key]
}

internal fun evaluateHas(expression: List<*>, context: EvaluationContext): Boolean {
    if (expression.size != 2) return false
    val key = expression[1] as? String ?: return false
    return context.featureProperties.containsKey(key)
}

// Lookup
internal fun evaluateAt(expression: List<*>, context: EvaluationContext, evaluator: ExpressionEvaluator): Any? {
    if (expression.size != 3) return null
    val index = toDouble(evaluator.evaluate(expression[1], context))?.toInt() ?: return null
    val array = evaluator.evaluate(expression[2], context) as? List<*> ?: return null
    return array.getOrNull(index)
}

internal fun evaluateIn(expression: List<*>, context: EvaluationContext, evaluator: ExpressionEvaluator): Boolean {
    if (expression.size != 3) return false
    val item = evaluator.evaluate(expression[1], context)
    val collection = evaluator.evaluate(expression[2], context)
    return when (collection) {
        is String -> (item as? String)?.let { collection.contains(it) } ?: false
        is List<*> -> collection.contains(item)
        else -> false
    }
}

internal fun evaluateIndexOf(expression: List<*>, context: EvaluationContext, evaluator: ExpressionEvaluator): Int? {
    if (expression.size < 3) return null
    val item = evaluator.evaluate(expression[1], context)
    val collection = evaluator.evaluate(expression[2], context)
    return when (collection) {
        is String -> (item as? String)?.let { collection.indexOf(it) }
        is List<*> -> collection.indexOf(item)
        else -> null
    }
}

internal fun evaluateLength(expression: List<*>, context: EvaluationContext, evaluator: ExpressionEvaluator): Int? {
    if (expression.size != 2) return null
    return when (val value = evaluator.evaluate(expression[1], context)) {
        is String -> value.length
        is List<*> -> value.size
        else -> null
    }
}

internal fun evaluateSlice(expression: List<*>, context: EvaluationContext, evaluator: ExpressionEvaluator): Any? {
    if (expression.size < 3) return null
    val value = evaluator.evaluate(expression[1], context)
    val from = toDouble(evaluator.evaluate(expression[2], context))?.toInt() ?: return null
    val to = if (expression.size > 3) toDouble(evaluator.evaluate(expression[3], context))?.toInt() else null
    return when (value) {
        is String -> value.substring(from, to ?: value.length)
        is List<*> -> value.subList(from, to ?: value.size)
        else -> null
    }
}

// Conditional
internal fun evaluateCase(expression: List<*>, context: EvaluationContext, evaluator: ExpressionEvaluator): Any? {
    if (expression.size < 4) return null
    for (i in 1 until expression.size - 1 step 2) {
        if (evaluator.evaluate(expression[i], context) == true) {
            return evaluator.evaluate(expression[i + 1], context)
        }
    }
    return evaluator.evaluate(expression.last(), context)
}

internal fun evaluateCoalesce(expression: List<*>, context: EvaluationContext, evaluator: ExpressionEvaluator): Any? {
    for (i in 1 until expression.size) {
        val result = evaluator.evaluate(expression[i], context)
        if (result != null) return result
    }
    return null
}

internal fun evaluateMatch(expression: List<*>, context: EvaluationContext, evaluator: ExpressionEvaluator): Any? {
    if (expression.size < 4) return null
    val input = evaluator.evaluate(expression[1], context)
    for (i in 2 until expression.size - 1 step 2) {
        val label = expression[i]
        if (label is List<*>) {
            if (input in label) return evaluator.evaluate(expression[i + 1], context)
        } else if (input == label) {
            return evaluator.evaluate(expression[i + 1], context)
        }
    }
    return evaluator.evaluate(expression.last(), context)
}

// Type
internal fun evaluateLiteral(expression: List<*>): Any? {
    if (expression.size != 2) return null
    return expression[1]
}

internal fun evaluateString(expression: List<*>, context: EvaluationContext, evaluator: ExpressionEvaluator): String? {
    if (expression.size != 2) return null
    return evaluator.evaluate(expression[1], context)?.toString()
}

internal fun evaluateTypeOf(expression: List<*>, context: EvaluationContext, evaluator: ExpressionEvaluator): String {
    val value = evaluator.evaluate(expression[1], context)
    return when (value) {
        null -> "null"
        is Boolean -> "boolean"
        is String -> "string"
        is Number -> "number"
        is List<*> -> "array"
        is Map<*, *> -> "object"
        else -> "object"
    }
}

// String
internal fun evaluateConcat(expression: List<*>, context: EvaluationContext, evaluator: ExpressionEvaluator): String {
    return (1 until expression.size).joinToString("") {
        evaluator.evaluate(expression[it], context)?.toString() ?: ""
    }
}

internal fun evaluateUpDownCase(expression: List<*>, context: EvaluationContext, evaluator: ExpressionEvaluator, up: Boolean): String? {
    if (expression.size != 2) return null
    val str = evaluator.evaluate(expression[1], context) as? String ?: return null
    return if (up) str.uppercase() else str.lowercase()
}

// Color
internal fun evaluateRgb(expression: List<*>, context: EvaluationContext, evaluator: ExpressionEvaluator): Color? {
    if (expression.size < 4 || expression.size > 5) return null
    val r = toDouble(evaluator.evaluate(expression[1], context))?.toInt()?.coerceIn(0, 255) ?: return null
    val g = toDouble(evaluator.evaluate(expression[2], context))?.toInt()?.coerceIn(0, 255) ?: return null
    val b = toDouble(evaluator.evaluate(expression[3], context))?.toInt()?.coerceIn(0, 255) ?: return null
    val a = if (expression.size == 5) toDouble(evaluator.evaluate(expression[4], context))?.coerceIn(0.0, 1.0) ?: 1.0 else 1.0
    return Color(r, g, b, (a * 255).toInt())
}

// Math
internal fun evaluateNumber(expression: List<*>, context: EvaluationContext, evaluator: ExpressionEvaluator): Double? {
    if (expression.size < 3) return null
    val op = expression[0] as String
    var result = toDouble(evaluator.evaluate(expression[1], context)) ?: return null
    for (i in 2 until expression.size) {
        val num = toDouble(evaluator.evaluate(expression[i], context)) ?: return null
        result = when (op) {
            "+" -> result + num
            "-" -> result - num
            "*" -> result * num
            "/" -> if (num != 0.0) result / num else return null
            else -> return null
        }
    }
    return result
}

// Interpolation
internal fun evaluateStep(expression: List<*>, context: EvaluationContext, evaluator: ExpressionEvaluator): Any? {
    if (expression.size < 4) return null
    val input = toDouble(evaluator.evaluate(expression[1], context)) ?: return null
    var output: Any? = evaluator.evaluate(expression[2], context)
    for (i in 3 until expression.size step 2) {
        val stop = toDouble(expression[i]) ?: continue
        if (input >= stop) {
            output = evaluator.evaluate(expression[i + 1], context)
        } else {
            break
        }
    }
    return output
}

internal fun evaluateInterpolate(expression: List<*>, context: EvaluationContext, evaluator: ExpressionEvaluator): Any? {
    if (expression.size < 5) return null
    val interpolation = expression[1] as? List<*> ?: return null
    val type = interpolation[0] as? String
    val base = if (type == "exponential") toDouble(interpolation.getOrNull(1)) ?: 1.0 else 1.0
    val input = toDouble(evaluator.evaluate(expression[2], context)) ?: return null

    val stops = expression.subList(3, expression.size)
    if (stops.size % 2 != 0) return null

    val stopInputs = (0 until stops.size step 2).mapNotNull { toDouble(stops[it]) }
    val stopOutputs = (1 until stops.size step 2).map { evaluator.evaluate(stops[it], context) }

    if (input <= stopInputs.first()) return stopOutputs.first()
    if (input >= stopInputs.last()) return stopOutputs.last()

    val index = stopInputs.indexOfFirst { it > input } - 1
    if (index < 0) return stopOutputs.first()

    val lowerBound = stopInputs[index]
    val upperBound = stopInputs[index + 1]
    val lowerOutput = stopOutputs[index]
    val upperOutput = stopOutputs[index + 1]

    val progress = (input - lowerBound) / (upperBound - lowerBound)

    return when {
        lowerOutput is Double && upperOutput is Double -> {
            when (type) {
                "linear", "exponential" -> exponentialInterpolate(progress, base, lowerOutput, upperOutput)
                // "cubic-bezier" is more complex, linear for now
                else -> linearInterpolate(progress, lowerOutput, upperOutput)
            }
        }
        lowerOutput is Color && upperOutput is Color -> {
            val r = linearInterpolate(progress, lowerOutput.red.toDouble(), upperOutput.red.toDouble()).toInt()
            val g = linearInterpolate(progress, lowerOutput.green.toDouble(), upperOutput.green.toDouble()).toInt()
            val b = linearInterpolate(progress, lowerOutput.blue.toDouble(), upperOutput.blue.toDouble()).toInt()
            val a = linearInterpolate(progress, lowerOutput.alpha.toDouble(), upperOutput.alpha.toDouble()).toInt()
            Color(r, g, b, a)
        }
        else -> lowerOutput // Not interpolatable, return lower
    }
}
