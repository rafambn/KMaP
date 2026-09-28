package com.rafambn.kmap.style.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement


/**
 * JSON style model based on the Mapbox and MapLibre Style Specifications.
 *
 * KMaP supports many constructs shared by these specifications, but rendering
 * covers a subset. See docs/vector-style-support.md for the current behavior.
 *
 * Mapbox: https://docs.mapbox.com/style-spec/
 * MapLibre: https://maplibre.org/maplibre-style-spec/
 */
@Serializable
data class Style(
    val version: Int? = null,
    val name: String? = null,
    val metadata: Map<String, JsonElement>? = null,
    val center: List<Double>? = null,
    val zoom: Double? = null,
    val bearing: Double? = null,
    val pitch: Double? = null,
    val light: Light? = null,
    val sources: Map<String, Source> = emptyMap(),
    val layers: List<StyleLayer>,
    val sprite: JsonElement? = null,
    val glyphs: String? = null,
    val transition: Transition? = null
)
