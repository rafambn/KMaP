package com.rafambn.kmap.source.preparation

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class PreparationLayer(
    val id: String,
    val type: String,
    val sourceLayer: String,
    val filter: JsonElement? = null,
)
