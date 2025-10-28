package com.rafambn.kmap.tiles

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Path as ComposePath
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.rafambn.kmap.gestures.MapGestureWrapper
import com.rafambn.kmap.gestures.detectMapGestures
import com.rafambn.kmap.gestures.sharedPointerInput
import com.rafambn.kmap.utils.CanvasDrawReference
import com.rafambn.kmap.utils.ScreenOffset
import com.rafambn.kmap.utils.asScreenOffset
import com.rafambn.kmap.utils.style.Style
import com.rafambn.kmap.utils.style.StyleLayer
import com.rafambn.kmap.utils.toIntFloor
import com.rafambn.kmap.utils.vectorTile.MVTFeature
import com.rafambn.kmap.utils.vectorTile.MVTLayer
import com.rafambn.kmap.utils.vectorTile.RawMVTGeomType
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.pow

@Composable
internal fun TileCanvas(
    id: Int,
    canvasSize: ScreenOffset,
    gestureWrapper: MapGestureWrapper?,
    magnifierScale: () -> Float,
    positionOffset: () -> CanvasDrawReference,
    tileSize: () -> TileDimension,
    rotationDegrees: () -> Float,
    translation: () -> Offset,
    tileLayers: (Int) -> TileLayers,
    style: Style?
) {
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
                        onScroll = null,
                    )
                }
            } ?: Modifier)
            .then(gestureWrapper?.onScroll?.let {//TODO quick fix of scroll, check pointer event order behavior has changed and fix code properly
                Modifier.pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val pointerEvent = awaitPointerEvent()
                            if (pointerEvent.type == PointerEventType.Scroll) {
                                pointerEvent.changes.forEach {
                                    if (it.scrollDelta.y != 0F)
                                        gestureWrapper.onScroll.invoke(it.position.asScreenOffset(), it.scrollDelta.y / 5)
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
                val tileLayers = tileLayers(id)
                withTransform({
                    translate(translation.x, translation.y)
                    rotate(rotation, Offset.Zero)
                    scale(2F.pow(magnifierScale), Offset.Zero)
                }) {
                    drawIntoCanvas { canvas ->
                        drawTiles(
                            tileLayers.backLayer.tiles,
                            tileSize,
                            positionOffset,
                            2F.pow(tileLayers.frontLayer.level - tileLayers.backLayer.level),
                            canvas,
                            style
                        )
                        drawTiles(
                            tileLayers.frontLayer.tiles,
                            tileSize,
                            positionOffset,
                            1F,
                            canvas,
                            style
                        )
                    }
                }
            }
    ) { _, constraints ->
        layout(canvasSize.xInt, canvasSize.yInt) {}
    }
}

private fun DrawScope.drawTiles(
    tiles: List<Tile>,
    tileSize: TileDimension,
    positionOffset: CanvasDrawReference,
    scaleAdjustment: Float = 1F,
    canvas: Canvas,
    style: Style? = null,
) {
    tiles.forEach { tile ->
        when (tile) {
            is RasterTile -> tile.imageBitmap?.let {
                canvas.drawImageRect(
                    image = it,
                    dstOffset = IntOffset(
                        (tileSize.width * tile.row * scaleAdjustment + positionOffset.x).dp.toPx().toIntFloor(),
                        (tileSize.height * tile.col * scaleAdjustment + positionOffset.y).dp.toPx().toIntFloor()
                    ),
                    dstSize = IntSize(
                        (tileSize.width.dp.toPx() * scaleAdjustment).toIntFloor(),
                        (tileSize.height.dp.toPx() * scaleAdjustment).toIntFloor()
                    ),
                    paint = Paint().apply {
                        isAntiAlias = false
                        filterQuality = FilterQuality.High
                    }
                )
            }

            is VectorTile -> {
                style?.let {
                    drawVectorTile(tile, tileSize, positionOffset, scaleAdjustment, canvas, style)
                }
            }
        }
    }
}

private fun DrawScope.drawVectorTile(
    tile: VectorTile,
    tileSize: TileDimension,
    positionOffset: CanvasDrawReference,
    scaleAdjustment: Float = 1F,
    canvas: Canvas,
    style: Style?
) {
    tile.mvtile?.let { mvTile ->
        style?.let { styleSpec ->
            // Calculate tile offset in canvas space
            val tileOffsetX = tileSize.width * tile.row * scaleAdjustment + positionOffset.x
            val tileOffsetY = tileSize.height * tile.col * scaleAdjustment + positionOffset.y
            val tileOffsetPx = Offset(tileOffsetX.dp.toPx(), tileOffsetY.dp.toPx())

            // Get zoom level from tile
            val currentZoom = tile.zoom.toDouble()

            // Process each layer in the style
            styleSpec.layers.forEach { styleLayer ->
                // Skip background layers (they don't have vector tile sources)
                if (styleLayer.type == "background") return@forEach

                // Check if this layer should be visible at current zoom
                if (styleLayer.minzoom != null && currentZoom < styleLayer.minzoom) return@forEach
                if (styleLayer.maxzoom != null && currentZoom >= styleLayer.maxzoom) return@forEach

                // Find the corresponding MVT layer
                val sourceLayer = styleLayer.sourceLayer ?: styleLayer.id
                val mvtLayer = mvTile.layers.find { it.name == sourceLayer } ?: return@forEach

                // Render features in this layer based on style layer type
                when (styleLayer.type) {
                    "fill" -> renderFillLayer(
                        mvtLayer, styleLayer, tileOffsetPx, tileSize, scaleAdjustment, canvas
                    )

                    "line" -> renderLineLayer(
                        mvtLayer, styleLayer, tileOffsetPx, tileSize, scaleAdjustment, canvas
                    )

                    "symbol" -> renderSymbolLayer(
                        mvtLayer, styleLayer, tileOffsetPx, tileSize, scaleAdjustment, canvas
                    )

                    "circle" -> renderCircleLayer(
                        mvtLayer, styleLayer, tileOffsetPx, tileSize, scaleAdjustment, canvas
                    )
                    // Other layer types can be added as needed
                }
            }
        }
    }
}

/**
 * Renders a fill layer (closed polygons with fill color and optional stroke)
 */
private fun DrawScope.renderFillLayer(
    layer: MVTLayer,
    styleLayer: StyleLayer,
    tileOffset: Offset,
    tileSize: TileDimension,
    scaleAdjustment: Float,
    canvas: Canvas
) {
    val fillColor = extractColorProperty(styleLayer.paint, "fill-color", Color(0xFF0000))
    val fillOpacity = extractOpacityProperty(styleLayer.paint, "fill-opacity", 1.0)
    val strokeColor = extractColorProperty(styleLayer.paint, "fill-outline-color", Color.Black)

    layer.features.forEach { feature ->
        // Only render polygon features for fill layers
        if (feature.type != RawMVTGeomType.POLYGON) return@forEach

        // Check if feature matches the layer filter
        if (!matchesFilter(feature, styleLayer.filter)) return@forEach

        // Build path from geometry
        val path = buildPathFromGeometry(feature.geometry, layer.extent, tileSize, scaleAdjustment, tileOffset)

        // Draw filled polygon
        canvas.drawPath(
            path,
            Paint().apply {
                color = fillColor.copy(alpha = fillOpacity.toFloat())
                isAntiAlias = true
                style = PaintingStyle.Fill
            }
        )

        // Draw outline if specified
        if (styleLayer.paint?.containsKey("fill-outline-color") == true) {
            canvas.drawPath(
                path,
                Paint().apply {
                    color = strokeColor
                    isAntiAlias = true
                    style = PaintingStyle.Stroke
                    strokeWidth = 1f
                }
            )
        }
    }
}

/**
 * Renders a line layer (LineString features with configurable stroke)
 */
private fun DrawScope.renderLineLayer(
    layer: MVTLayer,
    styleLayer: StyleLayer,
    tileOffset: Offset,
    tileSize: TileDimension,
    scaleAdjustment: Float,
    canvas: Canvas
) {
    val lineColor = extractColorProperty(styleLayer.paint, "line-color", Color.Black)
    val lineWidth = extractNumberProperty(styleLayer.paint, "line-width", 1.0).toFloat()
    val lineOpacity = extractOpacityProperty(styleLayer.paint, "line-opacity", 1.0)
    val lineCap = extractStringProperty(styleLayer.layout, "line-cap", "butt")
    val lineJoin = extractStringProperty(styleLayer.layout, "line-join", "miter")

    layer.features.forEach { feature ->
        // Only render line features for line layers
        if (feature.type != RawMVTGeomType.LINESTRING) return@forEach

        // Check if feature matches the layer filter
        if (!matchesFilter(feature, styleLayer.filter)) return@forEach

        // Build path from geometry
        val path = buildPathFromGeometry(feature.geometry, layer.extent, tileSize, scaleAdjustment, tileOffset)

        // Draw line
        canvas.drawPath(
            path,
            Paint().apply {
                color = lineColor.copy(alpha = lineOpacity.toFloat())
                isAntiAlias = true
                style = PaintingStyle.Stroke
                strokeWidth = lineWidth * scaleAdjustment
                strokeCap = when (lineCap) {
                    "round" -> StrokeCap.Round
                    "square" -> StrokeCap.Square
                    else -> StrokeCap.Butt
                }
                strokeJoin = when (lineJoin) {
                    "round" -> StrokeJoin.Round
                    "bevel" -> StrokeJoin.Bevel
                    else -> StrokeJoin.Miter
                }
            }
        )
    }
}

/**
 * Renders a circle layer (Point features as circles)
 */
private fun DrawScope.renderCircleLayer(
    layer: MVTLayer,
    styleLayer: StyleLayer,
    tileOffset: Offset,
    tileSize: TileDimension,
    scaleAdjustment: Float,
    canvas: Canvas
) {
    val circleRadius = extractNumberProperty(styleLayer.paint, "circle-radius", 5.0).toFloat()
    val circleColor = extractColorProperty(styleLayer.paint, "circle-color", Color.Blue)
    val circleOpacity = extractOpacityProperty(styleLayer.paint, "circle-opacity", 1.0)
    val circleStrokeColor = extractColorProperty(styleLayer.paint, "circle-stroke-color", Color.White)
    val circleStrokeWidth = extractNumberProperty(styleLayer.paint, "circle-stroke-width", 0.0).toFloat()

    layer.features.forEach { feature ->
        // Only render point features for circle layers
        if (feature.type != RawMVTGeomType.POINT) return@forEach

        // Check if feature matches the layer filter
        if (!matchesFilter(feature, styleLayer.filter)) return@forEach

        // Extract point coordinates
        feature.geometry.forEach { ring ->
            ring.forEach { (x, y) ->
                // Convert extent coordinates to canvas coordinates
                val canvasX = tileOffset.x + (x.toFloat() / layer.extent) * tileSize.width * scaleAdjustment
                val canvasY = tileOffset.y + (y.toFloat() / layer.extent) * tileSize.height * scaleAdjustment

                val scaledRadius = circleRadius * scaleAdjustment

                // Draw filled circle
                canvas.drawCircle(
                    Offset(canvasX, canvasY),
                    scaledRadius,
                    Paint().apply {
                        color = circleColor.copy(alpha = circleOpacity.toFloat())
                        isAntiAlias = true
                        style = PaintingStyle.Fill
                    }
                )

                // Draw stroke if specified
                if (circleStrokeWidth > 0) {
                    canvas.drawCircle(
                        Offset(canvasX, canvasY),
                        scaledRadius,
                        Paint().apply {
                            color = circleStrokeColor
                            isAntiAlias = true
                            style = PaintingStyle.Stroke
                            strokeWidth = circleStrokeWidth * scaleAdjustment
                        }
                    )
                }
            }
        }
    }
}

/**
 * Placeholder for symbol layer rendering (text labels)
 * Full implementation would require font loading and text rendering
 */
private fun DrawScope.renderSymbolLayer(
    layer: MVTLayer,
    styleLayer: StyleLayer,
    tileOffset: Offset,
    tileSize: TileDimension,
    scaleAdjustment: Float,
    canvas: Canvas
) {
    // Symbol layer rendering would go here
    // This includes text rendering with style properties like:
    // - text-field: which property to display
    // - text-size, text-color, text-opacity
    // - text-offset, text-anchor, text-justify
    // For now, this is left as a placeholder
}

/**
 * Builds a Path from MVT geometry coordinates
 * Converts extent-based coordinates [0, extent] to canvas coordinates
 */
private fun buildPathFromGeometry(
    geometry: List<List<Pair<Int, Int>>>,
    extent: Int,
    tileSize: TileDimension,
    scaleAdjustment: Float,
    tileOffset: Offset = Offset.Zero
): ComposePath {
    val path = ComposePath()
    val tileSizePixels = tileSize.width * scaleAdjustment

    geometry.forEach { ring ->
        if (ring.isEmpty()) return@forEach

        val (startX, startY) = ring.first()
        val startCanvasX = (startX.toFloat() / extent) * tileSizePixels + tileOffset.x
        val startCanvasY = (startY.toFloat() / extent) * tileSizePixels + tileOffset.y

        path.moveTo(startCanvasX, startCanvasY)

        ring.drop(1).forEach { (x, y) ->
            val canvasX = (x.toFloat() / extent) * tileSizePixels + tileOffset.x
            val canvasY = (y.toFloat() / extent) * tileSizePixels + tileOffset.y
            path.lineTo(canvasX, canvasY)
        }

        // Close the path for polygons
        path.close()
    }

    return path
}

/**
 * Extracts and parses a color from style paint properties
 * Supports hex colors, rgb(), rgba(), hsl(), hsla(), and named colors
 */
private fun extractColorProperty(
    paint: Map<String, JsonElement>?,
    propertyName: String,
    defaultColor: Color
): Color {
    paint?.get(propertyName)?.let { element ->
        return parseColor(element, defaultColor)
    }
    return defaultColor
}

/**
 * Extracts opacity/alpha from style paint properties
 */
private fun extractOpacityProperty(
    paint: Map<String, JsonElement>?,
    propertyName: String,
    defaultOpacity: Double
): Double {
    paint?.get(propertyName)?.let { element ->
        return when {
            element is JsonPrimitive && element.isString -> {
                element.content.toDoubleOrNull() ?: defaultOpacity
            }

            element is JsonPrimitive && !element.isString -> {
                element.jsonPrimitive.content.toDoubleOrNull() ?: defaultOpacity
            }

            else -> defaultOpacity
        }
    }
    return defaultOpacity
}

/**
 * Extracts a numeric value from style properties (paint or layout)
 */
private fun extractNumberProperty(
    properties: Map<String, JsonElement>?,
    propertyName: String,
    defaultValue: Double
): Double {
    properties?.get(propertyName)?.let { element ->
        return when {
            element is JsonPrimitive && !element.isString -> {
                element.jsonPrimitive.content.toDoubleOrNull() ?: defaultValue
            }

            else -> defaultValue
        }
    }
    return defaultValue
}

/**
 * Extracts a string value from style properties (layout or paint)
 */
private fun extractStringProperty(
    properties: Map<String, JsonElement>?,
    propertyName: String,
    defaultValue: String
): String {
    properties?.get(propertyName)?.let { element ->
        return when {
            element is JsonPrimitive && element.isString -> element.content
            else -> defaultValue
        }
    }
    return defaultValue
}

/**
 * Parses a color from various formats: hex, rgb, rgba, hsl, hsla, and named colors
 */
private fun parseColor(element: JsonElement, defaultColor: Color): Color {
    return when {
        element is JsonPrimitive && element.isString -> {
            val colorStr = element.content.trim()
            when {
                colorStr.startsWith("#") -> parseHexColor(colorStr)
                colorStr.startsWith("rgb") -> parseRgbColor(colorStr)
                colorStr.startsWith("hsl") -> parseHslColor(colorStr)
                else -> parseNamedColor(colorStr) ?: defaultColor
            }
        }

        else -> defaultColor
    }
}

/**
 * Parses hex color format: #RGB, #RRGGBB, #RRGGBBAA
 */
private fun parseHexColor(hex: String): Color {
    val cleanHex = hex.removePrefix("#")
    return when (cleanHex.length) {
        3 -> { // #RGB
            val r = cleanHex[0].toString().repeat(2).toInt(16)
            val g = cleanHex[1].toString().repeat(2).toInt(16)
            val b = cleanHex[2].toString().repeat(2).toInt(16)
            Color(red = r, green = g, blue = b)
        }

        6 -> { // #RRGGBB
            val r = cleanHex.substring(0, 2).toInt(16)
            val g = cleanHex.substring(2, 4).toInt(16)
            val b = cleanHex.substring(4, 6).toInt(16)
            Color(red = r, green = g, blue = b)
        }

        8 -> { // #RRGGBBAA
            val r = cleanHex.substring(0, 2).toInt(16)
            val g = cleanHex.substring(2, 4).toInt(16)
            val b = cleanHex.substring(4, 6).toInt(16)
            val a = cleanHex.substring(6, 8).toInt(16)
            Color(red = r, green = g, blue = b, alpha = a)
        }

        else -> Color.Black
    }
}

/**
 * Parses rgb/rgba color format: rgb(r, g, b) or rgba(r, g, b, a)
 */
private fun parseRgbColor(rgbStr: String): Color {
    val values = rgbStr
        .removePrefix("rgb(").removePrefix("rgba(").removeSuffix(")")
        .split(",")
        .map { it.trim().toIntOrNull() ?: 0 }

    return when {
        values.size >= 4 -> Color(red = values[0], green = values[1], blue = values[2], alpha = values[3])
        values.size >= 3 -> Color(red = values[0], green = values[1], blue = values[2])
        else -> Color.Black
    }
}

/**
 * Parses hsl/hsla color format: hsl(h, s%, l%) or hsla(h, s%, l%, a)
 * Note: This is a simplified implementation. Full implementation would handle
 * decimal values and edge cases more robustly.
 */
private fun parseHslColor(hslStr: String): Color {
    val values = hslStr
        .removePrefix("hsl(").removePrefix("hsla(").removeSuffix(")")
        .split(",")
        .map { it.trim().replace("%", "").toDoubleOrNull() ?: 0.0 }

    if (values.size < 3) return Color.Black

    val h = values[0] / 360.0
    val s = values[1] / 100.0
    val l = values[2] / 100.0
    val alpha = if (values.size >= 4) (values[3] * 255).toInt() else 255

    // Convert HSL to RGB
    val (r, g, b) = hslToRgb(h, s, l)

    return Color(red = r, green = g, blue = b, alpha = alpha)
}

/**
 * Converts HSL to RGB color space
 * h, s, l are normalized to [0, 1] range
 */
private fun hslToRgb(h: Double, s: Double, l: Double): Triple<Int, Int, Int> {
    val c = (1 - kotlin.math.abs(2 * l - 1)) * s
    val hPrime = h * 6
    val x = c * (1 - kotlin.math.abs((hPrime % 2) - 1))

    val (rPrime, gPrime, bPrime) = when {
        hPrime < 1 -> Triple(c, x, 0.0)
        hPrime < 2 -> Triple(x, c, 0.0)
        hPrime < 3 -> Triple(0.0, c, x)
        hPrime < 4 -> Triple(0.0, x, c)
        hPrime < 5 -> Triple(x, 0.0, c)
        else -> Triple(c, 0.0, x)
    }

    val m = l - c / 2
    return Triple(
        ((rPrime + m) * 255).toInt(),
        ((gPrime + m) * 255).toInt(),
        ((bPrime + m) * 255).toInt()
    )
}

/**
 * Parses named CSS colors
 */
private fun parseNamedColor(name: String): Color? {
    return when (name.lowercase()) {
        "black" -> Color.Black
        "white" -> Color.White
        "red" -> Color.Red
        "green" -> Color.Green
        "blue" -> Color.Blue
        "yellow" -> Color.Yellow
        "cyan" -> Color.Cyan
        "magenta" -> Color.Magenta
        "gray", "grey" -> Color.Gray
        "transparent" -> Color.Transparent
        else -> null
    }
}

/**
 * Checks if a feature matches the layer filter criteria
 * Simplified implementation supporting basic equality filters
 * Full implementation would support complex filter expressions
 */
private fun matchesFilter(feature: MVTFeature, filter: List<JsonElement>?): Boolean {
    if (filter == null) return true
    if (filter.isEmpty()) return true

    // Simplified filter matching - in a full implementation, this would parse
    // complex filter expressions from the Mapbox style spec:
    // Examples: ["==", "class", "water"], ["in", "class", "water", "ocean"]
    // For now, we always return true to render all features
    return true
}

