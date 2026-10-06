package com.rafambn.kmap.source.preparation

import kotlinx.serialization.Serializable

/** Only layer membership and integer tile-zoom filters belong to preparation. */
@Serializable
data class PreparationStyle(
    val layers: List<PreparationLayer>,
    val locale: String = "en",
)
