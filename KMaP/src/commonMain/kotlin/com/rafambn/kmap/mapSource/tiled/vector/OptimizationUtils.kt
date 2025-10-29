package com.rafambn.kmap.mapSource.tiled.vector

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path as ComposePath
import com.rafambn.kmap.utils.style.StyleLayer
import com.rafambn.kmap.utils.vectorTile.MVTFeature
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.abs

internal fun parseColor(element: JsonElement?, defaultColor: Color): Color {
    return when {
        element is JsonPrimitive && element.isString -> {
            val colorStr = element.content.trim()
            when {
                colorStr.startsWith("#") -> parseHexColor(colorStr)
                colorStr.startsWith("rgb") -> parseRgbColor(colorStr)
                colorStr.startsWith("hsl") -> parseHslColor(colorStr)
                else -> parseNamedColor(colorStr) ?: defaultColor
            }
        }

        else -> defaultColor
    }
}

internal fun extractColorProperty(
    paint: Map<String, JsonElement>?,
    propertyName: String,
    defaultColor: Color
): Color {
    paint?.get(propertyName)?.let { element ->
        return parseColor(element, defaultColor)
    }
    return defaultColor
}

internal fun extractOpacityProperty(
    paint: Map<String, JsonElement>?,
    propertyName: String,
    defaultOpacity: Double = 1.0
): Double {
    paint?.get(propertyName)?.let { element ->
        return when {
            element is JsonPrimitive && element.isString -> {
                element.content.toDoubleOrNull() ?: defaultOpacity
            }

            element is JsonPrimitive && !element.isString -> {
                element.jsonPrimitive.content.toDoubleOrNull() ?: defaultOpacity
            }

            else -> defaultOpacity
        }
    }
    return defaultOpacity
}

internal fun extractNumberProperty(
    properties: Map<String, JsonElement>?,
    propertyName: String,
    defaultValue: Double
): Double {
    properties?.get(propertyName)?.let { element ->
        return when {
            element is JsonPrimitive && !element.isString -> {
                element.jsonPrimitive.content.toDoubleOrNull() ?: defaultValue
            }

            else -> defaultValue
        }
    }
    return defaultValue
}

internal fun extractStringProperty(
    properties: Map<String, JsonElement>?,
    propertyName: String,
    defaultValue: String
): String {
    properties?.get(propertyName)?.let { element ->
        return when {
            element is JsonPrimitive && element.isString -> element.content
            else -> defaultValue
        }
    }
    return defaultValue
}

private fun parseHexColor(hex: String): Color {
    val cleanHex = hex.removePrefix("#")
    return when (cleanHex.length) {
        3 -> { // #RGB
            val r = cleanHex[0].toString().repeat(2).toInt(16)
            val g = cleanHex[1].toString().repeat(2).toInt(16)
            val b = cleanHex[2].toString().repeat(2).toInt(16)
            Color(red = r, green = g, blue = b)
        }

        6 -> { // #RRGGBB
            val r = cleanHex.substring(0, 2).toInt(16)
            val g = cleanHex.substring(2, 4).toInt(16)
            val b = cleanHex.substring(4, 6).toInt(16)
            Color(red = r, green = g, blue = b)
        }

        8 -> { // #RRGGBBAA
            val r = cleanHex.substring(0, 2).toInt(16)
            val g = cleanHex.substring(2, 4).toInt(16)
            val b = cleanHex.substring(4, 6).toInt(16)
            val a = cleanHex.substring(6, 8).toInt(16)
            Color(red = r, green = g, blue = b, alpha = a)
        }

        else -> Color.Black
    }
}

private fun parseRgbColor(rgbStr: String): Color {
    val values = rgbStr
        .removePrefix("rgb(").removePrefix("rgba(").removeSuffix(")")
        .split(",")
        .map { it.trim().toIntOrNull() ?: 0 }

    return when {
        values.size >= 4 -> Color(red = values[0], green = values[1], blue = values[2], alpha = values[3])
        values.size >= 3 -> Color(red = values[0], green = values[1], blue = values[2])
        else -> Color.Black
    }
}

