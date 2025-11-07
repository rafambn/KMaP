package com.rafambn.kmap.mapSource.tiled.raster

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Paint
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
import com.rafambn.kmap.mapSource.tiled.RasterTile
import com.rafambn.kmap.mapSource.tiled.Tile
import com.rafambn.kmap.mapSource.tiled.TileDimension
import com.rafambn.kmap.mapSource.tiled.ActiveTiles
import com.rafambn.kmap.utils.CanvasDrawReference
import com.rafambn.kmap.utils.ScreenOffset
import com.rafambn.kmap.utils.asScreenOffset
import com.rafambn.kmap.utils.toIntFloor
import kotlin.math.pow

@Composable
internal fun RasterTileCanvas(
    canvasSize: ScreenOffset,
    gestureWrapper: MapGestureWrapper?,
    magnifierScale: () -> Float,
    positionOffset: () -> CanvasDrawReference,
    tileSize: () -> TileDimension,
    rotationDegrees: () -> Float,
    translation: () -> Offset,
    activeTiles: () -> ActiveTiles,
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
                val activeTiles = activeTiles()
                withTransform({
                    translate(translation.x, translation.y)
                    rotate(rotation, Offset.Zero)
                    scale(2F.pow(magnifierScale), Offset.Zero)
                }) {
                    drawIntoCanvas { canvas ->
                        activeTiles.tiles.forEach { tile ->
                            val scaleAdjustment = 2F.pow(activeTiles.currentZoom - tile.zoom)
                            drawRasterTiles(
                                listOf(tile),
                                tileSize,
                                positionOffset,
                                scaleAdjustment,
                                canvas,
                            )
                        }
                    }
                }
            }
    ) { _, _ ->
        layout(canvasSize.xInt, canvasSize.yInt) {}
    }
}

private fun DrawScope.drawRasterTiles(
    tiles: List<Tile>,
    tileSize: TileDimension,
    positionOffset: CanvasDrawReference,
    scaleAdjustment: Float = 1F,
    canvas: Canvas,
) {
    tiles.forEach { tile ->
        canvas.drawImageRect(
            image = (tile as RasterTile).imageBitmap!!,
            dstOffset = IntOffset(
                (tileSize.width * tile.col * scaleAdjustment + positionOffset.x).dp.toPx().toIntFloor(),
                (tileSize.height * tile.row * scaleAdjustment + positionOffset.y).dp.toPx().toIntFloor()
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
}
