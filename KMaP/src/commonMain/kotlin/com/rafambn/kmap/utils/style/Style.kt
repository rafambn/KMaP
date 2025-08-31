package com.rafambn.kmap.utils.style

import kotlinx.serialization.SerialName
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement


/**
 * Represents a MapBox/MapLibre style specification.
 * Based on the MapBox Style Specification: https://docs.mapbox.com/mapbox-gl-js/style-spec/
 * And MapLibre Style Specification: https://maplibre.org/maplibre-style-spec/
 */
@Serializable
data class Style(
    val version: Int,
    val name: String? = null,
    val metadata: Map<String, JsonElement>? = null,
    val center: List<Double>? = null,
    val zoom: Double? = null,
    val bearing: Double? = null,
    val pitch: Double? = null,
    val light: Light? = null,
    val sources: Map<String, Source>,
    val layers: List<StyleLayer>,
    val sprite: String? = null,
    val glyphs: String? = null,
    val transition: Transition? = null
)
@Serializable
data class Light(
    val anchor: String? = null,
    val position: List<Double>? = null,
    val color: String? = null,
    val intensity: Double? = null
)

@Serializable
data class Transition(
    val duration: Int? = null,
    val delay: Int? = null
)

/**
 * Unified source class for all source types
 */
@Serializable
data class Source(
    val type: String,
    // VectorSource and RasterSource properties
    val url: String? = null,
    val tiles: List<String>? = null,
    val minzoom: Int? = null,
    val maxzoom: Int? = null,
    val attribution: String? = null,
    // RasterSource specific property
    val tileSize: Int? = null,
    // GeoJSONSource properties
    val data: String? = null,
    val buffer: Int? = null,
    val tolerance: Double? = null,
    val cluster: Boolean? = null,
    val clusterRadius: Int? = null,
    val clusterMaxZoom: Int? = null
)

@Serializable
data class StyleLayer(
    val id: String,
    val type: String,
    val source: String? = null,
    @SerialName("source-layer") val sourceLayer: String? = null,
    val minzoom: Double? = null,
    val maxzoom: Double? = null,
    val filter: List<JsonElement>? = null,
    val layout: Map<String, JsonElement>? = null,
    val paint: Map<String, JsonElement>? = null
)

