package com.rafambn.kmap.mapSource.tiled.canvas

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.rafambn.kmap.gestures.MapGestureWrapper
import com.rafambn.kmap.mapProperties.TileDimension
import com.rafambn.kmap.mapSource.tiled.ActiveTiles
import com.rafambn.kmap.mapSource.tiled.tiles.OptimizedVectorTile
import com.rafambn.kmap.mapSource.tiled.tiles.Tile
import com.rafambn.kmap.utils.CanvasDrawReference
import com.rafambn.kmap.utils.ScreenOffset
import com.rafambn.kmap.utils.style.OptimizedStyle
import com.rafambn.kmap.utils.style.OptimizedStyleLayer
import com.rafambn.kmap.utils.toIntFloor
import com.rafambn.kmap.utils.vectorTile.OptimizedGeometry
import com.rafambn.kmap.utils.vectorTile.OptimizedRenderFeature
import kotlin.math.pow

@Composable
fun VectorTileCanvas(
    canvasSize: ScreenOffset,
    gestureWrapper: MapGestureWrapper?,
    magnifierScale: () -> Float,
    positionOffset: () -> CanvasDrawReference,
    tileSize: () -> TileDimension,
    rotationDegrees: () -> Float,
    translation: () -> Offset,
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
                val translation = translation()
                val rotation = rotationDegrees()
                val magnifierScale = magnifierScale()
                val tileSize = tileSize()
                val positionOffset = positionOffset()
                val activeTiles = activeTiles()
                val style = style()
                val zoom = zoom()

                withTransform({
                    translate(translation.x, translation.y)
                    rotate(rotation, Offset.Zero)
                    scale(2F.pow(magnifierScale), Offset.Zero)
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
                            rotation
                        )
                    }
                }
            }
    ) { _, _ ->
        layout(canvasSize.xInt, canvasSize.yInt) {}
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
                rotationDegrees
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
) {
    val sizeX = (scaleAdjustment * tileSize.width.toPx())
    val sizeY = (scaleAdjustment * tileSize.height.toPx())
    val posOffsetX = positionOffset.x.toIntFloor()
    val posOffsetY = positionOffset.y.toIntFloor()
    canvas.withSave {
        tile.optimizedTile?.let { optimizedData ->
            val tileLeft = tile.col * sizeX + posOffsetX
            val tileTop = tile.row * sizeY + posOffsetY
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
                    scaleAdjustment,
                    fontResolver,
                    density,
                    optimizedLayer,
                    zoom,
                    optimizedData.extent.toFloat() / tileSize.height.toPx(),
                    rotationDegrees,
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
        backgroundLayer.paint.properties["background-opacity"]?.evaluate(zoom, emptyMap(), "") as? Float ?: 1F

    val paint = Paint().apply {
        color = backgroundColor.copy(alpha = backgroundOpacity)
        style = PaintingStyle.Fill
        isAntiAlias = false
    }

    activeTiles.tiles.forEach { tile ->
        canvas.withSave {
            val scaleAdjustment = 2F.pow(activeTiles.currentZoom - tile.zoom)
            val tileLeft = tileSize.width.toPx() * tile.col * scaleAdjustment + positionOffset.x
            val tileTop = tileSize.height.toPx() * tile.row * scaleAdjustment + positionOffset.y
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
    scaleAdjustment: Float,
    fontResolver: FontFamily.Resolver,
    density: Density,
    optimizedStyleLayer: OptimizedStyleLayer,
    zoom: Double,
    textScale: Float,
    rotationDegrees: Float,
) {
    when (renderFeature.geometry) {
        is OptimizedGeometry.Polygon -> {
            drawFillFeature(canvas, renderFeature.geometry, renderFeature.properties, optimizedStyleLayer, zoom)
        }

        is OptimizedGeometry.LineString -> {
            drawLineFeature(canvas, renderFeature.geometry, renderFeature.properties, scaleAdjustment, optimizedStyleLayer, zoom)
        }

        is OptimizedGeometry.Point -> {
            drawSymbolFeature(
                canvas,
                renderFeature.geometry,
                renderFeature.properties,
                fontResolver,
                density,
                optimizedStyleLayer,
                zoom,
                textScale,
                rotationDegrees,
            )
        }
    }
}

private fun DrawScope.drawFillFeature(
    canvas: Canvas,
    geometry: OptimizedGeometry.Polygon,
    properties: Map<String, Any>,
    optimizedStyleLayer: OptimizedStyleLayer,
    zoom: Double
) {
    geometry.paths.forEach { path ->
        val fillColor =
            optimizedStyleLayer.paint.properties["fill-color"]?.evaluate(zoom, properties, optimizedStyleLayer.id) as? Color ?: Color.Magenta
        val opacity =
            optimizedStyleLayer.paint.properties["fill-opacity"]?.evaluate(zoom, properties, optimizedStyleLayer.id) as? Double ?: 1.0
        val outlineColor =
            optimizedStyleLayer.paint.properties["fill-outline-color"]?.evaluate(zoom, properties, optimizedStyleLayer.id) as? Color
        canvas.drawPath(
            path,
            Paint().apply {
                color = fillColor.copy(alpha = opacity.toFloat())
                isAntiAlias = true
                style = PaintingStyle.Fill
            }
        )
        outlineColor?.let {
            canvas.drawPath(
                path,
                Paint().apply {
                    color = it
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
    properties: Map<String, Any>,
    scaleAdjustment: Float,
    optimizedStyleLayer: OptimizedStyleLayer,
    zoom: Double
) {
    val fillColor =
        optimizedStyleLayer.paint.properties["line-color"]?.evaluate(zoom, properties, optimizedStyleLayer.id) as? Color ?: Color.Magenta
    val width = optimizedStyleLayer.paint.properties["line-width"]?.evaluate(zoom, properties, optimizedStyleLayer.id) as? Double ?: 1.0f
    val opacity = optimizedStyleLayer.paint.properties["line-opacity"]?.evaluate(zoom, properties, optimizedStyleLayer.id) as? Double ?: 1.0f
    val cap = optimizedStyleLayer.layout.properties["line-cap"]?.evaluate(zoom, properties, optimizedStyleLayer.id) as? String ?: "butt"
    val join = optimizedStyleLayer.layout.properties["line-join"]?.evaluate(zoom, properties, optimizedStyleLayer.id) as? String ?: "miter"

    canvas.drawPath(
        geometry.path,
        Paint().apply {
            color = fillColor.copy(alpha = opacity.toFloat())
            isAntiAlias = true
            style = PaintingStyle.Stroke
            strokeWidth = width.toFloat() * scaleAdjustment
            strokeCap = when (cap) {
                "round" -> StrokeCap.Round
                "square" -> StrokeCap.Square
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
