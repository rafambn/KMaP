package com.rafambn.kmap.source.internal.render

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.rafambn.kmap.geometry.plane.CanvasDrawReference
import com.rafambn.kmap.gesture.MapGestureWrapper
import com.rafambn.kmap.gesture.internal.mapGestures
import com.rafambn.kmap.mapProperties.TileDimension
import com.rafambn.kmap.mvttile.OptimizedGeometry
import com.rafambn.kmap.mvttile.OptimizedRenderFeature
import com.rafambn.kmap.source.Tile
import com.rafambn.kmap.source.internal.ActiveTiles
import com.rafambn.kmap.source.internal.OptimizedVectorTile
import com.rafambn.kmap.style.OptimizedStyle
import com.rafambn.kmap.style.OptimizedStyleLayer
import kotlin.math.pow

@Composable
fun VectorTileCanvas(
    gestureWrapper: MapGestureWrapper?,
    magnifierScale: () -> Float,
    positionOffset: () -> CanvasDrawReference,
    tileSize: () -> TileDimension,
    rotationDegrees: () -> Float,
    activeTiles: () -> ActiveTiles,
    style: () -> OptimizedStyle,
    zoom: () -> Double,
) {
    val fontResolver = LocalFontFamilyResolver.current
    val density = LocalDensity.current
    Layout(
        modifier = Modifier
            .mapGestures(gestureWrapper)
            .drawBehind {
                val rotation = rotationDegrees()
                val magnifierScale = magnifierScale()
                val tileSize = tileSize()
                val positionOffset = positionOffset()
                val activeTiles = activeTiles()
                val style = style()
                val zoom = zoom()
                val screenScale = 2F.pow(magnifierScale)

                withTransform({
                    translate(center.x, center.y)
                    rotate(rotation, Offset.Zero)
                    scale(screenScale, Offset.Zero)
                }) {
                    drawIntoCanvas { canvas ->
                        val backgroundLayer = style.layers.find { it.type == "background" }
                        backgroundLayer?.let {
                            drawBackgroundForActiveTiles(
                                it,
                                canvas,
                                tileSize,
                                positionOffset,
                                activeTiles,
                                zoom
                            )
                        }

                        drawStyleLayersWithTileClipping(
                            activeTiles.tiles,
                            activeTiles.currentZoom,
                            style,
                            tileSize,
                            positionOffset,
                            canvas,
                            fontResolver,
                            density,
                            zoom,
                            rotation,
                            screenScale
                        )
                    }
                }
            }
    ) { _, constraints ->
        layout(constraints.maxWidth, constraints.maxHeight) {}
    }
}

private fun DrawScope.drawStyleLayersWithTileClipping(
    tiles: List<Tile>,
    zoomLevel: Int,
    style: OptimizedStyle,
    tileSize: TileDimension,
    positionOffset: CanvasDrawReference,
    canvas: Canvas,
    fontResolver: FontFamily.Resolver,
    density: Density,
    zoom: Double,
    rotationDegrees: Float,
    screenScale: Float,
) {
    style.layers.filter { it.type != "background" }.forEach { styleLayer ->
        tiles.forEach { tile ->
            drawVectorTileLayerWithClipping(
                tile as OptimizedVectorTile,
                styleLayer,
                tileSize,
                positionOffset,
                2F.pow(zoomLevel - tile.zoom),
                canvas,
                fontResolver,
                density,
                zoom,
                rotationDegrees,
                screenScale
            )
        }
    }
}

