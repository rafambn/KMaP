package com.rafambn.kmap.style.compiled

import com.rafambn.kmap.style.evaluation.FeatureGeometryContext
data class CompiledFilter(
    private val evaluator: (zoomLevel: Double, featureProperties: Map<String, Any>, geometryType: String, featureId: Any?, featureGeometry: FeatureGeometryContext?) -> Boolean
) {
    fun evaluate(
        zoomLevel: Double,
        featureProperties: Map<String, Any>,
        geometryType: String,
        featureId: Any?,
        featureGeometry: FeatureGeometryContext? = null
    ): Boolean = evaluator(zoomLevel, featureProperties, geometryType, featureId, featureGeometry)
}
