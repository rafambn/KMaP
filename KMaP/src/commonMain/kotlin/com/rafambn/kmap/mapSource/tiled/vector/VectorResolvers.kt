package com.rafambn.kmap.mapSource.tiled.vector

import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import com.rafambn.kmap.utils.vectorTile.OptimizedGeometry
import com.rafambn.kmap.utils.vectorTile.OptimizedPaintProperties
import com.rafambn.kmap.utils.vectorTile.OptimizedRenderFeature

internal fun DrawScope.drawRenderFeature(
    canvas: Canvas,
    renderFeature: OptimizedRenderFeature,
    scaleAdjustment: Float,
    fontResolver: FontFamily.Resolver,
    density: Density
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
                canvas, renderFeature.geometry, renderFeature.paintProperties, fontResolver, density
            )
        }

        else -> {}
    }
}

private fun DrawScope.drawFillFeature(
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

private fun DrawScope.drawLineFeature(
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

private fun DrawScope.drawSymbolFeature(
    canvas: Canvas,
    geometry: OptimizedGeometry.Point,
    properties: OptimizedPaintProperties.Symbol,
    fontResolver: FontFamily.Resolver,
    density: Density
) {
    // Text symbol rendering
    if (properties.field != null) {
        drawTextSymbol(canvas, geometry, properties, fontResolver, density)
    }

    // TODO: Image symbol rendering would go here
    // This includes:
    // - icon-image: which image to display
    // - icon-size, icon-opacity, icon-rotation
    // - icon-offset, icon-anchor
    // - image resource loading and caching
    // Pending implementation of image symbol system
}

private fun DrawScope.drawTextSymbol(
    canvas: Canvas,
    geometry: OptimizedGeometry.Point,
    properties: OptimizedPaintProperties.Symbol,
    fontResolver: FontFamily.Resolver,
    density: Density
) {
    val textColor = (properties.color ?: Color.Magenta).copy(alpha = properties.opacity)
    geometry.coordinates.forEach { (x, y) ->
        val textLayoutResult =
            TextMeasurer(
                defaultFontFamilyResolver = fontResolver,
                defaultDensity = density,
                defaultLayoutDirection = LayoutDirection.Ltr,
            ).measure(
                text = AnnotatedString(properties.field!!),
                style = TextStyle(
                    color = textColor,
                    fontSize = 150.sp,
                ),
                overflow = TextOverflow.Visible,
                softWrap = true,
                maxLines = 1,
                constraints = Constraints(0, this.size.width.toInt(), 0, this.size.height.toInt()),
                layoutDirection = layoutDirection,
                density = this,
            )
        withTransform({
            translate(x - textLayoutResult.size.width / 2, y - textLayoutResult.size.height / 2)
        }) {
            textLayoutResult.multiParagraph.paint(canvas = drawContext.canvas, blendMode = DrawScope.DefaultBlendMode)
        }
    }
}
