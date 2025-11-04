package com.rafambn.kmap.mapSource.tiled.vector

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.rafambn.kmap.mapSource.tiled.OptimizedVectorTile
import com.rafambn.kmap.mapSource.tiled.TileDimension
import com.rafambn.kmap.utils.CanvasDrawReference
import com.rafambn.kmap.utils.vectorTile.OptimizedGeometry
import com.rafambn.kmap.utils.vectorTile.OptimizedPaintProperties
import com.rafambn.kmap.utils.vectorTile.OptimizedRenderFeature

internal fun DrawScope.drawVectorTile(
    tile: OptimizedVectorTile,
    tileSize: TileDimension,
    positionOffset: CanvasDrawReference,
    scaleAdjustment: Float = 1F,
    canvas: Canvas,
) {
    tile.optimizedTile?.let { optimizedData ->
        val tileOffsetX = tileSize.width * tile.col * scaleAdjustment + positionOffset.x
        val tileOffsetY = tileSize.height * tile.row * scaleAdjustment + positionOffset.y
        val tileOffsetPx = Offset(tileOffsetX.toFloat(), tileOffsetY.toFloat())

        canvas.save()
        canvas.translate(tileOffsetPx.x, tileOffsetPx.y)

        val scaleX = (tileSize.width * scaleAdjustment) / optimizedData.extent
        val scaleY = (tileSize.height * scaleAdjustment) / optimizedData.extent
        canvas.scale(scaleX, scaleY)

        optimizedData.layerFeatures.forEach { (_, features) ->
            features.forEach { renderFeature ->
                drawRenderFeature(canvas, renderFeature, scaleAdjustment)
            }
        }

        canvas.restore()
    }
}

internal fun DrawScope.drawVectorTileLayer(
    tile: OptimizedVectorTile,
    layerId: String,
    tileSize: TileDimension,
    positionOffset: CanvasDrawReference,
    scaleAdjustment: Float = 1F,
    canvas: Canvas,
) {
    tile.optimizedTile?.let { optimizedData ->
        val tileOffsetX = tileSize.width * tile.col * scaleAdjustment + positionOffset.x
        val tileOffsetY = tileSize.height * tile.row * scaleAdjustment + positionOffset.y
        val tileOffsetPx = Offset(tileOffsetX.toFloat(), tileOffsetY.toFloat())

        canvas.save()
        canvas.translate(tileOffsetPx.x, tileOffsetPx.y)

        val scaleX = (tileSize.width * scaleAdjustment) / optimizedData.extent
        val scaleY = (tileSize.height * scaleAdjustment) / optimizedData.extent
        canvas.scale(scaleX, scaleY)

        optimizedData.layerFeatures[layerId]?.forEach { renderFeature ->
            drawRenderFeature(canvas, renderFeature, scaleAdjustment)
        }

        canvas.restore()
    }
}

internal fun drawRenderFeature(
    canvas: Canvas,
    renderFeature: OptimizedRenderFeature,
    scaleAdjustment: Float,
) {
    when (renderFeature.geometry) {
        is OptimizedGeometry.Polygon -> drawFillFeature(
            canvas, renderFeature.geometry, renderFeature.paintProperties
        )

        is OptimizedGeometry.LineString -> drawLineFeature(
            canvas, renderFeature.geometry, renderFeature.paintProperties, scaleAdjustment
        )

        is OptimizedGeometry.Point -> drawSymbolFeature(
            canvas, renderFeature.geometry, renderFeature.paintProperties
        )
    }
}

private fun drawFillFeature(
    canvas: Canvas,
    geometry: OptimizedGeometry.Polygon,
    properties: OptimizedPaintProperties,
) {
    geometry.paths.forEach { path ->
        val fillColor = (properties.backgroundColor ?: properties.fillColor)?.copy(
            alpha = (properties.backgroundColor?.let { properties.backgroundOpacity } ?: properties.fillOpacity)
        ) ?: Color.Red

        canvas.drawPath(
            path,
            Paint().apply {
                color = fillColor
                isAntiAlias = true
                style = PaintingStyle.Fill
            }
        )

        if (properties.fillOutlineColor != null) {
            canvas.drawPath(
                path,
                Paint().apply {
                    color = properties.fillOutlineColor
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
    properties: OptimizedPaintProperties,
    scaleAdjustment: Float,
) {
    canvas.drawPath(
        geometry.path,
        Paint().apply {
            color = properties.lineColor?.copy(alpha = properties.lineOpacity) ?: Color.Black
            isAntiAlias = true
            style = PaintingStyle.Stroke
            strokeWidth = properties.lineWidth * scaleAdjustment
            strokeCap = when (properties.lineCap) {
                "round" -> StrokeCap.Round
                "square" -> StrokeCap.Square
                else -> StrokeCap.Butt
            }
            strokeJoin = when (properties.lineJoin) {
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
    properties: OptimizedPaintProperties,
) {
    // Symbol layer rendering would go here
    // This includes text rendering with style properties like:
    // - textField: which property to display
    // - textSize, textColor, textOpacity
    // - text-offset, text-anchor, text-justify
    // For now, this is left as a placeholder pending font rendering implementation
}
