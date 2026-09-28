package com.rafambn.kmap.style.evaluation

import com.rafambn.kmap.style.SpriteImage

data class EvaluationContext(
    val featureProperties: Map<String, Any> = emptyMap(),
    val geometryType: String = "Point",
    val zoomLevel: Double = 0.0,
    val featureId: Any? = null,
    val locale: String = "en",
    val sprites: Map<String, SpriteImage> = emptyMap(),
    val featureGeometry: FeatureGeometryContext? = null
)
