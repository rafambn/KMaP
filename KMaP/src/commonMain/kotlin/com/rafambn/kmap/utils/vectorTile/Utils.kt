package com.rafambn.kmap.utils.vectorTile


const val CMD_MOVETO = 1
const val CMD_LINETO = 2
const val CMD_CLOSEPATH = 7

fun MVTile.parse(): ParsedMVTile {
    val parsedLayers = this.layers.map { layer ->
        val parsedFeatures = layer.features.map { feature ->
            val decodedGeometry = decodeFeatureGeometry(feature)
            val properties = resolveFeatureProperties(feature, layer)

            ParsedFeature(
                id = if (feature.id != 0L) feature.id else null,
                type = feature.type,
                geometry = decodedGeometry,
                properties = properties
            )
        }

        ParsedLayer(
            name = layer.name,
            extent = layer.extent,
            features = parsedFeatures
        )
    }

    return ParsedMVTile(parsedLayers)
}

fun ParsedMVTile.deparse(): MVTile {
    val layers = this.layers.map { parsedLayer ->
        val keys = mutableListOf<String>()
        val values = mutableListOf<Value>()

        val features = parsedLayer.features.map { parsedFeature ->
            val geometry = encodeFeatureGeometry(parsedFeature.geometry, parsedFeature.type)
            val tags = encodeFeatureProperties(parsedFeature.properties, keys, values)

            Feature(
                id = parsedFeature.id ?: 0L,
                tags = tags,
                type = parsedFeature.type,
                geometry = geometry
            )
        }

        Layer(
            name = parsedLayer.name,
            extent = parsedLayer.extent,
            keys = keys,
            values = values,
            features = features
        )
    }

    return MVTile(layers = layers)
}

internal fun decodeZigZag(n: Int): Int {
    return if (n and 1 == 1) {
        -((n shr 1) + 1)
    } else {
        n shr 1
    }
}

internal fun encodeZigZag(n: Int): Int {
    return (n shl 1) xor (n shr 31)
}

internal fun decodeFeatureGeometry(feature: Feature): List<List<Pair<Int, Int>>> {
    val geometry = feature.geometry
    var cursor = 0
    var x = 0
    var y = 0
    val allParts = mutableListOf<List<Pair<Int, Int>>>()
    var currentPart = mutableListOf<Pair<Int, Int>>()

    while (cursor < geometry.size) {
        val commandInteger = geometry[cursor++]
        val command = commandInteger and 0x7
        val count = commandInteger shr 3

        when (command) {
            CMD_MOVETO -> {
                if (currentPart.isNotEmpty()) {
                    allParts.add(currentPart)
                    currentPart = mutableListOf()
                }
                repeat(count) {
                    val dx = decodeZigZag(geometry[cursor++])
                    val dy = decodeZigZag(geometry[cursor++])
                    x += dx
                    y += dy
                    currentPart.add(x to y)
                }
            }
            CMD_LINETO -> {
                repeat(count) {
                    val dx = decodeZigZag(geometry[cursor++])
                    val dy = decodeZigZag(geometry[cursor++])
                    x += dx
                    y += dy
                    currentPart.add(x to y)
                }
            }
            CMD_CLOSEPATH -> {
                if (currentPart.isNotEmpty()) {
                    allParts.add(currentPart)
                    currentPart = mutableListOf()
                }
            }
            else -> {
                cursor += count * 2
            }
        }
    }

    if (currentPart.isNotEmpty()) {
        allParts.add(currentPart)
    }

    return allParts
}

internal fun encodeFeatureGeometry(coordinates: List<List<Pair<Int, Int>>>, type: GeomType): List<Int> {
    if (coordinates.isEmpty()) return emptyList()

    val geometry = mutableListOf<Int>()
    var lastX = 0
    var lastY = 0

    coordinates.forEach { part ->
        if (part.isEmpty()) return@forEach

        val firstPoint = part[0]
        val dx1 = firstPoint.first - lastX
        val dy1 = firstPoint.second - lastY
        lastX = firstPoint.first
        lastY = firstPoint.second

        geometry.add(CMD_MOVETO or (1 shl 3))
        geometry.add(encodeZigZag(dx1))
        geometry.add(encodeZigZag(dy1))

        if (part.size > 1) {
            val remainingPoints = part.drop(1)
            geometry.add(CMD_LINETO or (remainingPoints.size shl 3))

            remainingPoints.forEach { point ->
                val dx = point.first - lastX
                val dy = point.second - lastY
                lastX = point.first
                lastY = point.second

                geometry.add(encodeZigZag(dx))
                geometry.add(encodeZigZag(dy))
            }
        }

        if (type == GeomType.POLYGON) {
            geometry.add(CMD_CLOSEPATH)
        }
    }

    return geometry
}

internal fun resolveFeatureProperties(feature: Feature, layer: Layer): Map<String, Any?> {
    val properties = mutableMapOf<String, Any?>()
    val tags = feature.tags

    for (i in tags.indices step 2) {
        if (i + 1 < tags.size) {
            val keyIndex = tags[i]
            val valueIndex = tags[i + 1]

            if (keyIndex < layer.keys.size && valueIndex < layer.values.size) {
                val key = layer.keys[keyIndex]
                val rawValue = layer.values[valueIndex]

                val value: Any? = when {
                    rawValue.string_value != null -> rawValue.string_value
                    rawValue.float_value != null -> rawValue.float_value
                    rawValue.double_value != null -> rawValue.double_value
                    rawValue.int_value != null -> rawValue.int_value
                    rawValue.uint_value != null -> rawValue.uint_value
                    rawValue.sint_value != null -> rawValue.sint_value
                    rawValue.bool_value != null -> rawValue.bool_value
                    else -> null
                }
                properties[key] = value
            }
        }
    }
    return properties
}

internal fun encodeFeatureProperties(properties: Map<String, Any?>, keys: MutableList<String>, values: MutableList<Value>): List<Int> {
    val tags = mutableListOf<Int>()

    properties.forEach { (key, value) ->
        if (value == null) return@forEach

        val keyIndex = keys.indexOf(key).let { index ->
            if (index == -1) {
                keys.add(key)
                keys.size - 1
            } else {
                index
            }
        }

        val valueObj = when (value) {
            is String -> Value(string_value = value)
            is Float -> Value(float_value = value)
            is Double -> Value(double_value = value)
            is Int -> Value(int_value = value.toLong())
            is Long -> Value(int_value = value)
            is Boolean -> Value(bool_value = value)
            else -> Value(string_value = value.toString())
        }

        val valueIndex = values.indexOfFirst { existingValue ->
            existingValue.string_value == valueObj.string_value &&
            existingValue.float_value == valueObj.float_value &&
            existingValue.double_value == valueObj.double_value &&
            existingValue.int_value == valueObj.int_value &&
            existingValue.uint_value == valueObj.uint_value &&
            existingValue.sint_value == valueObj.sint_value &&
            existingValue.bool_value == valueObj.bool_value
        }.let { index ->
            if (index == -1) {
                values.add(valueObj)
                values.size - 1
            } else {
                index
            }
        }

        tags.add(keyIndex)
        tags.add(valueIndex)
    }

    return tags
}
