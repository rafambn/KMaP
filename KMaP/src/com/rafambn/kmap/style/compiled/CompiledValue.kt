package com.rafambn.kmap.style.compiled

data class CompiledValue<T>(
    private val evaluator: (zoomLevel: Double, featureProperties: Map<String, Any>, featureId: Any?, geometryType: String) -> T?
) {
    fun evaluate(
        zoomLevel: Double,
        featureProperties: Map<String, Any>,
        featureId: Any?,
        geometryType: String = "Point"
    ): T? = evaluator(zoomLevel, featureProperties, featureId, geometryType)
}
