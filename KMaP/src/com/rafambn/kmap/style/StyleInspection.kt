package com.rafambn.kmap.style

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull

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
private val styleTokenPattern = Regex("\\{[^{}]+\\}")

internal fun hasStyleTokens(value: JsonElement?): Boolean =
    value is JsonPrimitive && value.isString && styleTokenPattern.containsMatchIn(value.content)

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
                } else if (type == "symbol" && section == "layout" &&
                    (name == "text-field" || name == "icon-image") && hasStyleTokens(value)
                ) {
                    issue(propertyPath, StyleIssue.Kind.UNSUPPORTED, "Tokenized strings are not supported; use expressions", id)
                } else {
                    inspectExpression(value, propertyPath, id, issues, name.takeIf { it in literalArrayProperties })
                }
            }
        }
        (layer["filter"] as? JsonArray)?.let { inspectExpression(it, "$path/filter", id, issues, filter = true) }
    }
    if (usedSources.size > 1) issue("/sources", StyleIssue.Kind.UNSUPPORTED, "The vector canvas uses one caller supplied tile source for all layers")
    return issues
}

internal fun isStyleExpressionOperator(operator: String): Boolean =
    operator in expressionOperators || operator in unsupportedArrayOperators

internal fun hasLegacyFilterSyntax(value: JsonElement): Boolean {
    val expression = value as? JsonArray ?: return false
    if (isLegacyFilterExpression(expression)) return true
    if ((expression.firstOrNull() as? JsonPrimitive)?.contentOrNull == "literal") return false
    return expressionValueIndexes(expression).any { hasLegacyFilterSyntax(expression[it]) }
}

private fun isLegacyFilterExpression(expression: JsonArray): Boolean {
    val operator = (expression.firstOrNull() as? JsonPrimitive)?.contentOrNull ?: return false
    if (operator in setOf("none", "!in", "!has")) return true
    if (operator == "has" && expression.size == 2 &&
        (expression[1] as? JsonPrimitive)?.contentOrNull == "\$id") return true
    if ((expression.getOrNull(1) as? JsonPrimitive)?.isString != true) return false
    return when (operator) {
        "==", "!=", ">", ">=", "<", "<=" -> expression.size == 3 && expression[2] is JsonPrimitive
        "in" -> expression.size >= 3 && expression.drop(2).all { it is JsonPrimitive }
        else -> false
    }
}

private fun expressionValueIndexes(expression: JsonArray): List<Int> = when (
    (expression.firstOrNull() as? JsonPrimitive)?.contentOrNull
) {
    "match" -> expression.indices.filter { it == 1 || it == expression.lastIndex || it >= 3 && it % 2 == 1 }
    "step" -> expression.indices.filter { it == 1 || it == 2 || it >= 4 && it % 2 == 0 }
    "interpolate", "interpolate-hcl", "interpolate-lab" ->
        expression.indices.filter { it == 2 || it >= 4 && it % 2 == 0 }
    else -> (1 until expression.size).toList()
}

