package com.rafambn.kmap.utils.vectorTile

import androidx.compose.ui.graphics.Path

data class OptimizedMVTile(
    val extent: Int = 4096,
    val layerFeatures: Map<String, List<OptimizedRenderFeature>> = emptyMap(),
)

data class OptimizedRenderFeature(
    val geometry: OptimizedGeometry,
    val properties: Map<String, Any>,
)

sealed class OptimizedGeometry {
    data class Polygon(val paths: List<Path>) : OptimizedGeometry()
    data class LineString(val path: Path) : OptimizedGeometry()
    data class Point(val coordinates: List<Pair<Float, Float>>) : OptimizedGeometry()
}
