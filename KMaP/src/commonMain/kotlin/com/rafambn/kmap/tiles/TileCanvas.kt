package com.rafambn.kmap.tiles

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
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.rafambn.kmap.gestures.MapGestureWrapper
import com.rafambn.kmap.gestures.detectMapGestures
import com.rafambn.kmap.gestures.sharedPointerInput
import com.rafambn.kmap.utils.CanvasDrawReference
import com.rafambn.kmap.utils.ScreenOffset
import com.rafambn.kmap.utils.toIntFloor
import kotlin.math.pow

@Composable
internal fun TileCanvas(
    canvasSize: ScreenOffset,
    magnifierScale: Float,
    positionOffset: CanvasDrawReference,
    tileSize: TileDimension,
    rotationDegrees: Float,
    translation: Offset,
    gestureWrapper: MapGestureWrapper?,
    tileLayers: TileLayers
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
                        onScroll = gestureWrapper.onScroll,
                    )
                }
            } ?: Modifier)
            .drawBehind {
                withTransform({
                    translate(translation.x, translation.y)
                    rotate(rotationDegrees, Offset.Zero)
                    scale(2F.pow(magnifierScale), Offset.Zero)
                }) {
                    drawIntoCanvas { canvas ->
                        drawTiles(
                            tileLayers.backLayer.tiles,
                            tileSize,
                            positionOffset,
                            2F.pow(tileLayers.frontLayer.level - tileLayers.backLayer.level),
                            canvas
                        )
                        drawTiles(
                            tileLayers.frontLayer.tiles,
                            tileSize,
                            positionOffset,
                            1F,
                            canvas
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
    canvas: Canvas
) {
    tiles.forEach { tile ->
        tile.imageBitmap?.let {
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
    }
}