private fun DrawScope.drawVectorTileLayerWithClipping(
    tile: OptimizedVectorTile,
    optimizedLayer: OptimizedStyleLayer,
    tileSize: TileDimension,
    positionOffset: CanvasDrawReference,
    scaleAdjustment: Float = 1F,
    canvas: Canvas,
    fontResolver: FontFamily.Resolver,
    density: Density,
    zoom: Double,
    rotationDegrees: Float,
    screenScale: Float,
) {
    val sizeX = (scaleAdjustment * tileSize.width.toPx())
    val sizeY = (scaleAdjustment * tileSize.height.toPx())
    canvas.withSave {
        tile.optimizedTile?.let { optimizedData ->
            val tileLeft = (tile.col * sizeX.toDouble() + positionOffset.x).toFloat()
            val tileTop = (tile.row * sizeY.toDouble() + positionOffset.y).toFloat()
            canvas.translate(tileLeft, tileTop)
            val scaleX = sizeX / optimizedData.extent.toFloat()
            val scaleY = sizeY / optimizedData.extent.toFloat()
            canvas.scale(scaleX, scaleY)
            val clipRect = Rect(
                0F,
                0F,
                optimizedData.extent.toFloat(),
                optimizedData.extent.toFloat()
            )
            canvas.clipRect(clipRect)
            optimizedData.layerFeatures[optimizedLayer.id]?.forEach { renderFeature ->
                drawRenderFeature(
                    canvas,
                    renderFeature,
                    fontResolver,
                    density,
                    optimizedLayer,
                    zoom,
                    optimizedData.extent.toFloat() / tileSize.height.toPx(),
                    rotationDegrees,
                    scaleX,
                    scaleY,
                    screenScale,
                )
            }
        }
    }
}

private fun DrawScope.drawBackgroundForActiveTiles(
    backgroundLayer: OptimizedStyleLayer,
    canvas: Canvas,
    tileSize: TileDimension,
    positionOffset: CanvasDrawReference,
    activeTiles: ActiveTiles,
    zoom: Double,
) {
    val backgroundColor =
        backgroundLayer.paint.properties["background-color"]?.evaluate(zoom, emptyMap(), "") as? Color ?: Color.Magenta
    val backgroundOpacity =
        (backgroundLayer.paint.properties["background-opacity"]?.evaluate(zoom, emptyMap(), "") as? Number)?.toFloat() ?: 1F

    val paint = Paint().apply {
        color = backgroundColor.copy(alpha = backgroundColor.alpha * backgroundOpacity)
        style = PaintingStyle.Fill
        isAntiAlias = false
    }

    activeTiles.tiles.forEach { tile ->
        canvas.withSave {
            val scaleAdjustment = 2F.pow(activeTiles.currentZoom - tile.zoom)
            val tileLeft = tileSize.width.toPx().toDouble() * tile.col * scaleAdjustment + positionOffset.x
            val tileTop = tileSize.height.toPx().toDouble() * tile.row * scaleAdjustment + positionOffset.y
            val tileRight = tileLeft + tileSize.width.toPx() * scaleAdjustment
            val tileBottom = tileTop + tileSize.height.toPx() * scaleAdjustment

            canvas.drawRect(
                Rect(
                    tileLeft.toFloat(),
                    tileTop.toFloat(),
                    tileRight.toFloat(),
                    tileBottom.toFloat()
                ),
                paint
            )
            val clipRect = Rect(tileLeft.toFloat(), tileTop.toFloat(), tileRight.toFloat(), tileBottom.toFloat())
            canvas.clipRect(clipRect)
        }
    }
}

internal fun DrawScope.drawRenderFeature(
    canvas: Canvas,
    renderFeature: OptimizedRenderFeature,
    fontResolver: FontFamily.Resolver,
    density: Density,
    optimizedStyleLayer: OptimizedStyleLayer,
    zoom: Double,
    textScale: Float,
    rotationDegrees: Float,
    tileScaleX: Float,
    tileScaleY: Float,
    screenScale: Float,
) {
    val geometry = renderFeature.geometry
    when (optimizedStyleLayer.type) {
        "fill" -> if (geometry is OptimizedGeometry.Polygon) {
            drawFillFeature(canvas, geometry, renderFeature.properties, optimizedStyleLayer, zoom, tileScaleX, tileScaleY, screenScale)
        }

        "line" -> {
            val path = when (geometry) {
                is OptimizedGeometry.LineString -> geometry.path
                is OptimizedGeometry.Polygon -> geometry.path
                is OptimizedGeometry.Point -> null
            }
            if (path != null) {
                drawLineFeature(canvas, path, renderFeature.properties, optimizedStyleLayer, zoom, tileScaleX, tileScaleY, screenScale)
            }
        }

        "symbol" -> if (geometry is OptimizedGeometry.Point) {
            drawSymbolFeature(canvas, geometry, renderFeature.properties, fontResolver, density, optimizedStyleLayer, zoom, textScale, rotationDegrees)
        }
    }
}

