package com.rafambn.kmap.utils.style

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontFamily
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

class StyleResolver(private val evaluator: ExpressionEvaluator = ExpressionEvaluator()) {

    fun resolve(
        rawStyle: Style,
        sprites: Map<String, ImageBitmap> = emptyMap(),
        glyphs: Map<String, FontFamily> = emptyMap()
    ): OptimizedStyle {
        val compiledLayers = rawStyle.layers.mapNotNull { compileLayer(it) }
        return OptimizedStyle(
            version = rawStyle.version,
            name = rawStyle.name,
            layers = compiledLayers,
            sources = rawStyle.sources,
            sprites = sprites,
            glyphs = glyphs
        )
    }

    private fun compileLayer(layer: StyleLayer): OptimizedStyleLayer {
        val filter = layer.filter?.let { compileFilter(it.map { it.toValue() }) }
        val paint = compilePaint(layer.paint)
        val layout = compileLayout(layer.layout)

        return OptimizedStyleLayer(
            id = layer.id,
            type = layer.type,
            source = layer.source,
            sourceLayer = layer.sourceLayer,
            minZoom = layer.minzoom ?: 0.0,
            maxZoom = layer.maxzoom ?: 24.0,
            filter = filter,
            layout = layout,
            paint = paint
        )
    }

    private fun compileFilter(filterExpression: List<Any?>): CompiledFilter {
        val requiredProperties = evaluator.getRequiredProperties(filterExpression)
        return CompiledFilter(
            evaluate = { featureProperties, geometryType, featureId ->
                val context = EvaluationContext(featureProperties, geometryType, 0.0, featureId)
                evaluator.evaluate(filterExpression, context) as? Boolean ?: false
            },
            requiredProperties = requiredProperties
        )
    }

    private fun compilePaint(paintMap: Map<String, JsonElement>?): CompiledPaint {
        val compiledProperties = paintMap?.mapValues { (_, value) ->
            compileValue<Any>(value.toValue())
        } ?: emptyMap()
        return CompiledPaint(properties = compiledProperties)
    }

    private fun compileLayout(layoutMap: Map<String, JsonElement>?): CompiledLayout {
        val visibilityValue = layoutMap?.get("visibility")?.toValue()
        val visibility = if (visibilityValue != null) {
            compileValue(visibilityValue)
        } else {
            CompiledValue(evaluate = { _, _, _ -> true }, requiredProperties = emptySet())
        }

        val otherProperties = layoutMap?.filterKeys { it != "visibility" }?.mapValues { (_, value) ->
            compileValue<Any>(value.toValue())
        } ?: emptyMap()

        @Suppress("UNCHECKED_CAST")
        return CompiledLayout(visibility = visibility as CompiledValue<Boolean>, properties = otherProperties)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> compileValue(expression: Any?): CompiledValue<T> {
        val requiredProperties = evaluator.getRequiredProperties(expression)
        return CompiledValue(
            evaluate = { zoomLevel, featureProperties, featureId ->
                val context = EvaluationContext(featureProperties, "Point", zoomLevel, featureId)
                evaluator.evaluate(expression, context) as? T
            },
            requiredProperties = requiredProperties
        )
    }

    private fun JsonElement.toValue(): Any? {
        return when (this) {
            is JsonNull -> null
            is JsonObject -> this.map { it.key to it.value.toValue() }.toMap()
            is JsonArray -> this.map { it.toValue() }
            is JsonPrimitive -> {
                if (isString) content
                else booleanOrNull ?: content.toDoubleOrNull() ?: content.toLongOrNull() ?: content
            }
        }
    }
}
