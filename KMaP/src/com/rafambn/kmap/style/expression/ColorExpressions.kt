package com.rafambn.kmap.style.expression

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import com.rafambn.kmap.style.EvaluationContext
import com.rafambn.kmap.style.ExpressionEvaluator
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

internal fun evaluateColorComponents(expression: List<*>, context: EvaluationContext, evaluator: ExpressionEvaluator): List<Double>? {
    if (expression.size != 2) return null
    val value = evaluator.evaluate(expression[1], context)
    val color = when (value) {
        is Color -> value.convert(ColorSpaces.Srgb)
        is String -> parseColor(value)?.convert(ColorSpaces.Srgb)
        else -> null
    } ?: return null
    if (expression[0] == "to-rgba") {
        return listOf(color.red * 255.0, color.green * 255.0, color.blue * 255.0, color.alpha.toDouble())
    }
    val red = color.red.toDouble()
    val green = color.green.toDouble()
    val blue = color.blue.toDouble()
    val top = max(red, max(green, blue))
    val bottom = min(red, min(green, blue))
    val chroma = top - bottom
    val lightness = (top + bottom) / 2.0
    val saturation = if (chroma == 0.0) 0.0 else chroma / (1.0 - abs(2.0 * lightness - 1.0))
    val hue = when (top) {
        red -> if (chroma == 0.0) 0.0 else ((green - blue) / chroma) % 6.0
        green -> (blue - red) / chroma + 2.0
        else -> (red - green) / chroma + 4.0
    }
    return listOf((hue * 60.0 + 360.0) % 360.0, saturation * 100.0, lightness * 100.0, color.alpha.toDouble())
}

internal fun evaluatePerceptualInterpolate(expression: List<*>, context: EvaluationContext, evaluator: ExpressionEvaluator): Color? {
    if (expression.size < 7 || expression.size % 2 == 0) return null
    val interpolation = expression[1] as? List<*> ?: return null
    val input = (evaluator.evaluate(expression[2], context) as? Number)?.toDouble() ?: return null
    val stops = (3 until expression.size step 2).map { index ->
        val position = (expression[index] as? Number)?.toDouble() ?: return null
        val value = evaluator.evaluate(expression[index + 1], context)
        val color = when (value) {
            is Color -> value
            is String -> parseColor(value)
            else -> null
        } ?: return null
        position to color
    }
    if (stops.zipWithNext().any { (a, b) -> a.first >= b.first }) return null
    if (input <= stops.first().first) return stops.first().second
    if (input >= stops.last().first) return stops.last().second
    val upperIndex = stops.indexOfFirst { it.first > input }
    val lower = stops[upperIndex - 1]
    val upper = stops[upperIndex]
    val fraction = interpolationFraction(interpolation, input, lower.first, upper.first) ?: return null
    val a = lower.second.convert(ColorSpaces.CieLab)
    val b = upper.second.convert(ColorSpaces.CieLab)
    val lightness = linearInterpolate(fraction, a.red.toDouble(), b.red.toDouble()).toFloat()
    val alpha = linearInterpolate(fraction, a.alpha.toDouble(), b.alpha.toDouble()).toFloat()
    val (labA, labB) = if (expression[0] == "interpolate-hcl") {
        val firstChroma = sqrt(a.green * a.green + a.blue * a.blue)
        val secondChroma = sqrt(b.green * b.green + b.blue * b.blue)
        val chroma = linearInterpolate(fraction, firstChroma.toDouble(), secondChroma.toDouble())
        val firstHue = atan2(a.blue, a.green).toDouble()
        val secondHue = atan2(b.blue, b.green).toDouble()
        val startHue = if (firstChroma < 1e-6) secondHue else firstHue
        val endHue = if (secondChroma < 1e-6) startHue else secondHue
        val difference = ((endHue - startHue + PI * 3) % (PI * 2)) - PI
        val hue = startHue + fraction * difference
        (cos(hue) * chroma).toFloat() to (sin(hue) * chroma).toFloat()
    } else {
        linearInterpolate(fraction, a.green.toDouble(), b.green.toDouble()).toFloat() to
            linearInterpolate(fraction, a.blue.toDouble(), b.blue.toDouble()).toFloat()
    }
    return Color(lightness, labA, labB, alpha, ColorSpaces.CieLab).convert(ColorSpaces.Srgb)
}

private fun interpolationFraction(interpolation: List<*>, input: Double, lower: Double, upper: Double): Double? {
    val progress = (input - lower) / (upper - lower)
    return when (interpolation.firstOrNull()) {
        "linear" -> progress
        "exponential" -> {
            val base = (interpolation.getOrNull(1) as? Number)?.toDouble() ?: return null
            if (base == 1.0) progress else (base.pow(input - lower) - 1.0) / (base.pow(upper - lower) - 1.0)
        }
        "cubic-bezier" -> {
            if (interpolation.size != 5) return null
            val x1 = (interpolation[1] as? Number)?.toDouble() ?: return null
            val y1 = (interpolation[2] as? Number)?.toDouble() ?: return null
            val x2 = (interpolation[3] as? Number)?.toDouble() ?: return null
            val y2 = (interpolation[4] as? Number)?.toDouble() ?: return null
            var left = 0.0
            var right = 1.0
            repeat(24) {
                val middle = (left + right) / 2.0
                val x = cubicBezier(middle, x1, x2)
                if (x < progress) left = middle else right = middle
            }
            cubicBezier((left + right) / 2.0, y1, y2)
        }
        else -> null
    }
}

private fun cubicBezier(t: Double, control1: Double, control2: Double): Double {
    val inverse = 1.0 - t
    return 3.0 * inverse * inverse * t * control1 + 3.0 * inverse * t * t * control2 + t * t * t
}
