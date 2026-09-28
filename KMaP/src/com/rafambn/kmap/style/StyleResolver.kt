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

        val issues = inspectStyle(root).toMutableList()
        val style = try {
            json.decodeFromJsonElement<Style>(root)
        } catch (error: SerializationException) {
            return StyleResolution(null, issues + StyleIssue("$", StyleIssue.Kind.INVALID, error.message ?: "Invalid style"))
        }
        return StyleResolution(compile(style, sprites, glyphs, locale, issues), issues)
    }

    private fun compile(
        rawStyle: Style,
        sprites: Map<String, ImageBitmap>,
        glyphs: Map<String, FontFamily>,
        locale: String,
        issues: MutableList<StyleIssue>
    ): CompiledStyle {
        val compiledLayers = rawStyle.layers.mapIndexedNotNull { index, layer -> compileLayer(layer, index, locale, sprites, issues) }
        return CompiledStyle(
            layers = compiledLayers,
            sprites = sprites,
            glyphs = glyphs
        )
    }

    private fun compileLayer(
        layer: StyleLayer, index: Int, locale: String, sprites: Map<String, ImageBitmap>, issues: MutableList<StyleIssue>
    ): CompiledStyleLayer? {
        if (layer.type !in setOf("background", "fill", "line", "symbol")) return null
        fun <T> paintValue(name: String, convert: (Any?) -> T?): CompiledValue<T>? =
            compileProperty(layer.paint?.get(name), "/layers/$index/paint/$name", layer.id, issues, locale, sprites, convert)

        fun <T> layoutValue(
            name: String, convert: (Any?) -> T?, expandTokens: Boolean = false,
            validate: (Any?) -> Boolean = { convert(it) != null }
        ): CompiledValue<T>? = compileProperty(
            layer.layout?.get(name), "/layers/$index/layout/$name", layer.id, issues, locale, sprites, convert, expandTokens, validate
        )

        val filter = layer.filter?.let { elements -> compileFilter(elements.map { it.toValue() }, locale) }
        layer.layout?.get("visibility")?.let {
            inspectLiteral(it, "/layers/$index/layout/visibility", layer.id, issues, { value -> asString(value) != null })
        }
        val visibility = compileVisibility(layer.layout?.get("visibility")?.toValue(), locale, sprites)
        val paint = layer.paint
        val minZoom = layer.minzoom ?: 0.0
        val maxZoom = layer.maxzoom ?: Double.POSITIVE_INFINITY

        return when (layer.type) {
            "background" -> CompiledBackgroundLayer(
                layer.id, minZoom, maxZoom, filter, visibility,
                color = paintValue("background-color", ::asColor),
                opacity = paintValue("background-opacity", ::asNumber)
            )
            "fill" -> CompiledFillLayer(
                layer.id, layer.sourceLayer ?: return null, minZoom, maxZoom, filter, visibility,
                color = paintValue("fill-color", ::asColor),
                opacity = paintValue("fill-opacity", ::asNumber),
                outlineColor = paintValue("fill-outline-color", ::asColor),
                antialias = paintValue("fill-antialias", ::asBoolean)
            )
            "line" -> CompiledLineLayer(
                layer.id, layer.sourceLayer ?: return null, minZoom, maxZoom, filter, visibility,
                color = paintValue("line-color", ::asColor),
                width = paintValue("line-width", ::asNumber),
                opacity = paintValue("line-opacity", ::asNumber),
                dashArray = paintValue("line-dasharray", ::asNumberList),
                patternPresent = compileProperty(paint?.get("line-pattern"), locale, sprites, convert = { it != null }),
                cap = layoutValue("line-cap", ::asString),
                join = layoutValue("line-join", ::asString)
            )
            "symbol" -> CompiledSymbolLayer(
                layer.id, layer.sourceLayer ?: return null, minZoom, maxZoom, filter, visibility,
                textField = layoutValue("text-field", ::asString, expandTokens = true),
                textTransform = layoutValue("text-transform", ::asString),
                textSize = layoutValue("text-size", ::asNumber),
                textMaxWidth = layoutValue("text-max-width", ::asNumber),
                textLineHeight = layoutValue("text-line-height", ::asNumber),
                textJustify = layoutValue("text-justify", ::asString),
                textAnchor = layoutValue("text-anchor", ::asString),
                textOffset = layoutValue("text-offset", ::asNumberList),
                textRadialOffset = layoutValue("text-radial-offset", ::asNumber),
                textRotate = layoutValue("text-rotate", ::asNumber),
                textFont = layoutValue("text-font", ::asStringList),
                iconImage = layoutValue("icon-image", { asIconImage(it, sprites) }, expandTokens = true, validate = { it is String }),
                iconSize = layoutValue("icon-size", ::asNumber),
                iconRotate = layoutValue("icon-rotate", ::asNumber),
                iconOffset = layoutValue("icon-offset", ::asNumberList),
                iconAnchor = layoutValue("icon-anchor", ::asString),
                textColor = paintValue("text-color", ::asColor),
                textOpacity = paintValue("text-opacity", ::asNumber),
                textHaloColor = paintValue("text-halo-color", ::asColor),
                textHaloWidth = paintValue("text-halo-width", ::asNumber),
                textHaloBlur = paintValue("text-halo-blur", ::asNumber),
                textTranslate = paintValue("text-translate", ::asNumberList),
                iconOpacity = paintValue("icon-opacity", ::asNumber)
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

    private fun <T> compileProperty(
        value: JsonElement?, path: String, layerId: String, issues: MutableList<StyleIssue>,
        locale: String, sprites: Map<String, ImageBitmap>, convert: (Any?) -> T?, expandTokens: Boolean = false,
        validate: (Any?) -> Boolean = { convert(it) != null }
    ): CompiledValue<T>? {
        value?.let { inspectLiteral(it, path, layerId, issues, validate, path.endsWith("/text-font")) }
        return compileProperty(value, locale, sprites, convert, expandTokens)
    }

    private fun inspectLiteral(
        value: JsonElement, path: String, layerId: String, issues: MutableList<StyleIssue>,
        validate: (Any?) -> Boolean, allowStringArray: Boolean = false
    ) {
        val stops = (value as? JsonObject)?.get("stops") as? JsonArray
        if (stops != null) {
            stops.forEachIndexed { index, stop ->
                val pair = stop as? JsonArray
                if (pair?.size == 2) inspectLiteral(pair[1], "$path/stops/$index/1", layerId, issues, validate, allowStringArray)
            }
            return
        }
        if (value is JsonArray) {
            val operator = (value.firstOrNull() as? JsonPrimitive)?.takeIf { it.isString }?.content
            if (operator == "literal" && value.size == 2) {
                inspectLiteral(value[1], "$path/1", layerId, issues, validate, allowStringArray)
                return
            }
            if (operator != null && isStyleExpressionOperator(operator)) return
            if (operator != null && !allowStringArray) return
        }
        if (!validate(value.toValue())) {
            issues += StyleIssue(path, StyleIssue.Kind.INVALID, "Literal value is incompatible with this property", layerId)
        }
    }

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

    private fun asNumber(value: Any?): Double? = (value as? Number)?.toDouble()?.takeIf { it.isFinite() }
    private fun asBoolean(value: Any?): Boolean? = value as? Boolean
    private fun asString(value: Any?): String? = value as? String

    private fun asNumberList(value: Any?): List<Double>? {
        val values = value as? List<*> ?: return null
        if (values.any { it !is Number }) return null
        return values.map { (it as Number).toDouble() }.takeIf { numbers -> numbers.all { it.isFinite() } }
    }

    private fun asStringList(value: Any?): List<String>? {
        val values = value as? List<*> ?: return null
        if (values.any { it !is String }) return null
        return values.map { it as String }
    }

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