private fun parseHslColor(hslStr: String): Color {
    val values = hslStr
        .removePrefix("hsl(").removePrefix("hsla(").removeSuffix(")")
        .split(",")
        .map { it.trim().replace("%", "").toDoubleOrNull() ?: 0.0 }

    if (values.size < 3) return Color.Black

    val h = values[0] / 360.0
    val s = values[1] / 100.0
    val l = values[2] / 100.0
    val alpha = if (values.size >= 4) (values[3] * 255).toInt() else 255

    val (r, g, b) = hslToRgb(h, s, l)
    return Color(red = r, green = g, blue = b, alpha = alpha)
}

private fun hslToRgb(h: Double, s: Double, l: Double): Triple<Int, Int, Int> {
    val c = (1 - abs(2 * l - 1)) * s
    val hPrime = h * 6
    val x = c * (1 - abs((hPrime % 2) - 1))

    val (rPrime, gPrime, bPrime) = when {
        hPrime < 1 -> Triple(c, x, 0.0)
        hPrime < 2 -> Triple(x, c, 0.0)
        hPrime < 3 -> Triple(0.0, c, x)
        hPrime < 4 -> Triple(0.0, x, c)
        hPrime < 5 -> Triple(x, 0.0, c)
        else -> Triple(c, 0.0, x)
    }

    val m = l - c / 2
    return Triple(
        ((rPrime + m) * 255).toInt(),
        ((gPrime + m) * 255).toInt(),
        ((bPrime + m) * 255).toInt()
    )
}

private fun parseNamedColor(name: String): Color? {
    return when (name.lowercase()) {
        "black" -> Color.Black
        "white" -> Color.White
        "red" -> Color.Red
        "green" -> Color.Green
        "blue" -> Color.Blue
        "yellow" -> Color.Yellow
        "cyan" -> Color.Cyan
        "magenta" -> Color.Magenta
        "gray", "grey" -> Color.Gray
        "transparent" -> Color.Transparent
        else -> null
    }
}

internal fun buildPathFromGeometry(
    geometry: List<List<Pair<Int, Int>>>,
    extent: Int,
    scaleAdjustment: Float = 1.0f
): ComposePath {
    val path = ComposePath()

    geometry.forEach { ring ->
        if (ring.isEmpty()) return@forEach

        val (startX, startY) = ring.first()
        val startCanvasX = (startX.toFloat() / extent) * scaleAdjustment
        val startCanvasY = (startY.toFloat() / extent) * scaleAdjustment

        path.moveTo(startCanvasX, startCanvasY)

        ring.drop(1).forEach { (x, y) ->
            val canvasX = (x.toFloat() / extent) * scaleAdjustment
            val canvasY = (y.toFloat() / extent) * scaleAdjustment
            path.lineTo(canvasX, canvasY)
        }

        path.close()
    }

    return path
}

/**
 * Checks if a feature matches the layer filter criteria
 * Simplified implementation supporting basic equality filters
 */
internal fun matchesFilter(feature: MVTFeature, filter: List<JsonElement>?): Boolean {
    if (filter == null) return true
    if (filter.isEmpty()) return true

    //TODO implement this logic

    // Simplified filter matching - in a full implementation, this would parse
    // complex filter expressions from the Mapbox style spec:
    // Examples: ["==", "class", "water"], ["in", "class", "water", "ocean"]
    // For now, we always return true to render all features
    return true
}

internal fun isLayerVisibleAtZoom(styleLayer: StyleLayer, zoomLevel: Int): Boolean {
    if (styleLayer.minzoom != null && zoomLevel < styleLayer.minzoom) return false
    if (styleLayer.maxzoom != null && zoomLevel >= styleLayer.maxzoom) return false
    return true
}
