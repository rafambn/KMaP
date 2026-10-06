@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.rafambn.kmap.source.preparation

import com.rafambn.kmap.mvttile.*
import com.rafambn.kmap.style.evaluation.EvaluationContext
import com.rafambn.kmap.style.evaluation.ExpressionEvaluator
import com.rafambn.kmap.style.evaluation.FeatureGeometryContext
import kotlinx.serialization.json.*
import kotlinx.serialization.protobuf.ProtoBuf

/** Worker-local evaluators. This object and its closures never cross the execution boundary. */
class TilePreparation(private val style: PreparationStyle) {
    private val evaluator = ExpressionEvaluator()
    private val filters = style.layers.map { it.filter?.toExpression() }

    fun prepare(bytes: ByteArray, zoom: Int, row: Int, col: Int): PreparedTile =
        prepare(ProtoBuf.decodeFromByteArray(RawMVTile.serializer(), bytes).parse(), zoom, row, col)

    fun prepare(tile: MVTile, zoom: Int, row: Int, col: Int): PreparedTile {
        val features = mutableListOf<PreparedFeature>()
        val membership = style.layers.associate { it.id to mutableListOf<Int>() }
        // Match the decoded compatibility path, which resolves the first occurrence of a source layer.
        for (layer in tile.layers.distinctBy { it.name }) {
            require(layer.extent > 0) { "MVT extent must be positive" }
            val matching = style.layers.indices.filter { style.layers[it].sourceLayer == layer.name }
            for (feature in layer.features) {
                val properties = feature.properties.mapNotNull { (key, value) -> value?.let { key to it } }.toMap()
                val context = EvaluationContext(
                    featureProperties = properties,
                    geometryType = when (feature.type) {
                        RawMVTGeomType.POINT -> "Point"
                        RawMVTGeomType.LINESTRING -> "LineString"
                        RawMVTGeomType.POLYGON -> "Polygon"
                        else -> "Unknown"
                    },
                    zoomLevel = zoom.toDouble(), featureId = feature.id, locale = style.locale,
                    featureGeometry = FeatureGeometryContext(feature.geometry, zoom, row, col, layer.extent),
                )
                val accepted = matching.filter { index ->
                    val valid = when (style.layers[index].type) {
                        "fill" -> feature.type == RawMVTGeomType.POLYGON
                        "line" -> feature.type == RawMVTGeomType.LINESTRING || feature.type == RawMVTGeomType.POLYGON
                        "symbol" -> feature.type == RawMVTGeomType.POINT
                        else -> false
                    }
                    valid && (filters[index] == null || evaluator.evaluate(filters[index], context) == true)
                }
                if (accepted.isEmpty()) continue
                val parts = IntArray(feature.geometry.size + 1)
                val coordinates = IntArray(feature.geometry.sumOf { it.size } * 2)
                var point = 0
                feature.geometry.forEachIndexed { index, part ->
                    parts[index] = point
                    for ((x, y) in part) {
                        coordinates[point * 2] = x
                        coordinates[point * 2 + 1] = y
                        point++
                    }
                }
                parts[parts.lastIndex] = point
                val featureIndex = features.size
                features.add(PreparedFeature(feature.type, layer.extent, coordinates, parts, feature.id?.toString(),
                    properties.mapValues { preparedValue(it.value) }))
                for (index in accepted) membership.getValue(style.layers[index].id).add(featureIndex)
            }
        }
        return PreparedTile(tile.layers.firstOrNull()?.extent ?: 4096, features, membership)
    }
}

private fun JsonElement.toExpression(): Any? = when (this) {
    JsonNull -> null
    is JsonArray -> map { it.toExpression() }
    is JsonObject -> mapValues { it.value.toExpression() }
    is JsonPrimitive -> if (isString) content else booleanOrNull ?: content.toDoubleOrNull() ?: content
}
