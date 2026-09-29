package com.rafambn.kmap.style.expression

import androidx.compose.ui.graphics.Color
import com.rafambn.kmap.style.evaluation.EvaluationContext
import com.rafambn.kmap.style.evaluation.ExpressionEvaluator
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

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
        "==" -> equalValues(left, right)
        "!=" -> !equalValues(left, right)
        ">" -> compare(left, right)?.let { it > 0 }
        "<" -> compare(left, right)?.let { it < 0 }
        ">=" -> compare(left, right)?.let { it >= 0 }
        "<=" -> compare(left, right)?.let { it <= 0 }
        else -> null
    }
}

// Feature data
internal fun evaluateGet(expression: List<*>, context: EvaluationContext, evaluator: ExpressionEvaluator): Any? {
    if (expression.size !in 2..3) return null
    val key = (expression[1] as? String ?: evaluator.evaluate(expression[1], context) as? String) ?: return null
    val properties = if (expression.size == 3) evaluator.evaluate(expression[2], context) as? Map<*, *> ?: return null
    else context.featureProperties
    return properties[key]
}

internal fun evaluateHas(expression: List<*>, context: EvaluationContext, evaluator: ExpressionEvaluator): Boolean {
    if (expression.size !in 2..3) return false
    val key = (expression[1] as? String ?: evaluator.evaluate(expression[1], context) as? String) ?: return false
    val properties = if (expression.size == 3) evaluator.evaluate(expression[2], context) as? Map<*, *> ?: return false
    else context.featureProperties
    return properties.containsKey(key)
}

// Lookup
private fun String.hasSurrogatePairAt(index: Int): Boolean =
    index + 1 < length && this[index] in '\uD800'..'\uDBFF' && this[index + 1] in '\uDC00'..'\uDFFF'

private fun String.codePointCount(end: Int = length): Int {
    var count = 0
    var index = 0
    while (index < end) {
        index += if (hasSurrogatePairAt(index) && index + 1 < end) 2 else 1
        count++
    }
    return count
}

private fun String.utf16Offset(codePointIndex: Int): Int {
    var offset = 0
    var count = 0
    while (count < codePointIndex && offset < length) {
        offset += if (hasSurrogatePairAt(offset)) 2 else 1
        count++
    }
    return offset
}

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
        is List<*> -> collection.any { equalValues(item, it) }
        else -> false
    }
}

internal fun evaluateIndexOf(expression: List<*>, context: EvaluationContext, evaluator: ExpressionEvaluator): Int? {
    if (expression.size !in 3..4) return null
    val item = evaluator.evaluate(expression[1], context)
    val collection = evaluator.evaluate(expression[2], context)
    val start = if (expression.size == 4) toDouble(evaluator.evaluate(expression[3], context))?.toInt() ?: return null else 0
    return when (collection) {
        is String -> (item as? String)?.let {
            val offset = collection.indexOf(it, collection.utf16Offset(start.coerceAtLeast(0)))
            if (offset < 0) -1 else collection.codePointCount(offset)
        }
        is List<*> -> {
            val from = if (start < 0) (collection.size.toLong() + start).coerceAtLeast(0).toInt() else start
            (from until collection.size).firstOrNull { equalValues(item, collection[it]) } ?: -1
        }
        else -> null
    }
}

internal fun evaluateLength(expression: List<*>, context: EvaluationContext, evaluator: ExpressionEvaluator): Int? {
    if (expression.size != 2) return null
    return when (val value = evaluator.evaluate(expression[1], context)) {
        is String -> value.codePointCount()
        is List<*> -> value.size
        else -> null
    }
}

internal fun evaluateSlice(expression: List<*>, context: EvaluationContext, evaluator: ExpressionEvaluator): Any? {
    if (expression.size !in 3..4) return null
    val value = evaluator.evaluate(expression[1], context)
    val from = toDouble(evaluator.evaluate(expression[2], context))?.toInt() ?: return null
    val size = when (value) {
        is String -> value.codePointCount()
        is List<*> -> value.size
        else -> return null
    }
    val to = if (expression.size == 4) toDouble(evaluator.evaluate(expression[3], context))?.toInt() ?: return null else size
    fun bounded(index: Int): Int = if (index < 0) (size.toLong() + index).coerceAtLeast(0).toInt() else index.coerceAtMost(size)
    val start = bounded(from)
    val end = bounded(to).coerceAtLeast(start)
    return when (value) {
        is String -> value.substring(value.utf16Offset(start), value.utf16Offset(end))
        is List<*> -> value.subList(start, end)
        else -> null
    }
}

