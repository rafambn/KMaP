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
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.rafambn.kmap.geometry.plane.CanvasDrawReference
import com.rafambn.kmap.gesture.MapGestureCallbacks
import com.rafambn.kmap.gesture.internal.mapGestures
import com.rafambn.kmap.mapProperties.TileDimension
import com.rafambn.kmap.mvttile.OptimizedGeometry
import com.rafambn.kmap.mvttile.OptimizedRenderFeature
import com.rafambn.kmap.source.Tile
import com.rafambn.kmap.source.internal.ActiveTiles
import com.rafambn.kmap.source.internal.OptimizedVectorTile
import com.rafambn.kmap.style.compiled.CompiledBackgroundLayer
import com.rafambn.kmap.style.compiled.CompiledFillLayer
import com.rafambn.kmap.style.compiled.CompiledLineLayer
import com.rafambn.kmap.style.compiled.CompiledStyle
import com.rafambn.kmap.style.compiled.CompiledStyleLayer
import com.rafambn.kmap.style.compiled.CompiledSymbolLayer
import com.rafambn.kmap.style.SpriteImage
import kotlin.math.ceil
import kotlin.math.pow

private fun sdfIconColorFilter(color: Color, threshold: Float = 0.75f, smoothing: Float = 0.105f): ColorFilter {
    val alphaScale = 1f / (2f * smoothing)
    return ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
        0f, 0f, 0f, 0f, color.red * 255f,
        0f, 0f, 0f, 0f, color.green * 255f,
        0f, 0f, 0f, 0f, color.blue * 255f,
        // A linear approximation of the SDF shader's smoothstep around its distance threshold.
        0f, 0f, 0f, alphaScale, (0.5f - threshold * alphaScale) * 255f
    )))
}

private val defaultSdfIconColorFilter = sdfIconColorFilter(Color.Black)

private fun Canvas.drawSpriteImage(image: ImageBitmap, offset: IntOffset, size: IntSize, paint: Paint, alpha: Float) {
    if (alpha < 1f) {
        // Decode the SDF before applying opacity, which otherwise changes its distance threshold.
        withSaveLayer(Rect(offset.x.toFloat(), offset.y.toFloat(),
            (offset.x + size.width).toFloat(), (offset.y + size.height).toFloat()),
            Paint().apply { this.alpha = alpha }
        ) { drawImageRect(image = image, dstOffset = offset, dstSize = size, paint = paint) }
    } else {
        drawImageRect(image = image, dstOffset = offset, dstSize = size, paint = paint)
    }
}

