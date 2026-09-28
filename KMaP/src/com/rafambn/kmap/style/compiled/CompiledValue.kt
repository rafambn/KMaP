package com.rafambn.kmap.style.compiled

data class CompiledValue<T>(
    val evaluate: (zoomLevel: Double, featureProperties: Map<String, Any>, featureId: Any?) -> T?
)
