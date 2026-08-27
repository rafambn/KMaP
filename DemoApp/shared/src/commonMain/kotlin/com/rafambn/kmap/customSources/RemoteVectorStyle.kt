package com.rafambn.kmap.customSources

import androidx.compose.ui.graphics.Color
import com.rafambn.kmap.utils.style.CompiledLayout
import com.rafambn.kmap.utils.style.CompiledPaint
import com.rafambn.kmap.utils.style.CompiledValue
import com.rafambn.kmap.utils.style.OptimizedStyle
import com.rafambn.kmap.utils.style.OptimizedStyleLayer
import com.rafambn.kmap.utils.style.Source

fun remoteVectorStyle(): OptimizedStyle = OptimizedStyle(
    version = 8,
    name = "Remote vector tiles",
    sources = mapOf(
        "maptiler_planet_v4" to Source(
            type = "vector",
            url = "https://api.maptiler.com/tiles/v4/tiles.json",
        ),
    ),
    layers = listOf(
        backgroundLayer("background", Color(0xFFF7F4E9)),
        fillLayer("residential", "residential", Color(0xFFE9E4DA)),
        fillLayer("vegetation", "vegetation", Color(0xFFDDE9C9)),
        fillLayer("grass", "grass", Color(0xFFD4E8C2)),
        fillLayer("forest", "forest", Color(0xFFCADFBA)),
        fillLayer("water", "water", Color(0xFF8FD3F4)),
        lineLayer("waterway", "waterway", Color(0xFF78C4EA), width = 1.5),
        lineLayer("railway", "railway", Color(0xFFAAA59D), width = 1.2),
        lineLayer("road-casing", "road", Color(0xFFD2C9BC), width = 4.0),
        lineLayer("road", "road", Color(0xFFFFFFFF), width = 2.5),
        symbolLayer("water-label", "water_label", Color(0xFF267BA8)),
        symbolLayer("place-label", "place_label", Color(0xFF3E3A35)),
    ),
)

private fun backgroundLayer(id: String, color: Color) = OptimizedStyleLayer(
    id = id,
    type = "background",
    source = null,
    sourceLayer = null,
    minZoom = 0.0,
    maxZoom = 24.0,
    filter = null,
    layout = visibleLayout(),
    paint = CompiledPaint(
        properties = mapOf(
            "background-color" to constantValue(color),
            "background-opacity" to constantValue(1f),
        ),
    ),
)

private fun fillLayer(id: String, sourceLayer: String, color: Color) = OptimizedStyleLayer(
    id = id,
    type = "fill",
    source = "maptiler_planet_v4",
    sourceLayer = sourceLayer,
    minZoom = 0.0,
    maxZoom = 24.0,
    filter = null,
    layout = visibleLayout(),
    paint = CompiledPaint(
        properties = mapOf(
            "fill-color" to constantValue(color),
            "fill-opacity" to constantValue(1.0),
        ),
    ),
)

private fun lineLayer(
    id: String,
    sourceLayer: String,
    color: Color,
    width: Double,
) = OptimizedStyleLayer(
    id = id,
    type = "line",
    source = "maptiler_planet_v4",
    sourceLayer = sourceLayer,
    minZoom = 0.0,
    maxZoom = 24.0,
    filter = null,
    layout = visibleLayout(
        properties = mapOf(
            "line-cap" to constantValue("round"),
            "line-join" to constantValue("round"),
        ),
    ),
    paint = CompiledPaint(
        properties = mapOf(
            "line-color" to constantValue(color),
            "line-width" to constantValue(width),
            "line-opacity" to constantValue(1.0),
        ),
    ),
)

private fun symbolLayer(id: String, sourceLayer: String, color: Color) = OptimizedStyleLayer(
    id = id,
    type = "symbol",
    source = "maptiler_planet_v4",
    sourceLayer = sourceLayer,
    minZoom = 0.0,
    maxZoom = 24.0,
    filter = null,
    layout = visibleLayout(
        properties = mapOf(
            "text-field" to CompiledValue<String>(
                evaluate = { _, properties, _ ->
                    properties["name:pt"] as? String ?: properties["name"] as? String
                },
                requiredProperties = setOf("name:pt", "name"),
            ),
            "text-size" to constantValue(13.0),
            "text-max-width" to constantValue(10.0),
        ),
    ),
    paint = CompiledPaint(
        properties = mapOf(
            "text-color" to constantValue(color),
            "text-opacity" to constantValue(1.0),
            "text-halo-color" to constantValue(Color.White),
            "text-halo-width" to constantValue(1.0),
        ),
    ),
)

private fun visibleLayout(
    properties: Map<String, CompiledValue<*>> = emptyMap(),
) = CompiledLayout(
    visibility = constantValue(true),
    properties = properties,
)

private fun <T> constantValue(value: T) = CompiledValue<T>(
    evaluate = { _, _, _ -> value },
    requiredProperties = emptySet(),
)
