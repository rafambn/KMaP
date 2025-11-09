package com.rafambn.kmap.utils.style.expressions

import androidx.compose.ui.graphics.Color
import kotlin.math.pow

internal fun toDouble(value: Any?): Double? {
    return when (value) {
        is Number -> value.toDouble()
        is String -> value.toDoubleOrNull()
        else -> null
    }
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
    return when {
        colorString.startsWith("#") -> parseHexColor(colorString)
        colorString.startsWith("rgb") -> parseRgbColor(colorString)
        colorString.startsWith("hsl") -> parseHslColor(colorString)
        else -> null
    }
}

private fun parseHexColor(hexString: String): Color? {
    val hexColor = hexString.substring(1)
    val red = hexColor.substring(0, 2).toIntOrNull(16) ?: return null
    val green = hexColor.substring(2, 4).toIntOrNull(16) ?: return null
    val blue = hexColor.substring(4, 6).toIntOrNull(16) ?: return null
    val alpha = if (hexColor.length == 8) hexColor.substring(6, 8).toIntOrNull(16) ?: 255 else 255
    return Color(red, green, blue, alpha)
}

private fun parseRgbColor(rgbString: String): Color? {
    // Parse rgb(r,g,b) or rgba(r,g,b,a)
    val content = rgbString.substringAfter("(").substringBefore(")")
    val parts = content.split(",").map { it.trim() }

    if (parts.size < 3) return null

    val r = parts[0].toIntOrNull()?.coerceIn(0, 255) ?: return null
    val g = parts[1].toIntOrNull()?.coerceIn(0, 255) ?: return null
    val b = parts[2].toIntOrNull()?.coerceIn(0, 255) ?: return null
    val a = if (parts.size == 4) {
        ((parts[3].toDoubleOrNull()?.coerceIn(0.0, 1.0) ?: (1.0 * 255))).toInt()
    } else 255

    return Color(r, g, b, a)
}

private fun parseHslColor(hslString: String): Color? {
    // Parse hsl(h,s%,l%) or hsla(h,s%,l%,a)
    val content = hslString.substringAfter("(").substringBefore(")") ?: return null
    val parts = content.split(",").map { it.trim() }

    if (parts.size < 3) return null

    val h = parts[0].toDoubleOrNull() ?: return null
    val s = parts[1].removeSuffix("%").toDoubleOrNull()?.coerceIn(0.0, 100.0) ?: return null
    val l = parts[2].removeSuffix("%").toDoubleOrNull()?.coerceIn(0.0, 100.0) ?: return null
    val a = if (parts.size == 4) {
        parts[3].toDoubleOrNull()?.coerceIn(0.0, 1.0) ?: 1.0
    } else 1.0

    // Convert HSL to RGB
    val hNorm = (h % 360 + 360) % 360 / 360.0
    val sNorm = s / 100.0
    val lNorm = l / 100.0

    val c = (1 - kotlin.math.abs(2 * lNorm - 1)) * sNorm
    val hPrime = hNorm * 6
    val x = c * (1 - kotlin.math.abs((hPrime % 2) - 1))

    val (rPrime, gPrime, bPrime) = when {
        hPrime < 1 -> Triple(c, x, 0.0)
        hPrime < 2 -> Triple(x, c, 0.0)
        hPrime < 3 -> Triple(0.0, c, x)
        hPrime < 4 -> Triple(0.0, x, c)
        hPrime < 5 -> Triple(x, 0.0, c)
        else -> Triple(c, 0.0, x)
    }

    val m = lNorm - c / 2
    val r = ((rPrime + m) * 255).toInt().coerceIn(0, 255)
    val g = ((gPrime + m) * 255).toInt().coerceIn(0, 255)
    val b = ((bPrime + m) * 255).toInt().coerceIn(0, 255)
    val alpha = (a * 255).toInt().coerceIn(0, 255)

    return Color(r, g, b, alpha)
}
