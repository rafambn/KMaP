package com.rafambn.kmap.mapSource.tiled.vector

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.rafambn.kmap.mapSource.tiled.OptimizedVectorTile
import com.rafambn.kmap.mapSource.tiled.TileDimension
import com.rafambn.kmap.utils.CanvasDrawReference
import com.rafambn.kmap.utils.vectorTile.OptimizedGeometry
import com.rafambn.kmap.utils.vectorTile.OptimizedPaintProperties

internal fun DrawScope.drawVectorTile(
    tile: OptimizedVectorTile,
    tileSize: TileDimension,
    positionOffset: CanvasDrawReference,
    scaleAdjustment: Float = 1F,
    canvas: Canvas,
) {
    tile.optimizedTile?.let { optimizedData ->
        val tileOffsetX = tileSize.width * tile.row * scaleAdjustment + positionOffset.x
        val tileOffsetY = tileSize.height * tile.col * scaleAdjustment + positionOffset.y
        val tileOffsetPx = Offset(tileOffsetX.toFloat(), tileOffsetY.toFloat())

        optimizedData.renderFeature.forEach { renderFeature ->
            when (renderFeature.geometry) {
                is OptimizedGeometry.Polygon -> drawFillFeature(
                    canvas, renderFeature.geometry, renderFeature.paintProperties, tileOffsetPx, tileSize, optimizedData.extent, scaleAdjustment
                )

                is OptimizedGeometry.LineString -> drawLineFeature(
                    canvas, renderFeature.geometry, renderFeature.paintProperties, tileOffsetPx, tileSize, optimizedData.extent, scaleAdjustment
                )

                is OptimizedGeometry.Point -> drawSymbolFeature(
                    canvas, renderFeature.geometry, renderFeature.paintProperties, tileOffsetPx, tileSize, optimizedData.extent, scaleAdjustment
                )
            }
        }
    }
}

private fun drawFillFeature(
    canvas: Canvas,
    geometry: OptimizedGeometry.Polygon,
    properties: OptimizedPaintProperties,
    tileOffset: Offset,
    tileSize: TileDimension,
    mvtExtent: Int,
    scaleAdjustment: Float,
) {
    geometry.paths.forEach { path ->
        canvas.save()
        transformCanvasForTile(canvas, tileOffset, tileSize, mvtExtent, scaleAdjustment)

        canvas.drawPath(
            path,
            Paint().apply {
                color = properties.fillColor?.copy(alpha = properties.fillOpacity) ?: Color.Red
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

        canvas.restore()
    }
}

private fun drawLineFeature(
    canvas: Canvas,
    geometry: OptimizedGeometry.LineString,
    properties: OptimizedPaintProperties,
    tileOffset: Offset,
    tileSize: TileDimension,
    mvtExtent: Int,
    scaleAdjustment: Float,
) {
    canvas.save()
    transformCanvasForTile(canvas, tileOffset, tileSize, mvtExtent, scaleAdjustment)

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

    canvas.restore()
}

private fun drawSymbolFeature(
    canvas: Canvas,
    geometry: OptimizedGeometry.Point,
    properties: OptimizedPaintProperties,
    tileOffset: Offset,
    tileSize: TileDimension,
    mvtExtent: Int,
    scaleAdjustment: Float,
) {
    // Symbol layer rendering would go here
    // This includes text rendering with style properties like:
    // - textField: which property to display
    // - textSize, textColor, textOpacity
    // - text-offset, text-anchor, text-justify
    // For now, this is left as a placeholder pending font rendering implementation
}

private fun transformCanvasForTile(
    canvas: Canvas,
    tileOffset: Offset,
    tileSize: TileDimension,
    mvtExtent: Int,
    scaleAdjustment: Float,
) {
    // Translate to tile position
    canvas.translate(tileOffset.x, tileOffset.y)

    // Scale from MVT extent coordinates to tile size
    val scaleX = (tileSize.width * scaleAdjustment) / mvtExtent
    val scaleY = (tileSize.height * scaleAdjustment) / mvtExtent
    canvas.scale(scaleX, scaleY)
}
