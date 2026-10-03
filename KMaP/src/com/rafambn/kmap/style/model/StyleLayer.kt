package com.rafambn.kmap.style.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

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
