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
                    canvas, renderFeature.geometry, renderFeature.paintProperties, tileOffsetPx, tileSize, scaleAdjustment
                )

                is OptimizedGeometry.LineString -> drawLineFeature(
                    canvas, renderFeature.geometry, renderFeature.paintProperties, tileOffsetPx, tileSize, scaleAdjustment
                )

                is OptimizedGeometry.Point -> drawSymbolFeature(
                    canvas, renderFeature.geometry, renderFeature.paintProperties, tileOffsetPx, tileSize, scaleAdjustment
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
    scaleAdjustment: Float,
) {
    geometry.paths.forEach { path ->
        val transformedPath = transformPathToTile(path, tileOffset, tileSize, scaleAdjustment)

        canvas.drawPath(
            transformedPath,
            Paint().apply {
                color = properties.fillColor?.copy(alpha = properties.fillOpacity) ?: androidx.compose.ui.graphics.Color.Red
                isAntiAlias = true
                style = PaintingStyle.Fill
            }
        )

        if (properties.fillOutlineColor != null) {
            canvas.drawPath(
                transformedPath,
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
    tileOffset: Offset,
    tileSize: TileDimension,
    scaleAdjustment: Float,
) {
    val transformedPath = transformPathToTile(geometry.path, tileOffset, tileSize, scaleAdjustment)

    canvas.drawPath(
        transformedPath,
        Paint().apply {
            color = properties.lineColor?.copy(alpha = properties.lineOpacity) ?: androidx.compose.ui.graphics.Color.Black
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
    tileOffset: Offset,
    tileSize: TileDimension,
    scaleAdjustment: Float,
) {
    // Symbol layer rendering would go here
    // This includes text rendering with style properties like:
    // - textField: which property to display
    // - textSize, textColor, textOpacity
    // - text-offset, text-anchor, text-justify
    // For now, this is left as a placeholder pending font rendering implementation
}

private fun transformPathToTile(
    path: Path,
    tileOffset: Offset,
    tileSize: TileDimension,
    scaleAdjustment: Float,
): Path {
    //TODO fix this transformation
    // Note: Direct path transformation is not available in Compose Canvas API
    // In practice, paths should already be in the correct coordinate space when built during optimization
    // This is a placeholder for future optimization if needed
    return path
}
