package com.rafambn.kmap.style.expression

import androidx.compose.ui.graphics.Color
import com.rafambn.kmap.style.evaluation.EvaluationContext
import com.rafambn.kmap.style.evaluation.ExpressionEvaluator

internal fun evaluateTypeAssertion(expression: List<*>, context: EvaluationContext, evaluator: ExpressionEvaluator): Any? {
    val type = expression.firstOrNull() as? String ?: return null
    if (type == "array") {
        var index = 1
        val itemType = (expression.getOrNull(index) as? String)?.takeIf { it in setOf("string", "number", "boolean") }
        if (itemType != null) index++
        val length = if (itemType != null && expression.getOrNull(index) is Number) {
            val value = (expression[index++] as Number).toDouble()
            if (!value.isFinite() || value < 0 || value % 1.0 != 0.0 || value > Int.MAX_VALUE) return null
            value.toInt()
        } else null
        if (expression.size != index + 1) return null
        val array = evaluator.evaluate(expression[index], context) as? List<*> ?: return null
        if (length != null && array.size != length) return null
        if (itemType != null && array.any { !matchesType(it, itemType) }) return null
        return array
    }

    if (expression.size < 2) return null
    for (operand in expression.drop(1)) {
        val value = if (type == "string" && operand is String) operand else evaluator.evaluate(operand, context)
        if (matchesType(value, type)) return value
    }
    return null
}

private fun matchesType(value: Any?, type: String): Boolean = when (type) {
    "boolean" -> value is Boolean
    "number" -> value is Number
    "object" -> value is Map<*, *>
    "string" -> value is String
    else -> false
}

internal fun evaluateConversion(expression: List<*>, context: EvaluationContext, evaluator: ExpressionEvaluator): Any? {
    val operator = expression.firstOrNull() as? String ?: return null
    if (expression.size < 2 || operator == "to-boolean" && expression.size != 2) return null
    if (operator == "to-boolean") {
        return when (val value = evaluator.evaluate(expression[1], context)) {
            null, false -> false
            is String -> value.isNotEmpty()
            is Number -> value.toDouble() != 0.0 && !value.toDouble().isNaN()
            else -> true
        }
    }
    for (operand in expression.drop(1)) {
        val value = evaluator.evaluate(operand, context)
        when (operator) {
            "to-number" -> when (value) {
                null, false -> return 0.0
                true -> return 1.0
                is Number -> if (value.toDouble().isFinite()) return value.toDouble()
                is String -> {
                    val number = if (value.isBlank()) 0.0 else value.trim().toDoubleOrNull()
                    if (number != null && number.isFinite()) return number
                }
            }
            "to-color" -> when (value) {
                is Color -> return value
                is String -> parseColor(value)?.let { return it }
            }
        }
    }
    return null
}
