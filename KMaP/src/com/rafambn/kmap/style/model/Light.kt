package com.rafambn.kmap.style.model

import kotlinx.serialization.Serializable

@Serializable
data class Light(
    val anchor: String? = null,
    val position: List<Double>? = null,
    val color: String? = null,
    val intensity: Double? = null
)
