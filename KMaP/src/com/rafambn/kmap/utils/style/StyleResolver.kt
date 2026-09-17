package com.rafambn.kmap.utils.style

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontFamily
import kotlinx.serialization.json.*

class StyleResolver(private val evaluator: ExpressionEvaluator = ExpressionEvaluator()) {

    fun resolve(
        rawStyle: Style,
        sprites: Map<String, ImageBitmap> = emptyMap(),
        glyphs: Map<String, FontFamily> = emptyMap(),
        locale: String = "en"
    ): OptimizedStyle {
        val compiledLayers = rawStyle.layers.map { compileLayer(it, locale) }
        return OptimizedStyle(
            version = rawStyle.version,
            name = rawStyle.name,
            layers = compiledLayers,
            sources = rawStyle.sources,
            sprites = sprites,
            glyphs = glyphs
        )
    }

    private fun compileLayer(layer: StyleLayer, locale: String): OptimizedStyleLayer {
        val filter = layer.filter?.let { elements -> compileFilter(elements.map { it.toValue() }, locale) }
        val paint = compilePaint(layer.paint, locale)
        val layout = compileLayout(layer.layout, locale)

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

    private fun compileFilter(filterExpression: List<Any?>, locale: String): CompiledFilter {
        val requiredProperties = evaluator.getRequiredProperties(filterExpression)
        return CompiledFilter(
            evaluate = { featureProperties, geometryType, featureId ->
                val context = EvaluationContext(featureProperties, geometryType, 0.0, featureId, locale)
                evaluator.evaluate(filterExpression, context) as? Boolean ?: false
            },
            requiredProperties = requiredProperties
        )
    }

    private fun compilePaint(paintMap: Map<String, JsonElement>?, locale: String): CompiledPaint {
        val compiledProperties = paintMap?.mapValues { (_, value) ->
            compileValue<Any>(value.toValue(), locale)
        } ?: emptyMap()
        return CompiledPaint(properties = compiledProperties)
    }

    private fun compileLayout(layoutMap: Map<String, JsonElement>?, locale: String): CompiledLayout {
        val visibilityValue = layoutMap?.get("visibility")?.toValue()
        val visibility = if (visibilityValue != null) {
            compileValue(visibilityValue, locale)
        } else {
            CompiledValue(evaluate = { _, _, _-> true }, requiredProperties = emptySet())
        }

        val otherProperties = layoutMap?.filterKeys { it != "visibility" }?.mapValues { (_, value) ->
            compileValue<Any>(value.toValue(), locale)
        } ?: emptyMap()

        return CompiledLayout(visibility = visibility, properties = otherProperties)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> compileValue(expression: Any?, locale: String): CompiledValue<T> {
        val requiredProperties = evaluator.getRequiredProperties(expression)
        return CompiledValue(
            evaluate = { zoomLevel, featureProperties, featureId ->
                val context = EvaluationContext(featureProperties, "Point", zoomLevel, featureId, locale)
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
