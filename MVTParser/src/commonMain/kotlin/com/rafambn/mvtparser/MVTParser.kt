package com.rafambn.mvtparser

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf

const val CMD_MOVETO = 1
const val CMD_LINETO = 2
const val CMD_CLOSEPATH = 7

data class DecodedGeometry(
    val type: GeomType,
    val coordinates: List<List<Pair<Int, Int>>>
)

expect fun decompressGzip(compressedBytes: ByteArray): ByteArray

@OptIn(ExperimentalSerializationApi::class)
fun deserializeMVT(compressedBytes: ByteArray): MVTile {
    val decompressedBytes = decompressGzip(compressedBytes)
    return ProtoBuf.decodeFromByteArray(MVTile.serializer(), decompressedBytes)
}

fun parseMVT(mvtTile: MVTile): ParsedMVTile {
    val parsedLayers = mvtTile.layers.map { layer ->
        val parsedFeatures = layer.features.map { feature ->
            val decodedGeometry = decodeFeatureGeometry(feature, layer.extent)
            val properties = resolveFeatureProperties(feature, layer)

            ParsedFeature(
                id = if (feature.id != 0L) feature.id else null,
                type = feature.type,
                geometry = decodedGeometry.coordinates,
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

fun parseCompressedMVT(compressedBytes: ByteArray): ParsedMVTile {
    val mvtTile = deserializeMVT(compressedBytes)
    return parseMVT(mvtTile)
}

fun decodeZigZag(n: Int): Int {
    return if (n and 1 == 1) {
        -((n shr 1) + 1)
    } else {
        n shr 1
    }
}

fun decodeFeatureGeometry(feature: Feature, extent: Int): DecodedGeometry {
    val geometry = feature.geometry
    val geomType = feature.type
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
                // Unknown command, skip parameters
                cursor += count * 2 // Assuming 2 parameters per command
            }
        }
    }

    if (currentPart.isNotEmpty()) {
        allParts.add(currentPart)
    }

    return DecodedGeometry(geomType, allParts)
}

fun resolveFeatureProperties(feature: Feature, layer: Layer): Map<String, Any?> {
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
