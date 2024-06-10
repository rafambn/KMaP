package io.github.rafambn.kmap.core

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
import io.github.rafambn.kmap.model.TileCanvasStateModel
import io.github.rafambn.kmap.utils.toIntFloor
import kotlin.math.pow

@Composable
internal fun TileCanvas(
    modifier: Modifier,
    tileCanvasStateModel: TileCanvasStateModel,
    gestureListener: GestureInterface
) {
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
            scale(tileCanvasStateModel.magnifierScale)
            rotate(tileCanvasStateModel.rotation)
            translate(tileCanvasStateModel.translation.x, tileCanvasStateModel.translation.y)
        }) {
            drawIntoCanvas { canvas ->
                val adjustedTileSize =
                    2F.pow(tileCanvasStateModel.tileLayers.frontLayerLevel - tileCanvasStateModel.tileLayers.backLayerLevel)
                for (tile in tileCanvasStateModel.tileLayers.backLayer.toList()) {
                    tile.imageBitmap?.let {
                        canvas.drawImageRect(image = it,
                            dstOffset = IntOffset(
                                (tileCanvasStateModel.tileSize * tile.row * adjustedTileSize + tileCanvasStateModel.positionOffset.horizontal).dp.toPx().toIntFloor(),
                                (tileCanvasStateModel.tileSize * tile.col * adjustedTileSize + tileCanvasStateModel.positionOffset.vertical).dp.toPx().toIntFloor()
                            ),
                            dstSize = IntSize(
                                (tileCanvasStateModel.tileSize.dp.toPx() * adjustedTileSize).toIntFloor(),
                                (tileCanvasStateModel.tileSize.dp.toPx() * adjustedTileSize).toIntFloor()
                            ),
                            paint = Paint().apply {
                                isAntiAlias = false
                                filterQuality = FilterQuality.High
                            }
                        )
                    }
                }
                for (tile in tileCanvasStateModel.tileLayers.frontLayer.toList()) {
                    tile.imageBitmap?.let {
                        canvas.drawImageRect(image = it,
                            dstOffset = IntOffset(
                                (tileCanvasStateModel.tileSize * tile.row + tileCanvasStateModel.positionOffset.horizontal).dp.toPx().toIntFloor(),
                                (tileCanvasStateModel.tileSize * tile.col + tileCanvasStateModel.positionOffset.vertical).dp.toPx().toIntFloor()
                            ),
                            dstSize = IntSize(
                                tileCanvasStateModel.tileSize.dp.toPx().toIntFloor(),
                                tileCanvasStateModel.tileSize.dp.toPx().toIntFloor()
                            ),
                            paint = Paint().apply {
                                isAntiAlias = false
                                filterQuality = FilterQuality.High
                            }
                        )
                    }
                }
            }
        }
    }
}