package com.rafambn.kmap.source.internal

import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import com.rafambn.kmap.mvttile.*
import com.rafambn.kmap.source.VectorTile
import com.rafambn.kmap.style.compiled.CompiledLayerType
import com.rafambn.kmap.style.compiled.CompiledStyle
import com.rafambn.kmap.style.evaluation.FeatureGeometryContext
import com.rafambn.kmap.source.preparation.PreparedTile
import com.rafambn.kmap.source.TileSpecs

internal fun optimizeMVTile(tile: VectorTile, compiledStyle: CompiledStyle): OptimizedVectorTile {
    val mvtData = tile.mvtile ?: return OptimizedVectorTile(tile.zoom, tile.row, tile.col, null)

    val layerFeatures = mutableMapOf<String, MutableList<OptimizedRenderFeature>>()
    val extent = mvtData.layers.firstOrNull()?.extent ?: 4096

    compiledStyle.layers.forEach { optimizedStyleLayer ->
        if (optimizedStyleLayer.type == CompiledLayerType.BACKGROUND) return@forEach

        val sourceLayerName = optimizedStyleLayer.sourceLayer ?: return@forEach
        val mvtLayer = mvtData.layers.find { it.name == sourceLayerName } ?: return@forEach

        val optimizedFeatures = mvtLayer.features.mapNotNull { feature ->
            val featureProperties = feature.properties.mapNotNull { (key, value) -> value?.let { key to it } }.toMap()
            val geometryType = when (feature.type) {
                RawMVTGeomType.POINT -> "Point"
                RawMVTGeomType.LINESTRING -> "LineString"
                RawMVTGeomType.POLYGON -> "Polygon"
                else -> "Unknown"
            }
            val featureId = feature.id

            val featureGeometry = FeatureGeometryContext(
                feature.geometry, tile.zoom, tile.row, tile.col, mvtLayer.extent
            )
            if (optimizedStyleLayer.filter?.evaluate(tile.zoom.toDouble(), featureProperties, geometryType, featureId, featureGeometry) == false) return@mapNotNull null

            val isValidGeometry = when (optimizedStyleLayer.type) {
                CompiledLayerType.FILL -> feature.type == RawMVTGeomType.POLYGON
                CompiledLayerType.LINE -> feature.type == RawMVTGeomType.LINESTRING || feature.type == RawMVTGeomType.POLYGON
                CompiledLayerType.SYMBOL -> feature.type == RawMVTGeomType.POINT
                CompiledLayerType.BACKGROUND -> false
            }

            if (!isValidGeometry) return@mapNotNull null

            val geometry = buildOptimizedGeometry(feature) ?: return@mapNotNull null

            OptimizedRenderFeature(
                geometry = geometry,
                properties = featureProperties,
                id = featureId
            )
        }

        layerFeatures[optimizedStyleLayer.id] = optimizedFeatures.toMutableList()
    }

    val optimizedData = OptimizedMVTile(
        extent = extent,
        layerFeatures = layerFeatures
    )

    return OptimizedVectorTile(tile.zoom, tile.row, tile.col, optimizedData)
}

/** Creates graphics objects once on the engine owner; memberships share feature and Path instances. */
internal fun toOptimizedTile(specs: TileSpecs, prepared: PreparedTile): OptimizedVectorTile {
    val features = prepared.features.map { feature ->
        val scale = prepared.extent.toFloat() / feature.extent
        val geometry = when (feature.type) {
            RawMVTGeomType.POINT -> OptimizedGeometry.Point(
                (feature.coordinates.indices step 2).map { index ->
                    feature.coordinates[index] * scale to feature.coordinates[index + 1] * scale
                })
            RawMVTGeomType.POLYGON, RawMVTGeomType.LINESTRING -> {
                val path = Path()
                path.fillType = PathFillType.NonZero
                for (part in 0 until feature.parts.lastIndex) {
                    val start = feature.parts[part]
                    val end = feature.parts[part + 1]
                    if (start == end) continue
                    path.moveTo(feature.coordinates[start * 2] * scale, feature.coordinates[start * 2 + 1] * scale)
                    for (point in start + 1 until end) {
                        path.lineTo(feature.coordinates[point * 2] * scale, feature.coordinates[point * 2 + 1] * scale)
                    }
                    if (feature.type == RawMVTGeomType.POLYGON) path.close()
                }
                if (feature.type == RawMVTGeomType.POLYGON) OptimizedGeometry.Polygon(path)
                else OptimizedGeometry.LineString(path)
            }
            else -> error("Unknown prepared geometry")
        }
        OptimizedRenderFeature(geometry, feature.properties.mapValues { it.value.toValue() }, feature.id?.toULong())
    }
    val layers = prepared.layerFeatures.mapValues { (_, indices) -> indices.map { features[it] } }
    return OptimizedVectorTile(specs.zoom, specs.row, specs.col, OptimizedMVTile(prepared.extent, layers))
}

private fun buildOptimizedGeometry(feature: MVTFeature): OptimizedGeometry? {
    return when (feature.type) {
        RawMVTGeomType.POLYGON -> {
            val path = buildPathFromGeometry(feature.geometry, true).apply {
                fillType = PathFillType.NonZero
            }
            OptimizedGeometry.Polygon(path)
        }

        RawMVTGeomType.LINESTRING -> {
            val path = buildPathFromGeometry(feature.geometry, false)
            OptimizedGeometry.LineString(path)
        }

        RawMVTGeomType.POINT -> {
            val coordinates = feature.geometry.flatMap { ring ->
                ring.map { (x, y) -> Pair(x.toFloat(), y.toFloat()) }
            }
            OptimizedGeometry.Point(coordinates)
        }

        else -> null
    }
}


private fun buildPathFromGeometry(geometry: List<List<Pair<Int, Int>>>, isClosed: Boolean): Path {
    val path = Path()

    geometry.forEach { ring ->
        if (ring.isEmpty()) return@forEach

        val (startX, startY) = ring.first()
        path.moveTo(startX.toFloat(), startY.toFloat())

        ring.drop(1).forEach { (x, y) ->
            path.lineTo(x.toFloat(), y.toFloat())
        }

        if (isClosed)
            path.close()
    }

    return path
}