internal fun drawFillFeature(
    canvas: Canvas,
    geometry: OptimizedGeometry.Polygon,
    properties: Map<String, Any>,
    optimizedStyleLayer: OptimizedStyleLayer,
    zoom: Double,
    tileScaleX: Float,
    tileScaleY: Float,
    screenScale: Float,
) {
    val fillColor =
        optimizedStyleLayer.paint.properties["fill-color"]?.evaluate(zoom, properties, optimizedStyleLayer.id) as? Color ?: Color.Magenta
    val opacity =
        optimizedStyleLayer.paint.properties["fill-opacity"]?.evaluate(zoom, properties, optimizedStyleLayer.id) as? Double ?: 1.0
    val outlineColor =
        optimizedStyleLayer.paint.properties["fill-outline-color"]?.evaluate(zoom, properties, optimizedStyleLayer.id) as? Color
    val antialias =
        optimizedStyleLayer.paint.properties["fill-antialias"]?.evaluate(zoom, properties, optimizedStyleLayer.id) as? Boolean ?: true
    val fillAlpha = fillColor.alpha * opacity.toFloat()
    val groupDefaultOutline = antialias && outlineColor == null && fillAlpha < 1f

    val drawPaths = {
        canvas.drawPath(
            geometry.path,
            Paint().apply {
                color = fillColor.copy(alpha = if (groupDefaultOutline) 1f else fillAlpha)
                isAntiAlias = false
                style = PaintingStyle.Fill
            }
        )

        if (antialias) {
            val screenPath = Path().apply {
                fillType = geometry.path.fillType
                addPath(geometry.path)
                transform(Matrix().apply { scale(tileScaleX, tileScaleY) })
            }
            val strokeColor = outlineColor ?: fillColor
            canvas.withSave {
                canvas.scale(1f / tileScaleX, 1f / tileScaleY)
                if (outlineColor == null) canvas.clipPath(screenPath, ClipOp.Difference)
                canvas.drawPath(
                    screenPath,
                    Paint().apply {
                        color = strokeColor.copy(alpha = if (groupDefaultOutline) 1f else strokeColor.alpha * opacity.toFloat())
                        isAntiAlias = true
                        style = PaintingStyle.Stroke
                        strokeWidth = 1f / screenScale
                    }
                )
            }
        }
    }

    if (groupDefaultOutline) {
        val bounds = geometry.path.getBounds()
        val marginX = 2f / (tileScaleX * screenScale)
        val marginY = 2f / (tileScaleY * screenScale)
        canvas.withSaveLayer(
            Rect(bounds.left - marginX, bounds.top - marginY, bounds.right + marginX, bounds.bottom + marginY),
            Paint().apply { alpha = fillAlpha }
        ) { drawPaths() }
    } else {
        drawPaths()
    }
}

