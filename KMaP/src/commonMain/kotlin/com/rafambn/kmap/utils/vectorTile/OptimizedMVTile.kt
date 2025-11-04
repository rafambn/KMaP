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

data class OptimizedPaintProperties(//TODO this could be separated
    // Fill properties
    val fillColor: Color? = null,
    val fillOpacity: Float = 1.0f,
    val fillOutlineColor: Color? = null,

    // Line properties
    val lineColor: Color? = null,
    val lineWidth: Float = 1.0f,
    val lineOpacity: Float = 1.0f,
    val lineCap: String = "butt",
    val lineJoin: String = "miter",

    // Circle properties
    val circleRadius: Float = 5.0f,
    val circleColor: Color? = null,
    val circleOpacity: Float = 1.0f,
    val circleStrokeColor: Color? = null,
    val circleStrokeWidth: Float = 0.0f,

    // Background properties
    val backgroundColor: Color? = null,
    val backgroundOpacity: Float = 1.0f,

    // Symbol properties (text labels - future enhancement)
    val textField: String? = null,
    val textSize: Float = 12.0f,
    val textColor: Color? = null,
    val textOpacity: Float = 1.0f
)
