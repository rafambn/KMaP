package io.github.rafambn.kmap

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.rafambn.kmap.gestures.GestureInterface
import io.github.rafambn.kmap.gestures.detectMapGestures
import kotlin.math.floor

@Composable
internal fun TileCanvas(
    modifier: Modifier,
    translation: Offset,
    rotation: Float,
    magnifierScale: Float,
    visibleTilesList: List<Tile>,
    positionOffset: Position,
    mapState: Boolean,
    gestureListener: GestureInterface
) {
    remember { mapState }
    Canvas(
        modifier = modifier.fillMaxSize()
            .pointerInput(PointerEventPass.Main) {
                detectMapGestures(
                    onTap = { offset -> gestureListener.onTap(offset) },
                    onDoubleTap = { offset -> gestureListener.onDoubleTap(offset) },
                    onTwoFingersTap = { offset -> gestureListener.onTwoFingersTap(offset) },
                    onLongPress = { offset -> gestureListener.onLongPress(offset) },
                    onTapLongPress = { offset -> gestureListener.onTapLongPress(offset) },
                    onTapSwipe = { centroid, zoom -> gestureListener.onTapSwipe(centroid, zoom) },
                    onGesture = { centroid, pan, zoom, rotation -> gestureListener.onGesture(centroid, pan, zoom, rotation) },
                    onDrag = { dragAmount -> gestureListener.onDrag(dragAmount) },
                    onGestureStart = { gestureType, offset -> gestureListener.onGestureStart(gestureType, offset) },
                    onGestureEnd = { gestureType -> gestureListener.onGestureEnd(gestureType) },
                    onFling = { targetLocation -> gestureListener.onFling(targetLocation) },
                    onFlingZoom = { centroid, targetZoom -> gestureListener.onFlingZoom(centroid, targetZoom) },
                    onFlingRotation = { centroid, targetRotation -> gestureListener.onFlingRotation(centroid, targetRotation) },
                    onHover = { offset -> gestureListener.onHover(offset) },
                    onScroll = { mouseOffset, scrollAmount -> gestureListener.onScroll(mouseOffset, scrollAmount) },
                    onCtrlGesture = { rotation -> gestureListener.onCtrlGesture(rotation) }
                )
            }
    ) {
        withTransform({
            scale(magnifierScale)
            rotate(rotation)
            translate(translation.x, translation.y)
        }) {
            drawIntoCanvas { canvas ->
                for (tile in visibleTilesList) {
                    canvas.drawImageRect(image = tile.imageBitmap,
                        dstOffset = IntOffset(
                            floor((TileCanvasState.TILE_SIZE * tile.row + positionOffset.horizontal).dp.toPx()).toInt(),
                            floor((TileCanvasState.TILE_SIZE * tile.col + positionOffset.vertical).dp.toPx()).toInt()
                        ),
                        dstSize = IntSize(
                            TileCanvasState.TILE_SIZE.dp.toPx().toInt(),
                            TileCanvasState.TILE_SIZE.dp.toPx().toInt()
                        ),
                        paint = Paint().apply {
                            isAntiAlias = false
                            filterQuality = FilterQuality.High
                        }
                    )
                }
            }
        }
        drawIntoCanvas { //TODO remove later
            drawCircle(Color.Red, radius = 4F)
        }
    }
}