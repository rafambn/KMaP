package com.rafambn.kmap.mapSource.tiled.vector

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import com.rafambn.kmap.gestures.MapGestureWrapper
import com.rafambn.kmap.gestures.detectMapGestures
import com.rafambn.kmap.gestures.sharedPointerInput
import com.rafambn.kmap.mapSource.tiled.OptimizedVectorTile
import com.rafambn.kmap.mapSource.tiled.TileDimension
import com.rafambn.kmap.mapSource.tiled.ActiveTiles
import com.rafambn.kmap.mapSource.tiled.Tile
import com.rafambn.kmap.utils.CanvasDrawReference
import com.rafambn.kmap.utils.ScreenOffset
import com.rafambn.kmap.utils.asScreenOffset
import com.rafambn.kmap.utils.style.Style
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import com.rafambn.kmap.utils.style.OptimizedStyle
import com.rafambn.kmap.utils.style.OptimizedStyleLayer
import com.rafambn.kmap.utils.style.StyleLayer
import com.rafambn.kmap.utils.vectorTile.OptimizedGeometry
import com.rafambn.kmap.utils.vectorTile.OptimizedRenderFeature
import kotlin.math.pow

@Composable
internal fun VectorTileCanvas(
    canvasSize: ScreenOffset,
    gestureWrapper: MapGestureWrapper?,
    magnifierScale: () -> Float,
    positionOffset: () -> CanvasDrawReference,
    tileSize: () -> TileDimension,
    rotationDegrees: () -> Float,
    translation: () -> Offset,
    activeTiles: () -> ActiveTiles,
    style: () -> OptimizedStyle,
) {
    val fontResolver = LocalFontFamilyResolver.current
    val density = LocalDensity.current
    Layout(
        modifier = Modifier
            .then(gestureWrapper?.let {
                Modifier.sharedPointerInput {
                    detectMapGestures(
                        onTap = gestureWrapper.onTap,
                        onDoubleTap = gestureWrapper.onDoubleTap,
                        onLongPress = gestureWrapper.onLongPress,
                        onTapLongPress = gestureWrapper.onTapLongPress,
                        onTapSwipe = gestureWrapper.onTapSwipe,
                        onGesture = gestureWrapper.onGesture,
                        onTwoFingersTap = gestureWrapper.onTwoFingersTap,
                        onHover = gestureWrapper.onHover,
                    )
                }
            } ?: Modifier)
            .then(gestureWrapper?.onScroll?.let {
                Modifier.pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val pointerEvent = awaitPointerEvent()
                            if (pointerEvent.type == PointerEventType.Scroll) {
                                pointerEvent.changes.forEach {
                                    if (it.scrollDelta.y != 0F)
                                        gestureWrapper.onScroll.invoke(it.position.asScreenOffset(), it.scrollDelta.y)
                                }
                            }
                        }
                    }
                }
            } ?: Modifier)
            .drawBehind {
                val translation = translation()
                val rotation = rotationDegrees()
                val magnifierScale = magnifierScale()
                val tileSize = tileSize()
                val positionOffset = positionOffset()
                val activeTiles = activeTiles()
                val style = style()

                withTransform({
                    translate(translation.x, translation.y)
                    rotate(rotation, Offset.Zero)
                    scale(2F.pow(magnifierScale), Offset.Zero)
                }) {
                    drawIntoCanvas { canvas ->
                        val backgroundLayer = style.layers.find { it.type == "background" }
                        backgroundLayer?.let {
                            drawGlobalBackground(
                                it,
                                canvas,
                                tileSize,
                                positionOffset,
                                activeTiles.currentZoom,
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
                            density
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
    currentZoom: Int,
    style: OptimizedStyle,
    tileSize: TileDimension,
    positionOffset: CanvasDrawReference,
    canvas: Canvas,
    fontResolver: FontFamily.Resolver,
    density: Density
) {
    style.layers.filter { it.type != "background" }.forEach { styleLayer ->
        tiles.forEach { tile ->
            drawVectorTileLayerWithClipping(
                tile as OptimizedVectorTile,
                styleLayer,
                tileSize,
                positionOffset,
                2F.pow(currentZoom - tile.zoom),
                canvas,
                fontResolver,
                density
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
    density: Density
) {
    tile.optimizedTile?.let { optimizedData ->
        val tileOffsetX = tileSize.width * tile.col * scaleAdjustment + positionOffset.x
        val tileOffsetY = tileSize.height * tile.row * scaleAdjustment + positionOffset.y
        val tileOffsetPx = Offset(tileOffsetX.toFloat(), tileOffsetY.toFloat())

        canvas.save()
        canvas.translate(tileOffsetPx.x, tileOffsetPx.y)

        val tileRect = Rect(
            0f,
            0f,
            tileSize.width * scaleAdjustment,
            tileSize.height * scaleAdjustment
        )
        canvas.clipRect(tileRect)

        val scaleX = (tileSize.width * scaleAdjustment) / optimizedData.extent
        val scaleY = (tileSize.height * scaleAdjustment) / optimizedData.extent
        canvas.scale(scaleX, scaleY)

        optimizedData.layerFeatures[optimizedLayer.id]?.forEach { renderFeature ->
            drawRenderFeature(canvas, renderFeature, scaleAdjustment, fontResolver, density, optimizedLayer, tile.zoom.toDouble())
        }

        canvas.restore()
    }
}

private fun DrawScope.drawGlobalBackground(
    backgroundLayer: OptimizedStyleLayer,
    canvas: Canvas,
    tileSize: TileDimension,
    positionOffset: CanvasDrawReference,
    zoomLevel: Int,
) {
    val backgroundColor =
        backgroundLayer.paint.properties["background-color"]?.evaluate(zoomLevel.toDouble(), emptyMap(), "") as? Color ?: Color.Magenta
    val backgroundOpacity =
        backgroundLayer.paint.properties["background-opacity"]?.evaluate(zoomLevel.toDouble(), emptyMap(), "") as? Float ?: 1F

    val totalWidth = tileSize.width * (2F.pow(zoomLevel))
    val totalHeight = tileSize.height * (2F.pow(zoomLevel))

    canvas.drawRect(
        Rect(
            positionOffset.x.toFloat(),
            positionOffset.y.toFloat(),
            positionOffset.x.toFloat() + totalWidth,
            positionOffset.y.toFloat() + totalHeight
        ),
        Paint().apply {
            color = backgroundColor.copy(alpha = backgroundOpacity)
            style = PaintingStyle.Fill
        }
    )
}

internal fun DrawScope.drawRenderFeature(
    canvas: Canvas,
    renderFeature: OptimizedRenderFeature,
    scaleAdjustment: Float,
    fontResolver: FontFamily.Resolver,
    density: Density,
    optimizedStyleLayer: OptimizedStyleLayer,
    zoomLevel: Double
) {
    when (renderFeature.geometry) {
        is OptimizedGeometry.Polygon -> {
            drawFillFeature(canvas, renderFeature.geometry, renderFeature.properties, optimizedStyleLayer, zoomLevel)
        }

        is OptimizedGeometry.LineString -> {
            drawLineFeature(canvas, renderFeature.geometry, renderFeature.properties, scaleAdjustment, optimizedStyleLayer, zoomLevel)
        }

        is OptimizedGeometry.Point -> {
            drawSymbolFeature(canvas, renderFeature.geometry, renderFeature.properties, fontResolver, density, optimizedStyleLayer, zoomLevel)
        }
    }
}

private fun DrawScope.drawFillFeature(
    canvas: Canvas,
    geometry: OptimizedGeometry.Polygon,
    properties: Map<String, Any>,
    optimizedStyleLayer: OptimizedStyleLayer,
    zoomLevel: Double
) {
    geometry.paths.forEach { path ->
        val fillColor =
            optimizedStyleLayer.paint.properties["fill-color"]?.evaluate(zoomLevel, properties, optimizedStyleLayer.id) as? Color
                ?: Color.Magenta
        val opacity =
            optimizedStyleLayer.paint.properties["fill-opacity"]?.evaluate(zoomLevel, properties, optimizedStyleLayer.id) as? Float
                ?: 1.0f
        val outlineColor =
            optimizedStyleLayer.paint.properties["fill-outline-color"]?.evaluate(
                zoomLevel,
                properties,
                optimizedStyleLayer.id
            ) as? Color
        canvas.drawPath(
            path,
            Paint().apply {
                color = fillColor.copy(alpha = opacity)
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
    zoomLevel: Double
) {
    val fillColor = optimizedStyleLayer.paint.properties["line-color"]?.evaluate(zoomLevel, properties, optimizedStyleLayer.id) as? Color ?: Color.Magenta
    val width = optimizedStyleLayer.paint.properties["line-width"]?.evaluate(zoomLevel, properties, optimizedStyleLayer.id) as? Float ?: 1.0f
    val opacity = optimizedStyleLayer.paint.properties["line-opacity"]?.evaluate(zoomLevel, properties, optimizedStyleLayer.id) as? Float ?: 1.0f
    val cap = optimizedStyleLayer.layout.properties["line-cap"]?.evaluate(zoomLevel, properties, optimizedStyleLayer.id) as? String ?: "butt"
    val join = optimizedStyleLayer.layout.properties["line-join"]?.evaluate(zoomLevel, properties, optimizedStyleLayer.id) as? String ?: "miter"

    canvas.drawPath(
        geometry.path,
        Paint().apply {
            color = fillColor.copy(alpha = opacity)
            isAntiAlias = true
            style = PaintingStyle.Stroke
            strokeWidth = width * scaleAdjustment
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
    zoomLevel: Double
) {
    // Text symbol rendering
//    if (properties.field != null) {
//        drawTextSymbol(canvas, geometry, properties, fontResolver, density)
//    }

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
    density: Density
) {
//    val textColor = (properties.color ?: Color.Magenta).copy(alpha = properties.opacity)
//    geometry.coordinates.forEach { (x, y) ->
//        val textLayoutResult =
//            TextMeasurer(
//                defaultFontFamilyResolver = fontResolver,
//                defaultDensity = density,
//                defaultLayoutDirection = LayoutDirection.Ltr,
//            ).measure(
//                text = AnnotatedString(properties.field!!),
//                style = TextStyle(
//                    color = textColor,
//                    fontSize = 150.sp,
//                ),
//                overflow = TextOverflow.Visible,
//                softWrap = true,
//                maxLines = 1,
//                constraints = Constraints(0, this.size.width.toInt(), 0, this.size.height.toInt()),
//                layoutDirection = layoutDirection,
//                density = this,
//            )
//        withTransform({
//            translate(x - textLayoutResult.size.width / 2, y - textLayoutResult.size.height / 2)
//        }) {
//            textLayoutResult.multiParagraph.paint(canvas = drawContext.canvas, blendMode = DrawScope.DefaultBlendMode)
//        }
//    }
}
