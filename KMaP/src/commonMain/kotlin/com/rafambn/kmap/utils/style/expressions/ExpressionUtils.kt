package com.rafambn.kmap.utils.style.expressions

import com.rafambn.kmap.utils.style.Color
import kotlin.math.pow

internal fun toDouble(value: Any?): Double? {
    return when (value) {
        is Number -> value.toDouble()
        is String -> value.toDoubleOrNull()
        else -> null
    }
}

internal fun toColor(value: Any?): Color? {
    if (value is Color) return value
    if (value !is List<*>) return null
    if (value.size < 3 || value.size > 4) return null
    val r = toDouble(value[0])?.toInt()?.coerceIn(0, 255) ?: return null
    val g = toDouble(value[1])?.toInt()?.coerceIn(0, 255) ?: return null
    val b = toDouble(value[2])?.toInt()?.coerceIn(0, 255) ?: return null
    val a = if (value.size == 4) toDouble(value[3])?.coerceIn(0.0, 1.0) ?: 1.0 else 1.0
    return Color(r, g, b, (a * 255).toInt())
}


internal fun compare(a: Any?, b: Any?): Int? {
    if (a == null || b == null) return null
    if (a is String && b is String) return a.compareTo(b)
    val aNum = toDouble(a)
    val bNum = toDouble(b)
    if (aNum != null && bNum != null) return aNum.compareTo(bNum)
    return null
}

internal fun linearInterpolate(t: Double, from: Double, to: Double): Double {
    return from * (1 - t) + to * t
}

internal fun exponentialInterpolate(t: Double, base: Double, from: Double, to: Double): Double {
    return if (base == 1.0) {
        linearInterpolate(t, from, to)
    } else {
        from + (to - from) * (base.pow(t) - 1) / (base - 1)
    }
}

internal fun parseColor(colorString: String): Color? {
    if (colorString.startsWith("#")) {
        val hexColor = colorString.substring(1)
        val red = hexColor.substring(0, 2).toIntOrNull(16) ?: return null
        val green = hexColor.substring(2, 4).toIntOrNull(16) ?: return null
        val blue = hexColor.substring(4, 6).toIntOrNull(16) ?: return null
        val alpha = if (hexColor.length == 8) hexColor.substring(6, 8).toIntOrNull(16) ?: 255 else 255
        return Color(red, green, blue, alpha)
    }
    // TODO: Handle rgba(r,g,b,a) and hsla(h,s,l,a) strings
    return null
}