internal fun drawLineFeature(
    canvas: Canvas,
    path: Path,
    properties: Map<String, Any>,
    optimizedStyleLayer: OptimizedStyleLayer,
    zoom: Double,
    tileScaleX: Float,
    tileScaleY: Float,
    screenScale: Float,
) {
    val fillColor =
        optimizedStyleLayer.paint.properties["line-color"]?.evaluate(zoom, properties, optimizedStyleLayer.id) as? Color ?: Color.Magenta
    val width = (optimizedStyleLayer.paint.properties["line-width"]?.evaluate(zoom, properties, optimizedStyleLayer.id) as? Number)?.toFloat() ?: 1f
    if (width <= 0f) return
    val opacity = (optimizedStyleLayer.paint.properties["line-opacity"]?.evaluate(zoom, properties, optimizedStyleLayer.id) as? Number)?.toFloat() ?: 1f
    val cap = optimizedStyleLayer.layout.properties["line-cap"]?.evaluate(zoom, properties, optimizedStyleLayer.id) as? String ?: "butt"
    val join = optimizedStyleLayer.layout.properties["line-join"]?.evaluate(zoom, properties, optimizedStyleLayer.id) as? String ?: "miter"

    val screenPath = Path().apply {
        addPath(path)
        transform(Matrix().apply { scale(tileScaleX, tileScaleY) })
    }
    val strokePath = if (join == "none") Path().apply {
        val points = FloatArray(8)
        val segments = screenPath.iterator()
        var startX = 0f
        var startY = 0f
        var endX = 0f
        var endY = 0f

        while (segments.hasNext()) {
            when (segments.next(points)) {
                PathSegment.Type.Move -> {
                    startX = points[0]
                    startY = points[1]
                    endX = startX
                    endY = startY
                }
                PathSegment.Type.Line -> {
                    moveTo(points[0], points[1])
                    lineTo(points[2], points[3])
                    endX = points[2]
                    endY = points[3]
                }
                PathSegment.Type.Quadratic -> {
                    moveTo(points[0], points[1])
                    quadraticTo(points[2], points[3], points[4], points[5])
                    endX = points[4]
                    endY = points[5]
                }
                PathSegment.Type.Cubic -> {
                    moveTo(points[0], points[1])
                    cubicTo(points[2], points[3], points[4], points[5], points[6], points[7])
                    endX = points[6]
                    endY = points[7]
                }
                PathSegment.Type.Close -> {
                    if (endX != startX || endY != startY) {
                        moveTo(endX, endY)
                        lineTo(startX, startY)
                    }
                    endX = startX
                    endY = startY
                }
                else -> Unit
            }
        }
    } else screenPath
    canvas.withSave {
        canvas.scale(1f / tileScaleX, 1f / tileScaleY)
        canvas.drawPath(
            strokePath,
            Paint().apply {
                color = fillColor.copy(alpha = fillColor.alpha * opacity)
                isAntiAlias = true
                style = PaintingStyle.Stroke
                strokeWidth = width / screenScale
                strokeCap = when {
                    join == "none" -> StrokeCap.Butt
                    cap == "round" -> StrokeCap.Round
                    cap == "square" -> StrokeCap.Square
                    else -> StrokeCap.Butt
                }
                strokeJoin = when (join) {
                    "round" -> StrokeJoin.Round
                    "bevel" -> StrokeJoin.Bevel
                    else -> StrokeJoin.Miter
                }
            }
        )
    }
}

private fun DrawScope.drawSymbolFeature(
    canvas: Canvas,
    geometry: OptimizedGeometry.Point,
    properties: Map<String, Any>,
    fontResolver: FontFamily.Resolver,
    density: Density,
    optimizedStyleLayer: OptimizedStyleLayer,
    zoom: Double,
    textScale: Float,
    rotationDegrees: Float,
) {
    val text = optimizedStyleLayer.layout.properties["text-field"]?.evaluate(zoom, properties, optimizedStyleLayer.id) as? String
    text?.let {
        drawTextSymbol(canvas, geometry, properties, fontResolver, density, optimizedStyleLayer, 1.0, it, textScale, rotationDegrees)
    }
}

