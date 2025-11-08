package com.rafambn.kmap.utils.style

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontFamily

// Expression evaluation context
data class EvaluationContext(
    val featureProperties: Map<String, Any> = emptyMap(),
    val geometryType: String = "Point",
    val zoomLevel: Double = 0.0,
    val featureId: Any? = null
)

// Compiled types
data class CompiledFilter(
    val evaluate: (featureProperties: Map<String, Any>, geometryType: String, featureId: Any?) -> Boolean,
    val requiredProperties: Set<String> = emptySet()
)

data class CompiledValue<T>(
    val evaluate: (zoomLevel: Double, featureProperties: Map<String, Any>, featureId: Any?) -> T?,
    val requiredProperties: Set<String> = emptySet()
)

data class CompiledPaint(
    val properties: Map<String, CompiledValue<*>>
)

data class CompiledLayout(
    val visibility: CompiledValue<Boolean>,
    val properties: Map<String, CompiledValue<*>>
)

data class OptimizedStyleLayer(
    val id: String,
    val type: String,
    val source: String?,
    val sourceLayer: String?,
    val minZoom: Double,
    val maxZoom: Double,
    val filter: CompiledFilter?,
    val layout: CompiledLayout,
    val paint: CompiledPaint
)

data class OptimizedStyle(
    val version: Int,
    val name: String?,
    val layers: List<OptimizedStyleLayer>,
    val sources: Map<String, Source>,
    val sprites: Map<String, ImageBitmap> = emptyMap(),
    val glyphs: Map<String, FontFamily> = emptyMap()
)
