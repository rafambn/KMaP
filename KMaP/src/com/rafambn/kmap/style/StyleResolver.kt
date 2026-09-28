package com.rafambn.kmap.style

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontFamily
import com.rafambn.kmap.style.expression.parseColor
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.*

class StyleResolver(private val evaluator: ExpressionEvaluator = ExpressionEvaluator()) {

    private val json = Json { ignoreUnknownKeys = true }
    private val tokenPattern = Regex("\\{([^{}]+)\\}")

    fun resolve(
        rawJson: String,
        sprites: Map<String, ImageBitmap> = emptyMap(),
        glyphs: Map<String, FontFamily> = emptyMap(),
        locale: String = "en"
    ): StyleResolution {
        val root = try {
            json.parseToJsonElement(rawJson) as? JsonObject
        } catch (error: SerializationException) {
            return StyleResolution(null, listOf(StyleIssue("$", StyleIssue.Kind.INVALID, error.message ?: "Invalid JSON")))
        } ?: return StyleResolution(null, listOf(StyleIssue("$", StyleIssue.Kind.INVALID, "Style must be a JSON object")))

        val issues = inspectStyle(root)
        val style = try {
            json.decodeFromJsonElement<Style>(root)
        } catch (error: SerializationException) {
            return StyleResolution(null, issues + StyleIssue("$", StyleIssue.Kind.INVALID, error.message ?: "Invalid style"))
        }
        return StyleResolution(compile(style, sprites, glyphs, locale), issues)
    }

    private fun compile(
        rawStyle: Style,
        sprites: Map<String, ImageBitmap>,
        glyphs: Map<String, FontFamily>,
        locale: String
    ): CompiledStyle {
        val compiledLayers = rawStyle.layers.mapNotNull { compileLayer(it, locale, sprites) }
        return CompiledStyle(
            layers = compiledLayers,
            sprites = sprites,
            glyphs = glyphs
        )
    }

    private fun compileLayer(layer: StyleLayer, locale: String, sprites: Map<String, ImageBitmap>): CompiledStyleLayer? {
        val type = when (layer.type) {
            "background" -> CompiledLayerType.BACKGROUND
            "fill" -> CompiledLayerType.FILL
            "line" -> CompiledLayerType.LINE
            "symbol" -> CompiledLayerType.SYMBOL
            else -> return null
        }
        if (type != CompiledLayerType.BACKGROUND && layer.sourceLayer == null) return null

        val filter = layer.filter?.let { elements -> compileFilter(elements.map { it.toValue() }, locale) }
        val paint = compilePaint(layer.paint, locale, sprites)
        val layout = compileLayout(layer.layout, locale, sprites)

        return CompiledStyleLayer(
            id = layer.id,
            type = type,
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

        val otherProperties = layoutMap?.filterKeys { it != "visibility" }?.mapValues { (name, value) ->
            compileValue<Any>(value.toValue(), locale, sprites, expandTokens = name == "text-field" || name == "icon-image")
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
        expression: Any?, locale: String, sprites: Map<String, ImageBitmap>, color: Boolean = false, expandTokens: Boolean = false
    ): CompiledValue<T> {
        val tokenizedStrings = when {
            !expandTokens -> emptyList()
            expression is String -> listOf(expression)
            expression is Map<*, *> && expression["property"] == null ->
                (expression["stops"] as? List<*>)?.mapNotNull { (it as? List<*>)?.getOrNull(1) as? String } ?: emptyList()
            else -> emptyList()
        }
        val tokenProperties = tokenizedStrings.flatMap { value ->
            tokenPattern.findAll(value).map { it.groupValues[1] }.toList()
        }.toSet()
        if (tokenProperties.isNotEmpty()) {
            return CompiledValue(
                evaluate = { zoomLevel, featureProperties, featureId ->
                    val context = EvaluationContext(featureProperties, "Point", zoomLevel, featureId, locale, sprites)
                    val value = evaluator.evaluate(expression, context)
                    (if (value is String) tokenPattern.replace(value) { match ->
                        stringifyTokenValue(featureProperties[match.groupValues[1]])
                    } else value) as? T
                },
                requiredProperties = evaluator.getRequiredProperties(expression) + tokenProperties
            )
        }

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

    private fun stringifyTokenValue(value: Any?): String {
        if (value == null) return ""
        if (value is Float || value is Double) {
            val number = (value as Number).toDouble()
            if (number == 0.0) return "0"
            if (number % 1.0 == 0.0 && number >= Long.MIN_VALUE.toDouble() && number < Long.MAX_VALUE.toDouble()) {
                return number.toLong().toString()
            }
        }
        return value.toString()
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
