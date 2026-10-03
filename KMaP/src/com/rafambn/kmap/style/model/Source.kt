package com.rafambn.kmap.style.model

import kotlinx.serialization.Serializable

/** Unified source class for all source types. */
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
