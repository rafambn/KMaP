package com.rafambn.mvtparser

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf

/**
 * Constants for MVT command types
 */
const val CMD_MOVETO = 1u
const val CMD_LINETO = 2u
const val CMD_CLOSEPATH = 7u

/**
 * Data class to hold decoded geometry for a feature
 */
data class DecodedGeometry(
    val type: Tile.GeomType,
    val coordinates: List<List<Pair<Int, Int>>> // For multi-part geometries like polygons (outer ring, inner rings) or multi-line strings
)

/**
 * Decompresses GZIP-compressed data
 *
 * @param compressedBytes The compressed byte array
 * @return The decompressed byte array
 */
expect fun decompressGzip(compressedBytes: ByteArray): ByteArray

/**
 * Decodes a ZigZag-encoded integer (standard for protobuf signed integers)
 *
 * @param n The ZigZag-encoded integer
 * @return The decoded integer
 */
fun decodeZigZag(n: UInt): Int {
    return if (n and 1u == 1u) {
        -((n.toInt() shr 1) + 1)
    } else {
        n.toInt() shr 1
    }
}

/**
 * Decodes the geometry of a feature
 *
 * @param feature The feature to decode
 * @param extent The extent of the layer
 * @return The decoded geometry
 */
fun decodeFeatureGeometry(feature: Tile.Feature, extent: UInt): DecodedGeometry {
    val geometry = feature.geometry
    val geomType = feature.type
    var cursor = 0
    var x = 0
    var y = 0
    val allParts = mutableListOf<List<Pair<Int, Int>>>()
    var currentPart = mutableListOf<Pair<Int, Int>>()

    while (cursor < geometry.size) {
        val commandInteger = geometry[cursor++]
        val command = commandInteger and 0x7u // Lower 3 bits
        val count = commandInteger shr 3 // Next 3 bits

        when (command) {
            CMD_MOVETO -> {
                if (currentPart.isNotEmpty()) {
                    allParts.add(currentPart)
                    currentPart = mutableListOf()
                }
                repeat(count.toInt()) {
                    val dx = decodeZigZag(geometry[cursor++])
                    val dy = decodeZigZag(geometry[cursor++])
                    x += dx
                    y += dy
                    currentPart.add(x to y)
                }
            }
            CMD_LINETO -> {
                repeat(count.toInt()) {
                    val dx = decodeZigZag(geometry[cursor++])
                    val dy = decodeZigZag(geometry[cursor++])
                    x += dx
                    y += dy
                    currentPart.add(x to y)
                }
            }
            CMD_CLOSEPATH -> {
                // A ClosePath command consumes no parameters but acts on the current part
                if (currentPart.isNotEmpty()) {
                    // For polygons, the last point implicitly connects to the first.
                    // You might need to explicitly add the first point to close the loop for some rendering engines.
                    // currentPart.add(currentPart.first()) // Depends on your rendering needs
                    allParts.add(currentPart)
                    currentPart = mutableListOf() // Start new part for subsequent commands if any
                }
            }
            else -> {
                // Unknown command, handle error or skip
                // For simplicity, skip corresponding parameters
                cursor += (count * 2u).toInt() // Assuming 2 parameters per command
            }
        }
    }
    if (currentPart.isNotEmpty()) {
        allParts.add(currentPart)
    }

    return DecodedGeometry(geomType, allParts)
}

/**
 * Resolves the properties of a feature
 *
 * @param feature The feature to resolve properties for
 * @param layer The layer containing the feature
 * @return A map of property names to values
 */
fun resolveFeatureProperties(feature: Tile.Feature, layer: Tile.Layer): Map<String, Any?> {
    val properties = mutableMapOf<String, Any?>()
    val tags = feature.tags

    for (i in tags.indices step 2) {
        val keyIndex = tags[i].toInt()
        val valueIndex = tags[i + 1].toInt()

        if (keyIndex < layer.keys.size && valueIndex < layer.values.size) {
            val key = layer.keys[keyIndex]
            val rawValue = layer.values[valueIndex]

            // Convert the 'Value' message to a simpler Kotlin type
            val value: Any? = when {
                rawValue.stringValue != null -> rawValue.stringValue
                rawValue.floatValue != null -> rawValue.floatValue
                rawValue.doubleValue != null -> rawValue.doubleValue
                rawValue.intValue != null -> rawValue.intValue
                rawValue.uintValue != null -> rawValue.uintValue
                rawValue.sintValue != null -> rawValue.sintValue
                rawValue.boolValue != null -> rawValue.boolValue
                else -> null // Should not happen if proto is valid
            }
            properties[key] = value
        }
    }
    return properties
}

/**
 * Parses a MVT byte array
 *
 * @param bytes The MVT byte array
 * @return The parsed Tile
 */
@OptIn(ExperimentalSerializationApi::class)
fun parseMVT(bytes: ByteArray): Tile {
    val decompressedBytes = decompressGzip(bytes)
    return ProtoBuf.decodeFromByteArray(Tile.serializer(), decompressedBytes)
}
