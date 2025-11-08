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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import com.rafambn.kmap.utils.style.OptimizedStyle
import com.rafambn.kmap.utils.style.OptimizedStyleLayer
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
        val layerId = styleLayer.id

        tiles.forEach { tile ->
            val scaleAdjustment = 2F.pow(currentZoom - tile.zoom)
            drawVectorTileLayerWithClipping(
                tile as OptimizedVectorTile,
                layerId,
                tileSize,
                positionOffset,
                scaleAdjustment,
                canvas,
                fontResolver,
                density
            )
        }
    }
}

private fun DrawScope.drawVectorTileLayerWithClipping(
    tile: OptimizedVectorTile,
    layerId: String,
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

        optimizedData.layerFeatures[layerId]?.forEach { renderFeature ->
            drawRenderFeature(canvas, renderFeature, scaleAdjustment, fontResolver, density)
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
    val backgroundColor = backgroundLayer.paint.properties["background-color"]?.evaluate(zoomLevel.toDouble(), emptyMap(), "") as? Color ?: Color.Magenta
    val backgroundOpacity = backgroundLayer.paint.properties["background-opacity"]?.evaluate(zoomLevel.toDouble(), emptyMap(), "") as? Float ?: 1F

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
