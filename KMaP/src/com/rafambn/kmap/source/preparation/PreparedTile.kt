package com.rafambn.kmap.source.preparation

import kotlinx.serialization.Serializable

/**
 * Packed tile-local geometry; style layers reference features without duplicating coordinates.
 * Arrays belong to this result and must not be mutated after publication.
 */
@Serializable
data class PreparedTile(
    val extent: Int = 4096,
    val features: List<PreparedFeature> = emptyList(),
    val layerFeatures: Map<String, List<Int>> = emptyMap(),
)
