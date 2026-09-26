package com.rafambn.kmap.style.expression

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import com.rafambn.kmap.style.EvaluationContext
import com.rafambn.kmap.style.ExpressionEvaluator
import com.rafambn.kmap.style.FormattedText
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.pow

internal fun evaluateFormat(expression: List<*>, context: EvaluationContext, evaluator: ExpressionEvaluator): FormattedText? {
    if (expression.size < 3 || expression.size % 2 == 0) return null
    val sections = mutableListOf<FormattedText.Section>()
    for (index in 1 until expression.size step 2) {
        val value = evaluator.evaluate(expression[index], context)
        val options = expression[index + 1] as? Map<*, *> ?: return null
        when (value) {
            is String -> {
                val fontScale = options["font-scale"]?.let { evaluator.evaluate(it, context) as? Number }?.toDouble()
                val colorValue = options["text-color"]?.let { evaluator.evaluate(it, context) }
                val color = when (colorValue) {
                    is Color -> colorValue
                    is String -> parseColor(colorValue)
                    else -> null
                }
                val fonts = (options["text-font"]?.let { evaluator.evaluate(it, context) } as? List<*>)?.mapNotNull { it as? String }
                sections.add(FormattedText.Section.Text(value, fontScale, color, fonts))
            }
            is ImageBitmap -> sections.add(FormattedText.Section.Image(value))
            else -> return null
        }
    }
    return FormattedText(sections)
}

internal fun evaluateNumberFormat(expression: List<*>, context: EvaluationContext, evaluator: ExpressionEvaluator): String? {
    if (expression.size != 3) return null
    val value = (evaluator.evaluate(expression[1], context) as? Number)?.toDouble() ?: return null
    if (!value.isFinite()) return null
    val options = expression[2] as? Map<*, *> ?: return null
    val localeTag = options["locale"]?.let { evaluator.evaluate(it, context) } as? String ?: context.locale
    val locale = localeTag.substringBefore('-')
    val currency = options["currency"]?.let { evaluator.evaluate(it, context) } as? String
    val unit = options["unit"]?.let { evaluator.evaluate(it, context) } as? String
    val defaultDigits = if (currency == "JPY") 0 else if (currency != null) 2 else 3
    val minimum = ((options["min-fraction-digits"]?.let { evaluator.evaluate(it, context) } as? Number)?.toInt()
        ?: if (currency != null) defaultDigits else 0).coerceIn(0, 20)
    val maximum = ((options["max-fraction-digits"]?.let { evaluator.evaluate(it, context) } as? Number)?.toInt()
        ?: defaultDigits.coerceAtLeast(minimum)).coerceIn(minimum, 20)
    val factor = 10.0.pow(maximum)
    val scaled = abs(value) * factor
    val rounded = if (scaled.isFinite()) floor(scaled + 0.5) / factor else abs(value)
    val fixed = expandScientific(rounded.toString())
    val integer = fixed.substringBefore('.')
    val fraction = fixed.substringAfter('.', "").padEnd(maximum, '0').trimEnd('0').padEnd(minimum, '0')
    val grouping = when (locale) {
        "pt", "de", "es", "it" -> "."
        "fr", "ru" -> "\u00a0"
        else -> ","
    }
    val decimal = if (locale in setOf("pt", "de", "es", "it", "fr", "ru")) "," else "."
    val grouped = integer.reversed().chunked(3).joinToString(grouping).reversed()
    val sign = if (value < 0) "-" else ""
    val number = grouped + if (fraction.isEmpty()) "" else decimal + fraction
    if (currency != null) {
        val symbol = when (currency) {
            "USD" -> "$"
            "EUR" -> "€"
            "BRL" -> "R$"
            "GBP" -> "£"
            "JPY" -> "¥"
            else -> currency
        }
        return when {
            locale == "pt" && localeTag.endsWith("BR", ignoreCase = true) -> "$sign$symbol\u00a0$number"
            locale in setOf("de", "es", "it", "fr", "ru", "pt") -> "$sign$number\u00a0$symbol"
            else -> "$sign$symbol$number"
        }
    }
    return sign + if (unit == null) number else "$number $unit"
}

private fun expandScientific(value: String): String {
    val exponentIndex = value.indexOf('E')
    if (exponentIndex < 0) return value
    val mantissa = value.substring(0, exponentIndex)
    val exponent = value.substring(exponentIndex + 1).toIntOrNull() ?: return value
    val digits = mantissa.filter { it != '.' }
    val decimalIndex = mantissa.indexOf('.').let { if (it < 0) mantissa.length else it } + exponent
    return when {
        decimalIndex <= 0 -> "0." + "0".repeat(-decimalIndex) + digits
        decimalIndex >= digits.length -> digits + "0".repeat(decimalIndex - digits.length)
        else -> digits.substring(0, decimalIndex) + "." + digits.substring(decimalIndex)
    }
}