private fun inspectExpressionArguments(
    expression: JsonArray, operator: String, path: String, layerId: String?, issues: MutableList<StyleIssue>
) {
    val count = expression.size - 1
    val validCount = when (operator) {
        "id", "geometry-type", "zoom", "e", "ln2", "pi" -> count == 0
        "-", "get", "has" -> count in 1..2
        "+", "*", "max", "min", "coalesce" -> count >= 2
        "index-of", "slice" -> count in 2..3
        "array" -> count in 1..3
        "image" -> count >= 1
        "boolean", "number", "object", "string", "to-color", "to-number" -> count >= 1
        "case" -> count >= 3 && count % 2 == 1
        "match", "step" -> count >= 4 && count % 2 == 0
        "interpolate", "interpolate-hcl", "interpolate-lab" -> count >= 6 && count % 2 == 0
        "rgba", "hsla" -> count == 4
        "rgb", "hsl" -> count == 3
        "/", "%", "^", "at", "split", "number-format", "==", "!=", ">", ">=", "<", "<=" -> count == 2
        "!", "length", "literal", "within", "to-boolean", "to-string", "typeof", "downcase", "upcase",
        "to-hsla", "to-rgba", "acos", "asin", "atan", "cos", "sin", "tan", "ln", "log10", "log2",
        "abs", "ceil", "floor", "round", "sqrt" -> count == 1
        else -> true
    }
    if (!validCount) {
        issues += StyleIssue(path, StyleIssue.Kind.INVALID, "Invalid number of arguments for '$operator'", layerId)
    }

    val numericIndexes = when (operator) {
        "+", "-", "*", "/", "%", "^", "max", "min", "rgb", "rgba", "hsl", "hsla",
        "acos", "asin", "atan", "cos", "sin", "tan", "ln", "log10", "log2", "abs", "ceil", "floor",
        "round", "sqrt" -> 1 until expression.size
        "at", "number-format" -> 1..1
        "index-of" -> 3..3
        "slice" -> 2..3
        "step" -> listOf(1) + (3 until expression.size step 2).toList()
        "interpolate", "interpolate-hcl", "interpolate-lab" -> listOf(2) + (3 until expression.size step 2).toList()
        else -> emptyList()
    }
    numericIndexes.forEach { index ->
        val literal = expression.getOrNull(index) as? JsonPrimitive ?: return@forEach
        if (literal.isString || literal.doubleOrNull == null) {
            issues += StyleIssue("$path/$index", StyleIssue.Kind.INVALID, "'$operator' requires a number here", layerId)
        }
    }

    val stringIndexes = when (operator) {
        "get", "has", "image", "downcase", "upcase" -> listOf(1)
        "split" -> listOf(1, 2)
        else -> emptyList()
    }
    stringIndexes.forEach { index ->
        val literal = expression.getOrNull(index) as? JsonPrimitive ?: return@forEach
        if (!literal.isString) {
            issues += StyleIssue("$path/$index", StyleIssue.Kind.INVALID, "'$operator' requires a string here", layerId)
        }
    }

    val booleanIndexes = when (operator) {
        "!", "all", "any" -> (1 until expression.size).toList()
        "case" -> (1 until expression.lastIndex step 2).toList()
        else -> emptyList()
    }
    booleanIndexes.forEach { index ->
        val literal = expression.getOrNull(index) as? JsonPrimitive ?: return@forEach
        if (literal.isString || literal.booleanOrNull == null) {
            issues += StyleIssue("$path/$index", StyleIssue.Kind.INVALID, "'$operator' requires a boolean here", layerId)
        }
    }
}

private fun inspectExpression(
    value: JsonElement, path: String, layerId: String?, issues: MutableList<StyleIssue>,
    literalArrayProperty: String? = null, filter: Boolean = false
) {
    when (value) {
        is JsonObject -> {
            if ("stops" in value || "property" in value) {
                issues += StyleIssue(path, StyleIssue.Kind.UNSUPPORTED, "Legacy style functions are not supported", layerId)
            } else {
                value.forEach { (key, item) ->
                    inspectExpression(item, "$path/${key.pointerToken()}", layerId, issues, filter = filter)
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
            if (filter && isLegacyFilterExpression(value)) {
                issues += StyleIssue(path, StyleIssue.Kind.UNSUPPORTED, "Legacy filter syntax is not supported", layerId)
                return
            }
            if (operator !in expressionOperators) {
                issues += StyleIssue(path, StyleIssue.Kind.UNSUPPORTED, "Expression operator '$operator' is not implemented", layerId)
                return
            }
            inspectExpressionArguments(value, operator, path, layerId, issues)
            if (operator == "literal") return
            if (operator == "image" && value.size > 2) {
                issues += StyleIssue(path, StyleIssue.Kind.UNSUPPORTED, "Image expression options are not implemented", layerId)
            }
            if (operator == "in" && value.size != 3) {
                issues += StyleIssue(path, StyleIssue.Kind.UNSUPPORTED, "'in' requires one item and one collection; legacy filters are not implemented", layerId)
            }
            expressionValueIndexes(value).forEach { index ->
                inspectExpression(value[index], "$path/$index", layerId, issues, filter = filter)
            }
        }
        else -> Unit
    }
}

private fun String.pointerToken(): String = replace("~", "~0").replace("/", "~1")
