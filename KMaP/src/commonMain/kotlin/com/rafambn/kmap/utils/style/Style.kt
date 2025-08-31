package com.rafambn.kmap.utils.style

import kotlinx.serialization.SerialName
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import kotlinx.serialization.Serializable


/**
 * Represents a MapBox/MapLibre style specification.
 * Based on the MapBox Style Specification: https://docs.mapbox.com/mapbox-gl-js/style-spec/
 * And MapLibre Style Specification: https://maplibre.org/maplibre-style-spec/
 */
@Serializable
data class Style(
    val version: Int,
    val name: String? = null,
    val metadata: Map<String, String>? = null,
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
){
    companion object {
        val serializerModule = SerializersModule {
            polymorphic(Source::class) {
                subclass(VectorSource::class)
                subclass(RasterSource::class)
                subclass(GeoJSONSource::class)
            }

            polymorphic(StyleLayer::class) {
                subclass(BackgroundLayer::class)
                subclass(FillLayer::class)
                subclass(LineLayer::class)
                subclass(SymbolLayer::class)
                subclass(RasterLayer::class)
                subclass(CircleLayer::class)
                subclass(FillExtrusionLayer::class)
                subclass(HeatmapLayer::class)
                subclass(HillshadeLayer::class)
            }
        }
    }
}

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
 * Abstract base class for all source types
 */
@Serializable
sealed class Source {
    abstract val type: String
}

@Serializable
@SerialName("vector")
data class VectorSource(
    override val type: String = "vector",
    val url: String? = null,
    val tiles: List<String>? = null,
    val minzoom: Int? = null,
    val maxzoom: Int? = null,
    val attribution: String? = null
) : Source()

@Serializable
@SerialName("raster")
data class RasterSource(
    override val type: String = "raster",
    val url: String? = null,
    val tiles: List<String>? = null,
    val tileSize: Int? = null,
    val minzoom: Int? = null,
    val maxzoom: Int? = null,
    val attribution: String? = null
) : Source()

@Serializable
@SerialName("geojson")
data class GeoJSONSource(
    override val type: String = "geojson",
    val data: String? = null,
    val maxzoom: Int? = null,
    val buffer: Int? = null,
    val tolerance: Double? = null,
    val cluster: Boolean? = null,
    val clusterRadius: Int? = null,
    val clusterMaxZoom: Int? = null
) : Source()

@Serializable
@SerialName("layer")
sealed class StyleLayer {
    abstract val id: String
    abstract val type: String
    abstract val source: String?
    abstract val sourceLayer: String?
    abstract val minzoom: Double?
    abstract val maxzoom: Double?
    abstract val filter: List<String>?
    abstract val layout: Map<String, String>?
    abstract val paint: Map<String, String>?
}

@Serializable
@SerialName("background")
data class BackgroundLayer(
    override val id: String,
    override val type: String = "background",
    override val source: String? = null,
    @SerialName("source-layer") override val sourceLayer: String? = null,
    override val minzoom: Double? = null,
    override val maxzoom: Double? = null,
    override val filter: List<String>? = null,
    override val layout: Map<String, String>? = null,
    override val paint: Map<String, String>? = null
) : StyleLayer()

@Serializable
@SerialName("fill")
data class FillLayer(
    override val id: String,
    override val type: String = "fill",
    override val source: String,
    @SerialName("source-layer") override val sourceLayer: String? = null,
    override val minzoom: Double? = null,
    override val maxzoom: Double? = null,
    override val filter: List<String>? = null,
    override val layout: Map<String, String>? = null,
    override val paint: Map<String, String>? = null
) : StyleLayer()

@Serializable
@SerialName("line")
data class LineLayer(
    override val id: String,
    override val type: String = "line",
    override val source: String,
    @SerialName("source-layer") override val sourceLayer: String? = null,
    override val minzoom: Double? = null,
    override val maxzoom: Double? = null,
    override val filter: List<String>? = null,
    override val layout: Map<String, String>? = null,
    override val paint: Map<String, String>? = null
) : StyleLayer()

@Serializable
@SerialName("symbol")
data class SymbolLayer(
    override val id: String,
    override val type: String = "symbol",
    override val source: String,
    @SerialName("source-layer") override val sourceLayer: String? = null,
    override val minzoom: Double? = null,
    override val maxzoom: Double? = null,
    override val filter: List<String>? = null,
    override val layout: Map<String, String>? = null,
    override val paint: Map<String, String>? = null
) : StyleLayer()

@Serializable
@SerialName("raster")
data class RasterLayer(
    override val id: String,
    override val type: String = "raster",
    override val source: String,
    @SerialName("source-layer") override val sourceLayer: String? = null,
    override val minzoom: Double? = null,
    override val maxzoom: Double? = null,
    override val filter: List<String>? = null,
    override val layout: Map<String, String>? = null,
    override val paint: Map<String, String>? = null
) : StyleLayer()

@Serializable
@SerialName("circle")
data class CircleLayer(
    override val id: String,
    override val type: String = "circle",
    override val source: String,
    @SerialName("source-layer") override val sourceLayer: String? = null,
    override val minzoom: Double? = null,
    override val maxzoom: Double? = null,
    override val filter: List<String>? = null,
    override val layout: Map<String, String>? = null,
    override val paint: Map<String, String>? = null
) : StyleLayer()

@Serializable
@SerialName("fill-extrusion")
data class FillExtrusionLayer(
    override val id: String,
    override val type: String = "fill-extrusion",
    override val source: String,
    @SerialName("source-layer") override val sourceLayer: String? = null,
    override val minzoom: Double? = null,
    override val maxzoom: Double? = null,
    override val filter: List<String>? = null,
    override val layout: Map<String, String>? = null,
    override val paint: Map<String, String>? = null
) : StyleLayer()

@Serializable
@SerialName("heatmap")
data class HeatmapLayer(
    override val id: String,
    override val type: String = "heatmap",
    override val source: String,
    @SerialName("source-layer") override val sourceLayer: String? = null,
    override val minzoom: Double? = null,
    override val maxzoom: Double? = null,
    override val filter: List<String>? = null,
    override val layout: Map<String, String>? = null,
    override val paint: Map<String, String>? = null
) : StyleLayer()

@Serializable
@SerialName("hillshade")
data class HillshadeLayer(
    override val id: String,
    override val type: String = "hillshade",
    override val source: String,
    @SerialName("source-layer") override val sourceLayer: String? = null,
    override val minzoom: Double? = null,
    override val maxzoom: Double? = null,
    override val filter: List<String>? = null,
    override val layout: Map<String, String>? = null,
    override val paint: Map<String, String>? = null
) : StyleLayer()