@Composable
fun VectorTileCanvas(
    gestureCallbacks: MapGestureCallbacks?,
    magnifierScale: () -> Float,
    positionOffset: () -> CanvasDrawReference,
    tileSize: () -> TileDimension,
    rotationDegrees: () -> Float,
    activeTiles: () -> ActiveTiles,
    style: () -> CompiledStyle,
    zoom: () -> Double,
) {
    val fontResolver = LocalFontFamilyResolver.current
    val density = LocalDensity.current
    Layout(
        modifier = Modifier
            .mapGestures(gestureCallbacks)
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
                        val backgroundLayer = style.layers.filterIsInstance<CompiledBackgroundLayer>().firstOrNull()
                        if (backgroundLayer != null &&
                            zoom in backgroundLayer.minZoom..<backgroundLayer.maxZoom &&
                            backgroundLayer.visibility.evaluate(zoom.toInt().toDouble(), emptyMap(), null) == true
                        ) {
                            drawBackgroundForActiveTiles(
                                backgroundLayer,
                                canvas,
                                tileSize,
                                positionOffset,
                                activeTiles,
                                zoom,
                                screenScale
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
    style: CompiledStyle,
    tileSize: TileDimension,
    positionOffset: CanvasDrawReference,
    canvas: Canvas,
    fontResolver: FontFamily.Resolver,
    density: Density,
    zoom: Double,
    rotationDegrees: Float,
    screenScale: Float,
) {
    style.layers.filter {
        it !is CompiledBackgroundLayer &&
            zoom in it.minZoom..<it.maxZoom &&
            it.visibility.evaluate(zoom.toInt().toDouble(), emptyMap(), null) == true
    }.forEach { styleLayer ->
        tiles.forEach { tile ->
            drawVectorTileLayerWithClipping(
                tile as OptimizedVectorTile,
                styleLayer,
                style.glyphs,
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

internal fun DrawScope.drawVectorTileLayerWithClipping(
    tile: OptimizedVectorTile,
    optimizedLayer: CompiledStyleLayer,
    glyphs: Map<String, FontFamily>,
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
                    optimizedData.extent.toFloat() / sizeY,
                    rotationDegrees,
                    scaleX,
                    scaleY,
                    screenScale,
                    glyphs,
                    tile.col.toDouble() * sizeX,
                    tile.row.toDouble() * sizeY
                )
            }
        }
    }
}

internal fun DrawScope.drawBackgroundForActiveTiles(
    backgroundLayer: CompiledBackgroundLayer,
    canvas: Canvas,
    tileSize: TileDimension,
    positionOffset: CanvasDrawReference,
    activeTiles: ActiveTiles,
    zoom: Double,
    screenScale: Float,
) {
    val pattern = backgroundLayer.pattern?.evaluate(zoom.toInt().toDouble(), emptyMap(), null)
    if (backgroundLayer.pattern != null && pattern == null) return
    val backgroundColor = backgroundLayer.color?.evaluate(zoom, emptyMap(), null) ?: Color.Magenta
    val backgroundOpacity = backgroundLayer.opacity?.evaluate(zoom, emptyMap(), null)?.toFloat() ?: 1F

    val paint = Paint().apply {
        if (pattern == null) color = backgroundColor.copy(alpha = backgroundColor.alpha * backgroundOpacity)
        else {
            shader = pattern.repeatingShader
            alpha = backgroundOpacity.coerceIn(0f, 1f)
        }
        style = PaintingStyle.Fill
        isAntiAlias = false
    }

    activeTiles.tiles.forEach { tile ->
        canvas.withSave {
            val scaleAdjustment = 2F.pow(activeTiles.currentZoom - tile.zoom)
            val tileWidth = tileSize.width.toPx() * scaleAdjustment
            val tileHeight = tileSize.height.toPx() * scaleAdjustment
            val worldLeft = tile.col.toDouble() * tileWidth
            val worldTop = tile.row.toDouble() * tileHeight
            val tileLeft = worldLeft + positionOffset.x
            val tileTop = worldTop + positionOffset.y
            val tileRight = tileLeft + tileWidth
            val tileBottom = tileTop + tileHeight

            if (pattern == null) {
                canvas.drawRect(Rect(tileLeft.toFloat(), tileTop.toFloat(), tileRight.toFloat(), tileBottom.toFloat()), paint)
            } else {
                val patternScale = (1.0 / pattern.pixelRatio / screenScale).toFloat()
                val phaseX = worldLeft.positiveRemainder(pattern.bitmap.width * patternScale).toFloat() / patternScale
                val phaseY = worldTop.positiveRemainder(pattern.bitmap.height * patternScale).toFloat() / patternScale
                canvas.translate(tileLeft.toFloat(), tileTop.toFloat())
                canvas.scale(patternScale, patternScale)
                canvas.translate(-phaseX, -phaseY)
                canvas.drawRect(Rect(phaseX, phaseY, phaseX + tileWidth / patternScale,
                    phaseY + tileHeight / patternScale), paint)
            }
        }
    }
}

internal fun DrawScope.drawRenderFeature(
    canvas: Canvas,
    renderFeature: OptimizedRenderFeature,
    fontResolver: FontFamily.Resolver,
    density: Density,
    compiledStyleLayer: CompiledStyleLayer,
    zoom: Double,
    textScale: Float,
    rotationDegrees: Float,
    tileScaleX: Float,
    tileScaleY: Float,
    screenScale: Float,
    glyphs: Map<String, FontFamily> = emptyMap(),
    tileWorldX: Double = 0.0,
    tileWorldY: Double = 0.0,
) {
    val geometry = renderFeature.geometry
    when (compiledStyleLayer) {
        is CompiledFillLayer -> if (geometry is OptimizedGeometry.Polygon) {
            drawFillFeature(canvas, geometry, renderFeature.properties, compiledStyleLayer, zoom, tileScaleX, tileScaleY, screenScale, renderFeature.id, tileWorldX, tileWorldY)
        }

        is CompiledLineLayer -> {
            val path = when (geometry) {
                is OptimizedGeometry.LineString -> geometry.path
                is OptimizedGeometry.Polygon -> geometry.path
                is OptimizedGeometry.Point -> null
            }
            if (path != null) {
                drawLineFeature(
                    canvas, path, renderFeature.properties, compiledStyleLayer, zoom, tileScaleX, tileScaleY, screenScale,
                    renderFeature.id, if (geometry is OptimizedGeometry.Polygon) "Polygon" else "LineString", density.density
                )
            }
        }

        is CompiledSymbolLayer -> if (geometry is OptimizedGeometry.Point) {
            drawSymbolFeature(canvas, geometry, renderFeature.properties, fontResolver, density, compiledStyleLayer, glyphs, zoom, textScale, rotationDegrees, screenScale, renderFeature.id)
        }
        is CompiledBackgroundLayer -> Unit
    }
}

internal fun drawFillFeature(
    canvas: Canvas,
    geometry: OptimizedGeometry.Polygon,
    properties: Map<String, Any>,
    compiledStyleLayer: CompiledFillLayer,
    zoom: Double,
    tileScaleX: Float,
    tileScaleY: Float,
    screenScale: Float,
    featureId: ULong? = null,
    tileWorldX: Double = 0.0,
    tileWorldY: Double = 0.0,
) {
    val pattern = compiledStyleLayer.pattern?.evaluate(zoom.toInt().toDouble(), properties, featureId, "Polygon")
    if (compiledStyleLayer.pattern != null) {
        if (pattern != null) canvas.drawSpritePattern(geometry.path, pattern, tileScaleX, tileScaleY,
            screenScale, tileWorldX, tileWorldY,
            compiledStyleLayer.opacity?.evaluate(zoom, properties, featureId, "Polygon")?.toFloat() ?: 1f,
            compiledStyleLayer.antialias?.evaluate(zoom, properties, featureId, "Polygon") ?: true)
        return
    }
    val fillColor = compiledStyleLayer.color?.evaluate(zoom, properties, featureId, "Polygon") ?: Color.Magenta
    val opacity = compiledStyleLayer.opacity?.evaluate(zoom, properties, featureId, "Polygon") ?: 1.0
    val outlineColor = compiledStyleLayer.outlineColor?.evaluate(zoom, properties, featureId, "Polygon")
    val antialias = compiledStyleLayer.antialias?.evaluate(zoom, properties, featureId, "Polygon") ?: true
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

private fun Canvas.drawSpritePattern(
    path: Path,
    image: SpriteImage,
    tileScaleX: Float,
    tileScaleY: Float,
    screenScale: Float,
    tileWorldX: Double,
    tileWorldY: Double,
    opacity: Float,
    antialias: Boolean
) {
    val patternScale = (1.0 / image.pixelRatio / screenScale).toFloat()
    val phaseX = tileWorldX.positiveRemainder(image.bitmap.width * patternScale).toFloat()
    val phaseY = tileWorldY.positiveRemainder(image.bitmap.height * patternScale).toFloat()
    val scaledPath = Path().apply {
        fillType = path.fillType
        addPath(path)
        transform(Matrix().apply { scale(tileScaleX / patternScale, tileScaleY / patternScale) })
    }
    val worldPath = Path().apply {
        fillType = path.fillType
        addPath(scaledPath, Offset(phaseX / patternScale, phaseY / patternScale))
    }
    withSave {
        scale(1f / tileScaleX, 1f / tileScaleY)
        translate(-phaseX, -phaseY)
        scale(patternScale, patternScale)
        drawPath(worldPath, Paint().apply {
            shader = image.repeatingShader
            alpha = opacity.coerceIn(0f, 1f)
            style = PaintingStyle.Fill
            isAntiAlias = antialias
        })
    }
}

private fun Double.positiveRemainder(period: Float): Double {
    val value = period.toDouble()
    return (this % value + value) % value
}

internal fun drawLineFeature(
    canvas: Canvas,
    path: Path,
    properties: Map<String, Any>,
    compiledStyleLayer: CompiledLineLayer,
    zoom: Double,
    tileScaleX: Float,
    tileScaleY: Float,
    screenScale: Float,
    featureId: ULong? = null,
    geometryType: String = "LineString",
    displayDensity: Float = 1f,
) {
    val fillColor = compiledStyleLayer.color?.evaluate(zoom, properties, featureId, geometryType) ?: Color.Magenta
    val width = (compiledStyleLayer.width?.evaluate(zoom, properties, featureId, geometryType)?.toFloat() ?: 1f) * displayDensity
    if (width <= 0f) return
    val opacity = compiledStyleLayer.opacity?.evaluate(zoom, properties, featureId, geometryType)?.toFloat() ?: 1f
    val cap = compiledStyleLayer.cap?.evaluate(zoom, properties, featureId, geometryType) ?: "butt"
    val join = compiledStyleLayer.join?.evaluate(zoom, properties, featureId, geometryType) ?: "miter"
    val effectiveCap = if (join == "none") "butt" else cap
    val dashArray = if (compiledStyleLayer.patternPresent?.evaluate(zoom, properties, featureId, geometryType) != true) {
        compiledStyleLayer.dashArray?.evaluate(zoom.toInt().toDouble(), properties, featureId, geometryType)
    } else null
    val validDashArray = dashArray?.takeIf { values ->
        values.isNotEmpty() && values.all { it.toFloat().isFinite() && it.toFloat() >= 0f }
    }
    if (validDashArray?.all { it.toFloat() == 0f } == true) return
    var dashEffect: PathEffect? = null
    if (validDashArray != null && validDashArray.size > 1) {
        val values = validDashArray
        val odd = values.size % 2 != 0
        val intervalCount = if (odd) values.size - 1 else values.size

        // Mapbox joins the first and last dashes of an odd-length array at the repeat boundary.
        var intervals = FloatArray(intervalCount) { index ->
            val units = values[index].toFloat() +
                if (odd && index == 0) values.last().toFloat() else 0f
            units * width / screenScale
        }
        var phase = if (odd) values.last().toFloat() * width / screenScale else 0f
        if (intervals.all { it.isFinite() } && phase.isFinite()) {
            if (effectiveCap != "round") {
                val collapsed = mutableListOf<Float>()
                var leadingGap = 0f
                for (index in intervals.indices step 2) {
                    val dash = intervals[index]
                    val gap = intervals[index + 1]
                    when {
                        dash == 0f && collapsed.isEmpty() -> leadingGap += gap
                        dash == 0f -> collapsed[collapsed.lastIndex] += gap
                        collapsed.isEmpty() -> { collapsed.add(dash); collapsed.add(gap) }
                        collapsed.last() == 0f -> {
                            collapsed[collapsed.lastIndex - 1] += dash
                            collapsed[collapsed.lastIndex] = gap
                        }
                        else -> { collapsed.add(dash); collapsed.add(gap) }
                    }
                }
                if (collapsed.isEmpty()) return
                collapsed[collapsed.lastIndex] += leadingGap
                phase -= leadingGap
                if (collapsed.size > 2 && collapsed.last() == 0f) {
                    val lastDash = collapsed[collapsed.lastIndex - 1]
                    collapsed[0] += lastDash
                    phase += lastDash
                    collapsed.removeAt(collapsed.lastIndex)
                    collapsed.removeAt(collapsed.lastIndex)
                }
                intervals = collapsed.toFloatArray()
            }
            if (intervals.indices.any { it % 2 != 0 && intervals[it] > 0f }) {
                val period = intervals.sum()
                dashEffect = PathEffect.dashPathEffect(intervals, (phase % period + period) % period)
            }
        }
    }

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
                pathEffect = dashEffect
                strokeCap = when {
                    effectiveCap == "round" -> StrokeCap.Round
                    effectiveCap == "square" -> StrokeCap.Square
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
    compiledStyleLayer: CompiledSymbolLayer,
    glyphs: Map<String, FontFamily>,
    zoom: Double,
    textScale: Float,
    rotationDegrees: Float,
    screenScale: Float,
    featureId: ULong?,
) {
    drawIconSymbol(canvas, geometry, properties, density.density, compiledStyleLayer, zoom, textScale, rotationDegrees, screenScale, featureId)
    val text = compiledStyleLayer.textField?.evaluate(zoom.toInt().toDouble(), properties, featureId)
    text?.let {
        drawTextSymbol(canvas, geometry, properties, fontResolver, density, compiledStyleLayer, glyphs, zoom, it, textScale,
            rotationDegrees, screenScale, featureId)
    }
}

private fun drawIconSymbol(
    canvas: Canvas,
    geometry: OptimizedGeometry.Point,
    properties: Map<String, Any>,
    displayDensity: Float,
    layer: CompiledSymbolLayer,
    zoom: Double,
    textScale: Float,
    rotationDegrees: Float,
    screenScale: Float,
    featureId: ULong?,
) {
    val image = layer.iconImage?.evaluate(zoom.toInt().toDouble(), properties, featureId) ?: return
    val size = layer.iconSize?.evaluate(zoom.toInt().toDouble(), properties, featureId)?.toFloat() ?: 1f
    if (size <= 0f) return
    val opacity = layer.iconOpacity?.evaluate(zoom, properties, featureId)?.toFloat() ?: 1f
    if (opacity <= 0f) return
    val rotate = layer.iconRotate?.evaluate(zoom.toInt().toDouble(), properties, featureId)?.toFloat() ?: 0f
    val anchor = layer.iconAnchor?.evaluate(zoom.toInt().toDouble(), properties, featureId) ?: "center"
    val offset = layer.iconOffset?.evaluate(zoom.toInt().toDouble(), properties, featureId)
    val iconColor = if (image.sdf) layer.iconColor?.evaluate(zoom, properties, featureId) ?: Color.Black else null
    val iconAlpha = (opacity * (iconColor?.alpha ?: 1f)).coerceIn(0f, 1f)
    val haloColor = if (image.sdf) {
        layer.iconHaloColor?.evaluate(zoom, properties, featureId) ?: Color.Transparent
    } else Color.Transparent
    val haloAlpha = (opacity * haloColor.alpha).coerceIn(0f, 1f)
    val haloWidth = if (haloAlpha > 0f) {
        layer.iconHaloWidth?.evaluate(zoom, properties, featureId)?.toFloat()?.coerceAtLeast(0f) ?: 0f
    } else 0f
    val hasHalo = haloAlpha > 0f && haloWidth > 0f
    if (iconAlpha <= 0f && !hasHalo) return
    val haloBlur = if (hasHalo) {
        layer.iconHaloBlur?.evaluate(zoom, properties, featureId)?.toFloat()?.coerceAtLeast(0f) ?: 0f
    } else 0f
    val scale = size * displayDensity * textScale / screenScale
    val width = (image.bitmap.width / image.pixelRatio * scale).toFloat()
    val height = (image.bitmap.height / image.pixelRatio * scale).toFloat()
    val left = when {
        anchor.contains("left") -> 0f
        anchor.contains("right") -> -width
        else -> -width / 2f
    } + (offset?.getOrNull(0)?.toFloat() ?: 0f) * scale
    val top = when {
        anchor.contains("top") -> 0f
        anchor.contains("bottom") -> -height
        else -> -height / 2f
    } + (offset?.getOrNull(1)?.toFloat() ?: 0f) * scale
    val iconPaint = Paint().apply {
        alpha = if (image.sdf) 1f else iconAlpha
        filterQuality = FilterQuality.High
        if (iconColor != null) colorFilter = if (iconColor == Color.Black && size == 1f) {
            defaultSdfIconColorFilter
        } else sdfIconColorFilter(iconColor, smoothing = 0.105f / size)
    }
    val haloPaint = if (hasHalo) Paint().apply {
        filterQuality = FilterQuality.High
        // SDF distance uses an eight-pixel range; scale the halo thresholds with icon-size.
        colorFilter = sdfIconColorFilter(haloColor,
            threshold = (6f - haloWidth / size) / 8f,
            smoothing = (0.105f + haloBlur * 1.19f / 8f) / size)
    } else null
    val dstOffset = IntOffset(left.toInt(), top.toInt())
    val dstSize = IntSize(width.toInt().coerceAtLeast(1), height.toInt().coerceAtLeast(1))
    geometry.coordinates.forEach { (x, y) ->
        canvas.withSave {
            canvas.translate(x, y)
            canvas.rotate(-rotationDegrees + rotate)
            if (haloPaint != null) canvas.drawSpriteImage(image.bitmap, dstOffset, dstSize, haloPaint, haloAlpha)
            if (iconAlpha > 0f) canvas.drawSpriteImage(image.bitmap, dstOffset, dstSize, iconPaint,
                if (image.sdf) iconAlpha else 1f)
        }
    }
}

private fun DrawScope.drawTextSymbol(
    canvas: Canvas,
    geometry: OptimizedGeometry.Point,
    properties: Map<String, Any>,
    fontResolver: FontFamily.Resolver,
    density: Density,
    compiledStyleLayer: CompiledSymbolLayer,
    glyphs: Map<String, FontFamily>,
    zoom: Double,
    text: String,
    textScale: Float,
    rotationDegrees: Float,
    screenScale: Float,
    featureId: ULong?,
) {
    val layoutZoom = zoom.toInt().toDouble()
    val transform = compiledStyleLayer.textTransform?.evaluate(layoutZoom, properties, featureId) ?: "none"
    val size = compiledStyleLayer.textSize?.evaluate(zoom, properties, featureId) ?: 16.0
    val textColor = compiledStyleLayer.textColor?.evaluate(zoom, properties, featureId) ?: Color.Black
    val opacity = compiledStyleLayer.textOpacity?.evaluate(zoom, properties, featureId) ?: 1.0

    val haloColor = compiledStyleLayer.textHaloColor?.evaluate(zoom, properties, featureId)
    val haloWidth = compiledStyleLayer.textHaloWidth?.evaluate(zoom, properties, featureId) ?: 0.0
    val haloBlur = compiledStyleLayer.textHaloBlur?.evaluate(zoom, properties, featureId) ?: 0.0

    val maxWidth = compiledStyleLayer.textMaxWidth?.evaluate(layoutZoom, properties, featureId)
    val lineHeight = compiledStyleLayer.textLineHeight?.evaluate(layoutZoom, properties, featureId)
    val justify = compiledStyleLayer.textJustify?.evaluate(layoutZoom, properties, featureId) ?: "center"

    val anchor = compiledStyleLayer.textAnchor?.evaluate(layoutZoom, properties, featureId) ?: "center"
    val offset = compiledStyleLayer.textOffset?.evaluate(layoutZoom, properties, featureId) ?: listOf(0.0, 0.0)
    val radialOffset = compiledStyleLayer.textRadialOffset?.evaluate(layoutZoom, properties, featureId)
    val translate = compiledStyleLayer.textTranslate?.evaluate(zoom, properties, featureId) ?: listOf(0.0, 0.0)
    val rotate = compiledStyleLayer.textRotate?.evaluate(layoutZoom, properties, featureId)

    val emSize = size.toFloat() * textScale / screenScale
    val finalSize = emSize.sp
    val fontNames = compiledStyleLayer.textFont?.evaluate(layoutZoom, properties, featureId)
    val fontFamily = fontNames?.firstNotNullOfOrNull { glyphs[it] }

    val displayText = when (transform) {
        "uppercase" -> text.uppercase()
        "lowercase" -> text.lowercase()
        else -> text
    }

    val textStyle = TextStyle(
        fontSize = finalSize,
        fontFamily = fontFamily,
        color = textColor.copy(alpha = textColor.alpha * opacity.toFloat()),
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
        maxWidth = maxWidth?.let { maxWidthInEms ->
            with(density) { ceil((maxWidthInEms * emSize).sp.toPx()).toInt() }
        } ?: Constraints.Infinity
    )
    val textLayoutResult = textMeasurer.measure(
        text = displayText,
        style = textStyle,
        overflow = TextOverflow.Visible,
        softWrap = maxWidth != null,
        maxLines = Int.MAX_VALUE,
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

    val offsetX = (offset.getOrNull(0) ?: 0.0).toFloat() * emSize
    val offsetY = (offset.getOrNull(1) ?: 0.0).toFloat() * emSize
    anchorOffsetX += offsetX
    anchorOffsetY += offsetY

    if (radialOffset != null && radialOffset > 0) {
        anchorOffsetY -= (radialOffset * emSize).toFloat()
    }

    val translateX = (translate.getOrNull(0) ?: 0.0).toFloat()
    val translateY = (translate.getOrNull(1) ?: 0.0).toFloat()

    geometry.coordinates.forEach { (x, y) ->
        withTransform({
            translate(
                left = x + anchorOffsetX + translateX,
                top = y + anchorOffsetY + translateY
            )
            rotate(-rotationDegrees + (rotate?.toFloat() ?: 0F), Offset(-anchorOffsetX, -anchorOffsetY))
        }) {
            if (haloColor != null && haloWidth > 0) {
                val fadedHaloColor = haloColor.copy(alpha = haloColor.alpha * opacity.toFloat())
                val haloPixelScale = density.density * textScale / screenScale
                textLayoutResult.multiParagraph.paint(
                    canvas = drawContext.canvas,
                    color = fadedHaloColor,
                    shadow = if (haloBlur > 0) Shadow(
                        color = fadedHaloColor,
                        blurRadius = haloBlur.toFloat() * haloPixelScale
                    ) else null,
                    drawStyle = Stroke(
                        width = haloWidth.toFloat() * 2 * haloPixelScale,
                        join = StrokeJoin.Round,
                        cap = StrokeCap.Round
                    )
                )
            }

            textLayoutResult.multiParagraph.paint(
                canvas = drawContext.canvas,
                color = textStyle.color,
                shadow = Shadow.None,
                drawStyle = Fill,
                blendMode = DrawScope.DefaultBlendMode
            )
        }
    }
}
