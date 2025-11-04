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
import com.rafambn.kmap.mapSource.tiled.Tile
import com.rafambn.kmap.mapSource.tiled.TileDimension
import com.rafambn.kmap.mapSource.tiled.TileLayers
import com.rafambn.kmap.utils.CanvasDrawReference
import com.rafambn.kmap.utils.ScreenOffset
import com.rafambn.kmap.utils.asScreenOffset
import com.rafambn.kmap.utils.style.Style
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
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
    tileLayers: () -> TileLayers,
    style: () -> Style,
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
                val tileLayers = tileLayers()
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
                                tileLayers.frontLayer.level
                            )
                        }

                        drawStyleLayersWithTileClipping(
                            tileLayers.backLayer.tiles,
                            tileLayers.frontLayer.tiles,
                            2F.pow(tileLayers.frontLayer.level - tileLayers.backLayer.level),
                            style,
                            tileSize,
                            positionOffset,
                            canvas,
                        )
                    }
                }
            }
    ) { _, _ ->
        layout(canvasSize.xInt, canvasSize.yInt) {}
    }
}

private fun DrawScope.drawStyleLayersWithTileClipping(
    backTiles: List<Tile>,
    frontTiles: List<Tile>,
    backScaleAdjustment: Float,
    style: Style,
    tileSize: TileDimension,
    positionOffset: CanvasDrawReference,
    canvas: Canvas,
) {
    style.layers.filter { it.type != "background" }.forEach { styleLayer ->
        val layerId = styleLayer.id

        backTiles.forEach { tile ->
            drawVectorTileLayerWithClipping(
                tile as OptimizedVectorTile,
                layerId,
                tileSize,
                positionOffset,
                backScaleAdjustment,
                canvas
            )
        }

        frontTiles.forEach { tile ->
            drawVectorTileLayerWithClipping(
                tile as OptimizedVectorTile,
                layerId,
                tileSize,
                positionOffset,
                1F,
                canvas
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
            drawRenderFeature(canvas, renderFeature, scaleAdjustment)
        }

        canvas.restore()
    }
}

private fun DrawScope.drawGlobalBackground(
    backgroundLayer: com.rafambn.kmap.utils.style.StyleLayer,
    canvas: Canvas,
    tileSize: TileDimension,
    positionOffset: CanvasDrawReference,
    zoomLevel: Int,
) {
    val backgroundColor = extractColorProperty(
        backgroundLayer.paint,
        "background-color",
        Color.White
    )
    val backgroundOpacity = extractOpacityProperty(
        backgroundLayer.paint,
        "background-opacity",
        1.0
    ).toFloat()

    val totalWidth = tileSize.width * (2F.pow(zoomLevel))
    val totalHeight = tileSize.height * (2F.pow(zoomLevel))

    canvas.drawRect(
        androidx.compose.ui.geometry.Rect(
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
