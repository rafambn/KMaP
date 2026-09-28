package com.rafambn.kmap.style

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

private val rootKeys = setOf("version", "name", "metadata", "sources", "layers", "sprite", "glyphs")
private val ignoredRootKeys = setOf("center", "zoom", "bearing", "pitch", "light", "transition", "imports", "terrain", "fog", "rain", "snow", "projection", "schema", "lights", "models", "iconsets", "featuresets", "fragment", "color-theme", "state", "sky", "camera")
private val layerKeys = setOf("id", "type", "source", "source-layer", "minzoom", "maxzoom", "filter", "layout", "paint", "metadata")
private val sourceKeys = setOf("type", "url", "tiles", "minzoom", "maxzoom", "attribution", "tileSize", "data", "buffer", "tolerance", "cluster", "clusterRadius", "clusterMaxZoom")
private val paintProperties = mapOf(
    "background" to setOf("background-color", "background-opacity"),
    "fill" to setOf("fill-color", "fill-opacity", "fill-outline-color", "fill-antialias"),
    "line" to setOf("line-color", "line-width", "line-opacity", "line-dasharray"),
    "symbol" to setOf("text-color", "text-opacity", "text-halo-color", "text-halo-width", "text-halo-blur", "text-translate", "icon-opacity")
)
private val layoutProperties = mapOf(
    "background" to setOf("visibility"),
    "fill" to setOf("visibility"),
    "line" to setOf("visibility", "line-cap", "line-join"),
    "symbol" to setOf("visibility", "text-field", "text-transform", "text-size", "text-max-width", "text-line-height",
        "text-justify", "text-anchor", "text-offset", "text-radial-offset", "text-rotate", "text-font", "icon-image",
        "icon-size", "icon-rotate", "icon-offset", "icon-anchor")
)
private val expressionOperators = setOf(
    "all", "any", "!", "==", "!=", ">", ">=", "<", "<=", "get", "has", "geometry-type", "id", "zoom",
    "at", "in", "index-of", "length", "slice", "case", "coalesce", "match", "within", "literal", "image", "number-format",
    "array", "boolean", "number", "object", "string", "to-boolean", "to-color", "to-number", "to-string", "typeof",
    "concat", "downcase", "upcase", "split", "rgb", "rgba", "hsl", "hsla", "to-hsla", "to-rgba", "+", "-", "*", "/",
    "acos", "asin", "atan", "cos", "sin", "tan", "ln", "log10", "log2", "abs", "ceil", "floor", "round", "sqrt",
    "%", "^", "max", "min", "e", "ln2", "pi", "step", "interpolate", "interpolate-hcl", "interpolate-lab"
)
private val literalArrayProperties = setOf("line-dasharray", "text-offset", "text-translate", "icon-offset", "text-font")
private val unsupportedArrayOperators = setOf("feature-state", "format", "is-supported-script", "let", "var", "properties", "line-progress")

internal fun inspectStyle(root: JsonObject): List<StyleIssue> {
    val issues = mutableListOf<StyleIssue>()
    fun issue(path: String, kind: StyleIssue.Kind, message: String, layerId: String? = null) {
        issues += StyleIssue(path, kind, message, layerId)
    }

    root.keys.forEach { key ->
        when (key) {
            "sprite", "glyphs" -> issue("/$key", StyleIssue.Kind.UNSUPPORTED, "The '$key' URL is not fetched; supply assets to StyleResolver")
            in rootKeys -> Unit
            in ignoredRootKeys -> issue("/$key", StyleIssue.Kind.UNSUPPORTED, "Root property '$key' is not used by the vector canvas")
            else -> issue("/$key", StyleIssue.Kind.UNKNOWN, "Unknown root property '$key'")
        }
    }

    (root["sources"] as? JsonObject)?.forEach { (name, value) ->
        val path = "/sources/${name.pointerToken()}"
        val source = value as? JsonObject ?: return@forEach
        source.keys.filterNot { it in sourceKeys }.forEach { key ->
            issue("$path/${key.pointerToken()}", StyleIssue.Kind.UNKNOWN, "Unknown source property '$key'")
        }
        for (key in listOf("url", "tiles", "data")) {
            if (key in source) issue("$path/$key", StyleIssue.Kind.UNSUPPORTED, "Source '$key' is not loaded; the canvas uses its supplied tile source")
        }
    }

    val layers = root["layers"] as? JsonArray ?: return issues
    val seenIds = mutableSetOf<String>()
    val usedSources = mutableSetOf<String>()
    var backgroundSeen = false
    layers.forEachIndexed { index, element ->
        val path = "/layers/$index"
        val layer = element as? JsonObject ?: return@forEachIndexed
        val id = (layer["id"] as? JsonPrimitive)?.contentOrNull
        val type = (layer["type"] as? JsonPrimitive)?.contentOrNull
        if (id != null && !seenIds.add(id)) issue("$path/id", StyleIssue.Kind.UNSUPPORTED, "Duplicate layer ID '$id'", id)
        layer.keys.filterNot { it in layerKeys }.forEach { key ->
            issue("$path/$key", StyleIssue.Kind.UNKNOWN, "Unknown layer property '$key'", id)
        }
        if (type == null || type !in paintProperties) {
            issue("$path/type", StyleIssue.Kind.UNSUPPORTED, "Layer type '$type' is not rendered", id)
            return@forEachIndexed
        }
        if (type == "background") {
            if (backgroundSeen) issue(path, StyleIssue.Kind.UNSUPPORTED, "Only the first background layer is drawn", id)
            backgroundSeen = true
        } else {
            if ((layer["source-layer"] as? JsonPrimitive)?.contentOrNull == null) {
                issue("$path/source-layer", StyleIssue.Kind.UNSUPPORTED, "Vector layers need a source-layer to select tile features", id)
            }
            val source = (layer["source"] as? JsonPrimitive)?.contentOrNull
            if (source != null) usedSources += source
            val sourceType = ((root["sources"] as? JsonObject)?.get(source) as? JsonObject)?.get("type") as? JsonPrimitive
            if (sourceType != null && sourceType.contentOrNull != "vector") {
                issue("$path/source", StyleIssue.Kind.UNSUPPORTED, "The vector canvas only reads vector tile sources", id)
            }
        }
        for ((section, supported) in listOf("paint" to paintProperties.getValue(type), "layout" to layoutProperties.getValue(type))) {
            val properties = layer[section] as? JsonObject ?: continue
            properties.forEach { (name, value) ->
                val propertyPath = "$path/$section/${name.pointerToken()}"
                if (name !in supported) {
                    issue(propertyPath, StyleIssue.Kind.UNSUPPORTED, "'$name' is not rendered by $type layers", id)
                } else {
                    inspectExpression(value, propertyPath, id, issues, name.takeIf { it in literalArrayProperties })
                }
            }
        }
        (layer["filter"] as? JsonArray)?.let { inspectExpression(it, "$path/filter", id, issues) }
    }
    if (usedSources.size > 1) issue("/sources", StyleIssue.Kind.UNSUPPORTED, "The vector canvas uses one caller supplied tile source for all layers")
    return issues
}