private fun DrawScope.drawTextSymbol(
    canvas: Canvas,
    geometry: OptimizedGeometry.Point,
    properties: Map<String, Any>,
    fontResolver: FontFamily.Resolver,
    density: Density,
    optimizedStyleLayer: OptimizedStyleLayer,
    zoomLevel: Double,
    text: String,
    textScale: Float,
    rotationDegrees: Float,
) {
    val layout = optimizedStyleLayer.layout.properties
    val paint = optimizedStyleLayer.paint.properties

    val transform = layout["text-transform"]?.evaluate(zoomLevel, properties, optimizedStyleLayer.id) as? String ?: "none"
    val transformedText = when (transform) {
        "uppercase" -> text.uppercase()
        "lowercase" -> text.lowercase()
        else -> text
    }

    val size = layout["text-size"]?.evaluate(zoomLevel, properties, optimizedStyleLayer.id) as? Double ?: 16.0
    val textColor = paint["text-color"]?.evaluate(zoomLevel, properties, optimizedStyleLayer.id) as? Color ?: Color.Black
    val opacity = paint["text-opacity"]?.evaluate(zoomLevel, properties, optimizedStyleLayer.id) as? Double ?: 1.0

    val haloColor = paint["text-halo-color"]?.evaluate(zoomLevel, properties, optimizedStyleLayer.id) as? Color
    val haloWidth = paint["text-halo-width"]?.evaluate(zoomLevel, properties, optimizedStyleLayer.id) as? Double ?: 0.0
    val haloBlur = paint["text-halo-blur"]?.evaluate(zoomLevel, properties, optimizedStyleLayer.id) as? Double ?: 0.0

    val maxWidth = layout["text-max-width"]?.evaluate(zoomLevel, properties, optimizedStyleLayer.id) as? Double
    val lineHeight = layout["text-line-height"]?.evaluate(zoomLevel, properties, optimizedStyleLayer.id) as? Double
    val justify = layout["text-justify"]?.evaluate(zoomLevel, properties, optimizedStyleLayer.id) as? String ?: "center"

    val anchor = layout["text-anchor"]?.evaluate(zoomLevel, properties, optimizedStyleLayer.id) as? String ?: "center"
    val offset = layout["text-offset"]?.evaluate(zoomLevel, properties, optimizedStyleLayer.id) as? List<*> ?: listOf(0.0, 0.0)
    val radialOffset = layout["text-radial-offset"]?.evaluate(zoomLevel, properties, optimizedStyleLayer.id) as? Double
    val translate = paint["text-translate"]?.evaluate(zoomLevel, properties, optimizedStyleLayer.id) as? List<*> ?: listOf(0.0, 0.0)
    val rotate = layout["text-rotate"]?.evaluate(zoomLevel, properties, optimizedStyleLayer.id) as? Double

    val finalSize = (size * textScale).sp
    val emSize = size.toFloat() * textScale

    val textStyle = TextStyle(
        fontSize = finalSize,
        lineHeight = lineHeight?.let { (it * emSize).sp } ?: TextUnit.Unspecified,
        textAlign = when (justify) {
            "left" -> TextAlign.Left
            "right" -> TextAlign.Right
            else -> TextAlign.Center
        },
    )

    val textMeasurer = TextMeasurer(
        defaultFontFamilyResolver = fontResolver,
        defaultDensity = density,
        defaultLayoutDirection = LayoutDirection.Ltr,
    )
    val constraints = Constraints(
        maxWidth = maxWidth?.let { (it * emSize).toInt() } ?: Constraints.Infinity
    )
    val textLayoutResult = textMeasurer.measure(
        text = AnnotatedString(transformedText),
        style = textStyle,
        overflow = TextOverflow.Visible,
        softWrap = maxWidth != null,
        maxLines = if (maxWidth != null) Int.MAX_VALUE else 1,
        constraints = constraints,
        layoutDirection = layoutDirection,
        density = this,
    )

    val textWidth = textLayoutResult.size.width
    val textHeight = textLayoutResult.size.height

    var anchorOffsetX = when {
        anchor.contains("left") -> 0f
        anchor.contains("right") -> -textWidth.toFloat()
        else -> -textWidth / 2f
    }
    var anchorOffsetY = when {
        anchor.contains("top") -> 0f
        anchor.contains("bottom") -> -textHeight.toFloat()
        else -> -textHeight / 2f
    }

    val offsetX = (offset.getOrNull(0) as? Number ?: 0.0).toFloat() * emSize
    val offsetY = (offset.getOrNull(1) as? Number ?: 0.0).toFloat() * emSize
    anchorOffsetX += offsetX
    anchorOffsetY += offsetY

    if (radialOffset != null && radialOffset > 0) {
        anchorOffsetY -= (radialOffset * emSize).toFloat()
    }

    val translateX = (translate.getOrNull(0) as? Number ?: 0.0).toFloat()
    val translateY = (translate.getOrNull(1) as? Number ?: 0.0).toFloat()

    geometry.coordinates.forEach { (x, y) ->
        withTransform({
            translate(
                left = x + anchorOffsetX + translateX,
                top = y + anchorOffsetY + translateY
            )
            rotate(-rotationDegrees + (rotate?.toFloat() ?: 0F), Offset(-anchorOffsetX, -anchorOffsetY))
        }) {
            if (haloColor != null && haloWidth > 0) {
                textLayoutResult.multiParagraph.paint(
                    canvas = drawContext.canvas,
                    color = haloColor,
                    shadow = if (haloBlur > 0) Shadow(
                        color = haloColor,
                        blurRadius = haloBlur.toFloat()
                    ) else null,
                    drawStyle = Stroke(
                        width = haloWidth.toFloat() * 2,
                        join = StrokeJoin.Round,
                        cap = StrokeCap.Round
                    )
                )
            }

            textLayoutResult.multiParagraph.paint(
                canvas = drawContext.canvas,
                color = textColor.copy(alpha = opacity.toFloat()),
                drawStyle = Fill,
                blendMode = DrawScope.DefaultBlendMode
            )
        }
    }
}
