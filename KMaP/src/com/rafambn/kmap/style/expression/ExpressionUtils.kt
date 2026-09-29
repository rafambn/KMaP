package com.rafambn.kmap.style.expression

import androidx.compose.ui.graphics.Color
import kotlin.math.pow
import kotlin.math.roundToInt

internal fun numberToDouble(value: Any?): Double? = when (value) {
    is ULong -> value.toDouble()
    is Number -> value.toDouble()
    else -> null
}

internal fun toDouble(value: Any?): Double? =
    if (value is String) value.toDoubleOrNull() else numberToDouble(value)

internal fun equalValues(a: Any?, b: Any?): Boolean = when {
    a is ULong && b is ULong -> a == b
    a is ULong && b is Number -> compareUnsigned(a, b) == 0
    a is Number && b is ULong -> compareUnsigned(b, a) == 0
    a is Number && b is Number -> {
        a.exactInteger()?.let { compareSigned(it, b) == 0 }
            ?: b.exactInteger()?.let { compareSigned(it, a) == 0 }
            ?: (a.toDouble() == b.toDouble())
    }
    else -> a == b
}

private fun Number.exactInteger(): Long? = when (this) {
    is Byte, is Short, is Int, is Long -> toLong()
    else -> null
}

private fun compareUnsigned(value: ULong, other: Number): Int? {
    other.exactInteger()?.let { signed ->
        return if (signed < 0) 1 else value.compareTo(signed.toULong())
    }
    val number = other.toDouble()
    return when {
        number.isNaN() -> null
        number < 0 -> 1
        number >= 18446744073709551616.0 -> -1
        value == 0UL && number == 0.0 -> 0
        number < 9007199254740992.0 -> value.toDouble().compareTo(number)
        else -> value.compareTo(number.toULong())
    }
}

private fun compareSigned(value: Long, other: Number): Int? {
    other.exactInteger()?.let { return value.compareTo(it) }
    val number = other.toDouble()
    return when {
        number.isNaN() -> null
        number < Long.MIN_VALUE.toDouble() -> 1
        number >= 9223372036854775808.0 -> -1
        value == 0L && number == 0.0 -> 0
        else -> {
            val integer = number.toLong()
            value.compareTo(integer).takeIf { it != 0 } ?: integer.toDouble().compareTo(number)
        }
    }
}

internal fun styleValueToString(value: Any?): String {
    if (value == null) return ""
    if (value is Float || value is Double) {
        val number = (value as Number).toDouble()
        if (number == 0.0) return "0"
        if (number % 1.0 == 0.0 && number >= Long.MIN_VALUE.toDouble() && number < Long.MAX_VALUE.toDouble()) {
            return number.toLong().toString()
        }
    }
    return value.toString()
}

internal fun compare(a: Any?, b: Any?): Int? {
    if (a == null || b == null) return null
    if (a is String && b is String) return a.compareTo(b)
    if (a is ULong && b is ULong) return a.compareTo(b)
    if (a is ULong && b is Number) return compareUnsigned(a, b)
    if (a is Number && b is ULong) return compareUnsigned(b, a)?.let { -it }
    if (a is Number && b is Number) {
        a.exactInteger()?.let { return compareSigned(it, b) }
        b.exactInteger()?.let { return compareSigned(it, a)?.let { comparison -> -comparison } }
    }
    val aNum = toDouble(a)
    val bNum = toDouble(b)
    if (aNum != null && bNum != null) return aNum.compareTo(bNum)
    return null
}

internal fun linearInterpolate(t: Double, from: Double, to: Double): Double {
    return from * (1 - t) + to * t
}

internal fun interpolationFraction(interpolation: List<*>, input: Double, lower: Double, upper: Double): Double? {
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
            if (x1 !in 0.0..1.0 || y1 !in 0.0..1.0 || x2 !in 0.0..1.0 || y2 !in 0.0..1.0) return null
            var left = 0.0
            var right = 1.0
            repeat(24) {
                val middle = (left + right) / 2.0
                if (cubicBezier(middle, x1, x2) < progress) left = middle else right = middle
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

internal fun parseColor(colorString: String): Color? {
    val value = colorString.trim().lowercase()
    if (value == "transparent") return Color.Transparent
    cssNamedColorHex[value]?.let { rgb ->
        return Color((rgb shr 16) and 0xff, (rgb shr 8) and 0xff, rgb and 0xff)
    }
    return when {
        value.startsWith("#") -> parseHexColor(value)
        value.startsWith("rgb(") || value.startsWith("rgba(") -> parseRgbColor(value)
        value.startsWith("hsl(") || value.startsWith("hsla(") -> parseHslColor(value)
        else -> null
    }
}

private fun parseHexColor(hexString: String): Color? {
    val hexColor = hexString.substring(1).let {
        when (it.length) {
            3, 4 -> it.map { digit -> "$digit$digit" }.joinToString("")
            6, 8 -> it
            else -> return null
        }
    }
    val red = hexColor.substring(0, 2).toIntOrNull(16) ?: return null
    val green = hexColor.substring(2, 4).toIntOrNull(16) ?: return null
    val blue = hexColor.substring(4, 6).toIntOrNull(16) ?: return null
    val alpha = if (hexColor.length == 8) hexColor.substring(6, 8).toIntOrNull(16) ?: return null else 255
    return Color(red, green, blue, alpha)
}

private fun parseRgbColor(rgbString: String): Color? {
    if (!rgbString.endsWith(")")) return null
    val content = rgbString.substringAfter("(").dropLast(1)
    val parts = content.split(",").map { it.trim() }
    val hasAlpha = rgbString.startsWith("rgba(")
    if (parts.size != if (hasAlpha) 4 else 3) return null
    val r = parseRgbChannel(parts[0]) ?: return null
    val g = parseRgbChannel(parts[1]) ?: return null
    val b = parseRgbChannel(parts[2]) ?: return null
    val alpha = if (hasAlpha) parseFraction(parts[3]) ?: return null else 1f
    return Color(r / 255f, g / 255f, b / 255f, alpha)
}

private fun parseHslColor(hslString: String): Color? {
    if (!hslString.endsWith(")")) return null
    val content = hslString.substringAfter("(").dropLast(1)
    val parts = content.split(",").map { it.trim() }
    val hasAlpha = hslString.startsWith("hsla(")
    if (parts.size != if (hasAlpha) 4 else 3) return null
    val hue = parts[0].toDoubleOrNull()?.takeIf { it.isFinite() } ?: return null
    val saturation = parseFraction(parts[1]) ?: return null
    val lightness = parseFraction(parts[2]) ?: return null
    val alpha = if (hasAlpha) parseFraction(parts[3]) ?: return null else 1f
    return Color.hsl((((hue % 360) + 360) % 360).toFloat(), saturation, lightness, alpha)
}

private fun parseRgbChannel(value: String): Int? {
    val number = value.removeSuffix("%").toDoubleOrNull()?.takeIf { it.isFinite() } ?: return null
    return if (value.endsWith("%")) (number * 255 / 100).roundToInt().coerceIn(0, 255)
    else number.toInt().coerceIn(0, 255)
}

private fun parseFraction(value: String): Float? {
    val number = value.removeSuffix("%").toDoubleOrNull()?.takeIf { it.isFinite() } ?: return null
    return (if (value.endsWith("%")) number / 100 else number).coerceIn(0.0, 1.0).toFloat()
}
