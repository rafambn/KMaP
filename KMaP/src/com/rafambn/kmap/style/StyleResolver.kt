package com.rafambn.kmap.style

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontFamily
import com.rafambn.kmap.style.compiled.CompiledBackgroundLayer
import com.rafambn.kmap.style.compiled.CompiledFillLayer
import com.rafambn.kmap.style.compiled.CompiledFilter
import com.rafambn.kmap.style.compiled.CompiledLineLayer
import com.rafambn.kmap.style.compiled.CompiledStyle
import com.rafambn.kmap.style.compiled.CompiledStyleLayer
import com.rafambn.kmap.style.compiled.CompiledSymbolLayer
import com.rafambn.kmap.style.compiled.CompiledValue
import com.rafambn.kmap.style.evaluation.EvaluationContext
import com.rafambn.kmap.style.evaluation.ExpressionEvaluator
import com.rafambn.kmap.style.expression.parseColor
import com.rafambn.kmap.style.model.Style
import com.rafambn.kmap.style.model.StyleLayer
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
        if (layer.type !in setOf("background", "fill", "line", "symbol")) return null
        val filter = layer.filter?.let { elements -> compileFilter(elements.map { it.toValue() }, locale) }
        val visibility = compileVisibility(layer.layout?.get("visibility")?.toValue(), locale, sprites)
        val paint = layer.paint
        val layout = layer.layout
        val minZoom = layer.minzoom ?: 0.0
        val maxZoom = layer.maxzoom ?: Double.POSITIVE_INFINITY

        return when (layer.type) {
            "background" -> CompiledBackgroundLayer(
                layer.id, minZoom, maxZoom, filter, visibility,
                color = compileProperty(paint?.get("background-color"), locale, sprites, ::asColor),
                opacity = compileProperty(paint?.get("background-opacity"), locale, sprites, ::asNumber)
            )
            "fill" -> CompiledFillLayer(
                layer.id, layer.sourceLayer ?: return null, minZoom, maxZoom, filter, visibility,
                color = compileProperty(paint?.get("fill-color"), locale, sprites, ::asColor),
                opacity = compileProperty(paint?.get("fill-opacity"), locale, sprites, ::asNumber),
                outlineColor = compileProperty(paint?.get("fill-outline-color"), locale, sprites, ::asColor),
                antialias = compileProperty(paint?.get("fill-antialias"), locale, sprites, ::asBoolean)
            )
            "line" -> CompiledLineLayer(
                layer.id, layer.sourceLayer ?: return null, minZoom, maxZoom, filter, visibility,
                color = compileProperty(paint?.get("line-color"), locale, sprites, ::asColor),
                width = compileProperty(paint?.get("line-width"), locale, sprites, ::asNumber),
                opacity = compileProperty(paint?.get("line-opacity"), locale, sprites, ::asNumber),
                dashArray = compileProperty(paint?.get("line-dasharray"), locale, sprites, ::asNumberList),
                patternPresent = compileProperty(paint?.get("line-pattern"), locale, sprites, convert = { it != null }),
                cap = compileProperty(layout?.get("line-cap"), locale, sprites, ::asString),
                join = compileProperty(layout?.get("line-join"), locale, sprites, ::asString)
            )
            "symbol" -> CompiledSymbolLayer(
                layer.id, layer.sourceLayer ?: return null, minZoom, maxZoom, filter, visibility,
                textField = compileProperty(layout?.get("text-field"), locale, sprites, ::asString, expandTokens = true),
                textTransform = compileProperty(layout?.get("text-transform"), locale, sprites, ::asString),
                textSize = compileProperty(layout?.get("text-size"), locale, sprites, ::asNumber),
                textMaxWidth = compileProperty(layout?.get("text-max-width"), locale, sprites, ::asNumber),
                textLineHeight = compileProperty(layout?.get("text-line-height"), locale, sprites, ::asNumber),
                textJustify = compileProperty(layout?.get("text-justify"), locale, sprites, ::asString),
                textAnchor = compileProperty(layout?.get("text-anchor"), locale, sprites, ::asString),
                textOffset = compileProperty(layout?.get("text-offset"), locale, sprites, ::asNumberList),
                textRadialOffset = compileProperty(layout?.get("text-radial-offset"), locale, sprites, ::asNumber),
                textRotate = compileProperty(layout?.get("text-rotate"), locale, sprites, ::asNumber),
                textFont = compileProperty(layout?.get("text-font"), locale, sprites, ::asStringList),
                iconImage = compileProperty(layout?.get("icon-image"), locale, sprites, { asIconImage(it, sprites) }, expandTokens = true),
                iconSize = compileProperty(layout?.get("icon-size"), locale, sprites, ::asNumber),
                iconRotate = compileProperty(layout?.get("icon-rotate"), locale, sprites, ::asNumber),
                iconOffset = compileProperty(layout?.get("icon-offset"), locale, sprites, ::asNumberList),
                iconAnchor = compileProperty(layout?.get("icon-anchor"), locale, sprites, ::asString),
                textColor = compileProperty(paint?.get("text-color"), locale, sprites, ::asColor),
                textOpacity = compileProperty(paint?.get("text-opacity"), locale, sprites, ::asNumber),
                textHaloColor = compileProperty(paint?.get("text-halo-color"), locale, sprites, ::asColor),
                textHaloWidth = compileProperty(paint?.get("text-halo-width"), locale, sprites, ::asNumber),
                textHaloBlur = compileProperty(paint?.get("text-halo-blur"), locale, sprites, ::asNumber),
                textTranslate = compileProperty(paint?.get("text-translate"), locale, sprites, ::asNumberList),
                iconOpacity = compileProperty(paint?.get("icon-opacity"), locale, sprites, ::asNumber)
            )
            else -> null
        }
    }

    private fun compileFilter(filterExpression: List<Any?>, locale: String): CompiledFilter {
        return CompiledFilter(
            evaluator = { zoomLevel, featureProperties, geometryType, featureId, featureGeometry ->
                val context = EvaluationContext(featureProperties, geometryType, zoomLevel, featureId, locale, featureGeometry = featureGeometry)
                evaluator.evaluate(filterExpression, context) as? Boolean ?: false
            }
        )
    }

    private fun compileVisibility(expression: Any?, locale: String, sprites: Map<String, ImageBitmap>): CompiledValue<Boolean> {
        if (expression == null) {
            return CompiledValue(evaluator = { _, _, _, _ -> true })
        }

        return CompiledValue(
            evaluator = { zoomLevel, featureProperties, featureId, geometryType ->
                val context = EvaluationContext(featureProperties, geometryType, zoomLevel, featureId, locale, sprites)
                evaluator.evaluate(expression, context) != "none"
            }
        )
    }

    private fun <T> compileProperty(
        value: JsonElement?, locale: String, sprites: Map<String, ImageBitmap>, convert: (Any?) -> T?, expandTokens: Boolean = false
    ): CompiledValue<T>? = value?.let { compileValue(it.toValue(), locale, sprites, convert, expandTokens) }

    private fun <T> compileValue(
        expression: Any?, locale: String, sprites: Map<String, ImageBitmap>, convert: (Any?) -> T?, expandTokens: Boolean
    ): CompiledValue<T> {
        val tokenizedStrings = when {
            !expandTokens -> emptyList()
            expression is String -> listOf(expression)
            expression is Map<*, *> && expression["property"] == null ->
                (expression["stops"] as? List<*>)?.mapNotNull { (it as? List<*>)?.getOrNull(1) as? String } ?: emptyList()
            else -> emptyList()
        }
        val replaceTokens = tokenizedStrings.any { tokenPattern.containsMatchIn(it) }
        return CompiledValue(
            evaluator = { zoomLevel, featureProperties, featureId, geometryType ->
                val context = EvaluationContext(featureProperties, geometryType, zoomLevel, featureId, locale, sprites)
                val result = evaluator.evaluate(expression, context)
                val value = if (replaceTokens && result is String) tokenPattern.replace(result) { match ->
                    stringifyTokenValue(featureProperties[match.groupValues[1]])
                } else result
                convert(value)
            }
        )
    }

    private fun asColor(value: Any?): Color? = when (value) {
        is Color -> value
        is String -> parseColor(value)
        else -> null
    }

    private fun asNumber(value: Any?): Double? = (value as? Number)?.toDouble()
    private fun asBoolean(value: Any?): Boolean? = value as? Boolean
    private fun asString(value: Any?): String? = value as? String

    private fun asNumberList(value: Any?): List<Double>? {
        val values = value as? List<*> ?: return null
        if (values.any { it !is Number }) return null
        return values.map { (it as Number).toDouble() }
    }

    private fun asStringList(value: Any?): List<String>? = (value as? List<*>)?.filterIsInstance<String>()

    private fun asIconImage(value: Any?, sprites: Map<String, ImageBitmap>): ImageBitmap? = when (value) {
        is ImageBitmap -> value
        is String -> sprites[value]
        else -> null
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
