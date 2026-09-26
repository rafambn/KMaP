package com.rafambn.kmap.style

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontFamily
import com.rafambn.kmap.style.expression.parseColor
import kotlinx.serialization.json.*

class StyleResolver(private val evaluator: ExpressionEvaluator = ExpressionEvaluator()) {

    fun resolve(
        rawStyle: Style,
        sprites: Map<String, ImageBitmap> = emptyMap(),
        glyphs: Map<String, FontFamily> = emptyMap(),
        locale: String = "en"
    ): OptimizedStyle {
        val compiledLayers = rawStyle.layers.map { compileLayer(it, locale, sprites) }
        return OptimizedStyle(
            version = rawStyle.version,
            name = rawStyle.name,
            layers = compiledLayers,
            sources = rawStyle.sources,
            sprites = sprites,
            glyphs = glyphs
        )
    }

    private fun compileLayer(layer: StyleLayer, locale: String, sprites: Map<String, ImageBitmap>): OptimizedStyleLayer {
        val filter = layer.filter?.let { elements -> compileFilter(elements.map { it.toValue() }, locale) }
        val paint = compilePaint(layer.paint, locale, sprites)
        val layout = compileLayout(layer.layout, locale, sprites)

        return OptimizedStyleLayer(
            id = layer.id,
            type = layer.type,
            source = layer.source,
            sourceLayer = layer.sourceLayer,
            minZoom = layer.minzoom ?: 0.0,
            maxZoom = layer.maxzoom ?: Double.POSITIVE_INFINITY,
            filter = filter,
            layout = layout,
            paint = paint
        )
    }

    private fun compileFilter(filterExpression: List<Any?>, locale: String): CompiledFilter {
        val requiredProperties = evaluator.getRequiredProperties(filterExpression)
        return CompiledFilter(
            evaluator = { zoomLevel, featureProperties, geometryType, featureId, featureGeometry ->
                val context = EvaluationContext(featureProperties, geometryType, zoomLevel, featureId, locale, featureGeometry = featureGeometry)
                evaluator.evaluate(filterExpression, context) as? Boolean ?: false
            },
            requiredProperties = requiredProperties
        )
    }

    private fun compilePaint(paintMap: Map<String, JsonElement>?, locale: String, sprites: Map<String, ImageBitmap>): CompiledPaint {
        val compiledProperties = paintMap?.mapValues { (name, value) ->
            compileValue<Any>(value.toValue(), locale, sprites, name.endsWith("-color"))
        } ?: emptyMap()
        return CompiledPaint(properties = compiledProperties)
    }

    private fun compileLayout(layoutMap: Map<String, JsonElement>?, locale: String, sprites: Map<String, ImageBitmap>): CompiledLayout {
        val visibilityValue = layoutMap?.get("visibility")?.toValue()
        val visibility = compileVisibility(visibilityValue, locale, sprites)

        val otherProperties = layoutMap?.filterKeys { it != "visibility" }?.mapValues { (_, value) ->
            compileValue<Any>(value.toValue(), locale, sprites)
        } ?: emptyMap()

        return CompiledLayout(visibility = visibility, properties = otherProperties)
    }

    private fun compileVisibility(expression: Any?, locale: String, sprites: Map<String, ImageBitmap>): CompiledValue<Boolean> {
        if (expression == null) {
            return CompiledValue(evaluate = { _, _, _ -> true }, requiredProperties = emptySet())
        }

        return CompiledValue(
            evaluate = { zoomLevel, featureProperties, featureId ->
                val context = EvaluationContext(featureProperties, "Point", zoomLevel, featureId, locale, sprites)
                evaluator.evaluate(expression, context) != "none"
            },
            requiredProperties = evaluator.getRequiredProperties(expression)
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> compileValue(
        expression: Any?, locale: String, sprites: Map<String, ImageBitmap>, color: Boolean = false
    ): CompiledValue<T> {
        val requiredProperties = evaluator.getRequiredProperties(expression)
        return CompiledValue(
            evaluate = { zoomLevel, featureProperties, featureId ->
                val context = EvaluationContext(featureProperties, "Point", zoomLevel, featureId, locale, sprites)
                val result = evaluator.evaluate(expression, context)
                (if (color && result is String) parseColor(result) ?: result else result) as? T
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
                else booleanOrNull ?: content.toDoubleOrNull() ?: content
            }
        }
    }
}
