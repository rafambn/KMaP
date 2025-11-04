package com.rafambn.kmap.mapSource.tiled.vector

import androidx.compose.ui.graphics.*
import com.rafambn.kmap.utils.vectorTile.OptimizedGeometry
import com.rafambn.kmap.utils.vectorTile.OptimizedPaintProperties
import com.rafambn.kmap.utils.vectorTile.OptimizedRenderFeature

internal fun drawRenderFeature(
    canvas: Canvas,
    renderFeature: OptimizedRenderFeature,
    scaleAdjustment: Float,
) {
    when (renderFeature.geometry) {
        is OptimizedGeometry.Polygon if renderFeature.paintProperties is OptimizedPaintProperties.Fill -> {
            drawFillFeature(
                canvas, renderFeature.geometry, renderFeature.paintProperties
            )
        }

        is OptimizedGeometry.LineString if renderFeature.paintProperties is OptimizedPaintProperties.Line -> {
            drawLineFeature(
                canvas, renderFeature.geometry, renderFeature.paintProperties, scaleAdjustment
            )
        }

        is OptimizedGeometry.Point if renderFeature.paintProperties is OptimizedPaintProperties.Symbol -> {
            drawSymbolFeature(
                canvas, renderFeature.geometry, renderFeature.paintProperties
            )
        }

        else -> {}
    }
}

private fun drawFillFeature(
    canvas: Canvas,
    geometry: OptimizedGeometry.Polygon,
    properties: OptimizedPaintProperties.Fill,
) {
    geometry.paths.forEach { path ->
        val fillColor = (properties.color)?.copy(
            alpha = properties.opacity
        ) ?: Color.Red

        canvas.drawPath(
            path,
            Paint().apply {
                color = fillColor
                isAntiAlias = true
                style = PaintingStyle.Fill
            }
        )

        if (properties.outlineColor != null) {
            canvas.drawPath(
                path,
                Paint().apply {
                    color = properties.outlineColor
                    isAntiAlias = true
                    style = PaintingStyle.Stroke
                    strokeWidth = 1f
                }
            )
        }
    }
}

private fun drawLineFeature(
    canvas: Canvas,
    geometry: OptimizedGeometry.LineString,
    properties: OptimizedPaintProperties.Line,
    scaleAdjustment: Float,
) {
    canvas.drawPath(
        geometry.path,
        Paint().apply {
            color = properties.color?.copy(alpha = properties.opacity) ?: Color.Black
            isAntiAlias = true
            style = PaintingStyle.Stroke
            strokeWidth = properties.width * scaleAdjustment
            strokeCap = when (properties.cap) {
                "round" -> StrokeCap.Round
                "square" -> StrokeCap.Square
                else -> StrokeCap.Butt
            }
            strokeJoin = when (properties.join) {
                "round" -> StrokeJoin.Round
                "bevel" -> StrokeJoin.Bevel
                else -> StrokeJoin.Miter
            }
        }
    )
}

private fun drawSymbolFeature(
    canvas: Canvas,
    geometry: OptimizedGeometry.Point,
    properties: OptimizedPaintProperties.Symbol,
) {
    // Symbol layer rendering would go here
    // This includes text rendering with style properties like:
    // - field: which property to display
    // - size, color, opacity
    // - text-offset, text-anchor, text-justify
    // For now, this is left as a placeholder pending font rendering implementation
}