// Conditional
internal fun evaluateCase(expression: List<*>, context: EvaluationContext, evaluator: ExpressionEvaluator): Any? {
    if (expression.size < 4) return null
    var i = 1
    while (i < expression.size - 1) {
        if (evaluator.evaluate(expression[i], context) == true) {
            return evaluator.evaluate(expression[i + 1], context)
        }
        i += 2
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
    var i = 2
    while (i < expression.size - 1) {
        val label = expression[i]
        if (label is List<*>) {
            if (label.any { equalValues(input, it) }) return evaluator.evaluate(expression[i + 1], context)
        } else if (equalValues(input, label)) {
            return evaluator.evaluate(expression[i + 1], context)
        }
        i += 2
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
    return styleValueToString(evaluator.evaluate(expression[1], context))
}

internal fun evaluateTypeOf(expression: List<*>, context: EvaluationContext, evaluator: ExpressionEvaluator): String {
    val value = evaluator.evaluate(expression[1], context)
    return when (value) {
        null -> "null"
        is Boolean -> "boolean"
        is String -> "string"
        is Number, is ULong -> "number"
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

internal fun evaluateSplit(expression: List<*>, context: EvaluationContext, evaluator: ExpressionEvaluator): List<String>? {
    if (expression.size != 3) return null
    val value = evaluator.evaluate(expression[1], context) as? String ?: return null
    val delimiter = evaluator.evaluate(expression[2], context) as? String ?: return null
    return if (delimiter.isEmpty()) value.map(Char::toString) else value.split(delimiter)
}

// Color
internal fun evaluateRgb(expression: List<*>, context: EvaluationContext, evaluator: ExpressionEvaluator): Color? {
    if (expression.size != if (expression[0] == "rgba") 5 else 4) return null
    val r = toDouble(evaluator.evaluate(expression[1], context))?.takeIf { it.isFinite() && it in 0.0..255.0 } ?: return null
    val g = toDouble(evaluator.evaluate(expression[2], context))?.takeIf { it.isFinite() && it in 0.0..255.0 } ?: return null
    val b = toDouble(evaluator.evaluate(expression[3], context))?.takeIf { it.isFinite() && it in 0.0..255.0 } ?: return null
    val a = if (expression.size == 5) {
        toDouble(evaluator.evaluate(expression[4], context))?.takeIf { it.isFinite() && it in 0.0..1.0 } ?: return null
    } else 1.0
    return Color((r / 255).toFloat(), (g / 255).toFloat(), (b / 255).toFloat(), a.toFloat())
}

internal fun evaluateHsl(expression: List<*>, context: EvaluationContext, evaluator: ExpressionEvaluator): Color? {
    if (expression.size != if (expression[0] == "hsla") 5 else 4) return null
    val h = toDouble(evaluator.evaluate(expression[1], context))?.takeIf { it.isFinite() && it in 0.0..360.0 } ?: return null
    val s = toDouble(evaluator.evaluate(expression[2], context))?.takeIf { it.isFinite() && it in 0.0..100.0 } ?: return null
    val l = toDouble(evaluator.evaluate(expression[3], context))?.takeIf { it.isFinite() && it in 0.0..100.0 } ?: return null
    val a = if (expression.size == 5) {
        toDouble(evaluator.evaluate(expression[4], context))?.takeIf { it.isFinite() && it in 0.0..1.0 } ?: return null
    } else 1.0
    return Color.hsl(h.toFloat(), (s / 100).toFloat(), (l / 100).toFloat(), a.toFloat())
}

// Math
internal fun evaluateNumber(expression: List<*>, context: EvaluationContext, evaluator: ExpressionEvaluator): Double? {
    val op = expression[0] as String
    when (op) {
        "-" -> if (expression.size !in 2..3) return null
        "/" -> if (expression.size != 3) return null
        "+", "*" -> if (expression.size < 3) return null
        else -> return null
    }
    if (expression.size == 2) return numberToDouble(evaluator.evaluate(expression[1], context))?.let { -it }
    var result = numberToDouble(evaluator.evaluate(expression[1], context)) ?: return null
    for (i in 2 until expression.size) {
        val num = numberToDouble(evaluator.evaluate(expression[i], context)) ?: return null
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

internal fun evaluateUnaryMath(expression: List<*>, context: EvaluationContext, evaluator: ExpressionEvaluator): Double? {
    if (expression.size != 2) return null
    val value = numberToDouble(evaluator.evaluate(expression[1], context)) ?: return null
    val result = when (expression[0]) {
        "acos" -> acos(value)
        "asin" -> asin(value)
        "atan" -> atan(value)
        "cos" -> cos(value)
        "sin" -> sin(value)
        "tan" -> tan(value)
        "ln" -> ln(value)
        "log10" -> log10(value)
        "log2" -> log2(value)
        "abs" -> abs(value)
        "ceil" -> ceil(value)
        "floor" -> floor(value)
        "round" -> if (value >= 0) floor(value + 0.5) else ceil(value - 0.5)
        "sqrt" -> sqrt(value)
        else -> return null
    }
    return result.takeIf { it.isFinite() }
}

internal fun evaluateBinaryMath(expression: List<*>, context: EvaluationContext, evaluator: ExpressionEvaluator): Double? {
    if (expression.size < 3) return null
    val values = expression.drop(1).map { numberToDouble(evaluator.evaluate(it, context)) ?: return null }
    val result = when (expression[0]) {
        "%" -> if (values.size == 2 && values[1] != 0.0) values[0] % values[1] else return null
        "^" -> if (values.size == 2) values[0].pow(values[1]) else return null
        "max" -> values.reduce(::max)
        "min" -> values.reduce(::min)
        else -> return null
    }
    return result.takeIf { it.isFinite() }
}

// Interpolation
internal fun evaluateStep(expression: List<*>, context: EvaluationContext, evaluator: ExpressionEvaluator): Any? {
    if (expression.size < 4) return null
    val input = toDouble(evaluator.evaluate(expression[1], context)) ?: return null
    var output: Any? = evaluator.evaluate(expression[2], context)
    var i = 3
    while (i < expression.size) {
        val stop = toDouble(expression[i])
        if (stop == null) {
            i += 2
            continue
        }
        if (input >= stop) {
            output = evaluator.evaluate(expression[i + 1], context)
        } else {
            break
        }
        i += 2
    }
    return output
}

internal fun evaluateInterpolate(expression: List<*>, context: EvaluationContext, evaluator: ExpressionEvaluator): Any? {
    if (expression.size < 5) return null
    val interpolation = expression[1] as? List<*> ?: return null
    val input = toDouble(evaluator.evaluate(expression[2], context)) ?: return null

    val stops = expression.subList(3, expression.size)
    if (stops.size % 2 != 0) return null

    val stopInputs = (0 until stops.size step 2).map { toDouble(stops[it]) ?: return null }
    if (stopInputs.zipWithNext().any { (lower, upper) -> lower >= upper }) return null
    val stopOutputs = (1 until stops.size step 2).map {
        val value = evaluator.evaluate(stops[it], context)
        if (value is String) parseColor(value) ?: value else value
    }

    if (input <= stopInputs.first()) return stopOutputs.first()
    if (input >= stopInputs.last()) return stopOutputs.last()

    val index = stopInputs.indexOfFirst { it > input } - 1
    if (index < 0) return stopOutputs.first()

    val lowerBound = stopInputs[index]
    val upperBound = stopInputs[index + 1]
    val lowerOutput = stopOutputs[index]
    val upperOutput = stopOutputs[index + 1]

    val fraction = interpolationFraction(interpolation, input, lowerBound, upperBound) ?: return null

    val lowerOutNum = toDouble(lowerOutput)
    val upperOutNum = toDouble(upperOutput)

    return when {
        lowerOutNum != null && upperOutNum != null -> linearInterpolate(fraction, lowerOutNum, upperOutNum)
        lowerOutput is List<*> && upperOutput is List<*> && lowerOutput.size == upperOutput.size &&
            lowerOutput.all { numberToDouble(it) != null } && upperOutput.all { numberToDouble(it) != null } -> {
            lowerOutput.indices.map { itemIndex ->
                val from = numberToDouble(lowerOutput[itemIndex]) ?: return null
                val to = numberToDouble(upperOutput[itemIndex]) ?: return null
                linearInterpolate(fraction, from, to)
            }
        }
        (lowerOutput is Color || lowerOutput is String && parseColor(lowerOutput) != null) &&
            (upperOutput is Color || upperOutput is String && parseColor(upperOutput) != null) -> {
            val lowerColor = if (lowerOutput is Color) lowerOutput else parseColor(lowerOutput as String)!!
            val upperColor = if (upperOutput is Color) upperOutput else parseColor(upperOutput as String)!!
            Color(
                linearInterpolate(fraction, lowerColor.red.toDouble(), upperColor.red.toDouble()).toFloat(),
                linearInterpolate(fraction, lowerColor.green.toDouble(), upperColor.green.toDouble()).toFloat(),
                linearInterpolate(fraction, lowerColor.blue.toDouble(), upperColor.blue.toDouble()).toFloat(),
                linearInterpolate(fraction, lowerColor.alpha.toDouble(), upperColor.alpha.toDouble()).toFloat()
            )
        }
        else -> lowerOutput
    }
}
