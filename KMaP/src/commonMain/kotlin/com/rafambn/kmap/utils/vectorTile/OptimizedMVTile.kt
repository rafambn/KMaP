package com.rafambn.kmap.utils.vectorTile

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path as ComposePath

data class OptimizedMVTile(
    val extent: Int = 4096,
    val layerFeatures: Map<String, List<OptimizedRenderFeature>> = emptyMap(),
    val backgroundFeature: OptimizedRenderFeature? = null
)

data class OptimizedRenderFeature(
    val geometry: OptimizedGeometry,
    val properties: Map<String, Any?>,
    val paintProperties: OptimizedPaintProperties
)

sealed class OptimizedGeometry {
    data class Polygon(val paths: List<ComposePath>) : OptimizedGeometry()
    data class LineString(val path: ComposePath) : OptimizedGeometry()
    data class Point(val coordinates: List<Pair<Float, Float>>) : OptimizedGeometry()
}

sealed class OptimizedPaintProperties {
    data class Fill(
        val color: Color? = null,
        val opacity: Float = 1.0f,
        val outlineColor: Color? = null
    ) : OptimizedPaintProperties()

    data class Line(
        val color: Color? = null,
        val width: Float = 1.0f,
        val opacity: Float = 1.0f,
        val cap: String = "butt",
        val join: String = "miter"
    ) : OptimizedPaintProperties()

    data class Background(
        val color: Color? = null,
        val opacity: Float = 1.0f
    ) : OptimizedPaintProperties()

    data class Symbol(
        val field: String? = null,
        val size: Float = 12.0f,
        val color: Color? = null,
        val opacity: Float = 1.0f
    ) : OptimizedPaintProperties()
}