internal fun isStyleExpressionOperator(operator: String): Boolean =
    operator in expressionOperators || operator in unsupportedArrayOperators

private fun inspectExpression(value: JsonElement, path: String, layerId: String?, issues: MutableList<StyleIssue>, literalArrayProperty: String? = null) {
    when (value) {
        is JsonObject -> {
            val stops = value["stops"] as? JsonArray
            if (stops == null) {
                value.forEach { (key, item) -> inspectExpression(item, "$path/${key.pointerToken()}", layerId, issues) }
            } else {
                value.keys.filterNot { key ->
                    key == "stops" || key == "base" || key == "type" && (value[key] as? JsonPrimitive)?.contentOrNull == "exponential"
                }.forEach { key ->
                    issues += StyleIssue("$path/${key.pointerToken()}", StyleIssue.Kind.UNSUPPORTED, "Function option '$key' is ignored; stops use zoom", layerId)
                }
                stops.forEachIndexed { index, stop ->
                    val pair = stop as? JsonArray
                    if (pair?.size == 2) inspectExpression(pair[1], "$path/stops/$index/1", layerId, issues, literalArrayProperty)
                }
            }
        }
        is JsonArray -> {
            val operator = (value.firstOrNull() as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull
            if (operator == null) {
                if (literalArrayProperty != null && value.any { it is JsonArray || it is JsonObject }) {
                    issues += StyleIssue(path, StyleIssue.Kind.UNSUPPORTED, "Nested expressions in a literal array are not evaluated", layerId)
                }
                return
            }
            if (literalArrayProperty == "text-font" && value.all { it is JsonPrimitive } &&
                operator !in expressionOperators && operator !in unsupportedArrayOperators
            ) return
            if (operator !in expressionOperators) {
                issues += StyleIssue(path, StyleIssue.Kind.UNSUPPORTED, "Expression operator '$operator' is not implemented", layerId)
                return
            }
            if (operator == "literal") return
            if (operator == "image" && value.size > 2) {
                issues += StyleIssue(path, StyleIssue.Kind.UNSUPPORTED, "Image expression options are not implemented", layerId)
            }
            if (operator == "in" && value.size != 3) {
                issues += StyleIssue(path, StyleIssue.Kind.UNSUPPORTED, "'in' requires one item and one collection; legacy filters are not implemented", layerId)
            }
            val indexes = when (operator) {
                "match" -> value.indices.filter { it == 1 || it == value.lastIndex || it >= 3 && it % 2 == 1 }
                "step" -> value.indices.filter { it == 1 || it == 2 || it >= 4 && it % 2 == 0 }
                "interpolate", "interpolate-hcl", "interpolate-lab" -> value.indices.filter { it == 2 || it >= 4 && it % 2 == 0 }
                else -> 1 until value.size
            }
            indexes.forEach { index -> inspectExpression(value[index], "$path/$index", layerId, issues) }
        }
        else -> Unit
    }
}

private fun String.pointerToken(): String = replace("~", "~0").replace("/", "~1")
