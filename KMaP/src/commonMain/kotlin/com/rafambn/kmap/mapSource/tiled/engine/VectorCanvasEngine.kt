package com.rafambn.kmap.mapSource.tiled.engine

import androidx.compose.ui.graphics.Path
import com.rafambn.kmap.mapSource.tiled.TileResult
import com.rafambn.kmap.mapSource.tiled.tiles.OptimizedVectorTile
import com.rafambn.kmap.mapSource.tiled.tiles.VectorTile
import com.rafambn.kmap.utils.style.OptimizedStyle
import com.rafambn.kmap.utils.vectorTile.*
import kotlinx.coroutines.CoroutineScope

class VectorCanvasEngine(
    maxCacheTiles: Int,
    getTile: suspend (zoom: Int, row: Int, column: Int) -> TileResult<VectorTile>,
    coroutineScope: CoroutineScope,
    style: OptimizedStyle
) : CanvasEngine<OptimizedVectorTile>(
    maxCacheTiles,
    coroutineScope,
    TileRenderer(
        coroutineScope = coroutineScope,
        getTile = getTile,
        processTile = { optimizeMVTile(it, style) }
    )
)

private fun optimizeMVTile(tile: VectorTile, optimizedStyle: OptimizedStyle): OptimizedVectorTile {
    val mvtData = tile.mvtile ?: return OptimizedVectorTile(tile.zoom, tile.row, tile.col, null)

    val layerFeatures = mutableMapOf<String, MutableList<OptimizedRenderFeature>>()
    val extent = mvtData.layers.firstOrNull()?.extent ?: 4096

    optimizedStyle.layers.forEach { optimizedStyleLayer ->
        if (optimizedStyleLayer.type == "background") return@forEach

        val currentZoom = tile.zoom.toDouble()
        if (currentZoom < optimizedStyleLayer.minZoom) return@forEach
        if (currentZoom > optimizedStyleLayer.maxZoom) return@forEach

        val sourceLayerName = optimizedStyleLayer.sourceLayer ?: return@forEach
        val mvtLayer = mvtData.layers.find { it.name == sourceLayerName } ?: return@forEach

        val optimizedFeatures = mvtLayer.features.mapNotNull { feature ->
            val featureProperties = feature.properties.filterValues { it != null } as Map<String, Any>
            val geometryType = when (feature.type) {
                RawMVTGeomType.POINT -> "Point"
                RawMVTGeomType.LINESTRING -> "LineString"
                RawMVTGeomType.POLYGON -> "Polygon"
                else -> "Unknown"
            }
            val featureId = feature.id

            if (optimizedStyleLayer.filter?.evaluate(featureProperties, geometryType, featureId) == false) return@mapNotNull null

            val isValidGeometry = when (optimizedStyleLayer.type) {
                "fill" -> feature.type == RawMVTGeomType.POLYGON
                "line" -> feature.type == RawMVTGeomType.LINESTRING
                "symbol" -> feature.type == RawMVTGeomType.POINT
                else -> false
            }

            if (!isValidGeometry) return@mapNotNull null

            val geometry = buildOptimizedGeometry(feature) ?: return@mapNotNull null

            OptimizedRenderFeature(
                geometry = geometry,
                properties = featureProperties
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

private fun buildOptimizedGeometry(feature: MVTFeature): OptimizedGeometry? {
    return when (feature.type) {
        RawMVTGeomType.POLYGON -> {
            val paths = feature.geometry.map { ring -> buildPathFromGeometry(listOf(ring), true) }
            OptimizedGeometry.Polygon(paths)
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
