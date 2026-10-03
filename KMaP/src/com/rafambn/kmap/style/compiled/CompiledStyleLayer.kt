package com.rafambn.kmap.style.compiled

sealed interface CompiledStyleLayer {
    val id: String
    val type: CompiledLayerType
    val sourceLayer: String?
    val minZoom: Double
    val maxZoom: Double
    val filter: CompiledFilter?
    val visibility: CompiledValue<Boolean>
}
