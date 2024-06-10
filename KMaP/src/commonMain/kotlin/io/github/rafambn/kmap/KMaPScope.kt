package io.github.rafambn.kmap

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.rafambn.kmap.core.DrawPosition
import io.github.rafambn.kmap.core.MapComponentData
import io.github.rafambn.kmap.core.Placer
import io.github.rafambn.kmap.core.componentData
import io.github.rafambn.kmap.gestures.GestureInterface
import io.github.rafambn.kmap.gestures.detectMapGestures
import io.github.rafambn.kmap.model.TileCanvasStateModel
import io.github.rafambn.kmap.utils.offsets.CanvasPosition
import io.github.rafambn.kmap.utils.rotateCentered
import io.github.rafambn.kmap.utils.toIntFloor
import io.github.rafambn.kmap.utils.toRadians
import kotlin.math.pow

interface KMaPScope {
    @Composable
    fun placers(items: List<Placer>, markerContent: @Composable (Placer) -> Unit) = items.forEach { item ->
        Layout(
            content = { markerContent(item) },
            modifier = Modifier
                .componentData(MapComponentData(item.coordinates, item.zIndex, item.drawPosition, item.angle)),
            measurePolicy = { measurables, constraints ->
                val placeable = measurables.first().measure(constraints)
                layout(placeable.width, placeable.height) {
                    placeable.placeWithLayer(//TODO see if all this math can be avoid just by changing where things are calculated
                        x = (-item.drawPosition.x * placeable.width + if (item.scaleWithMap) (1 - 1 / 2F.pow(item.zoom - item.zoomToFix)) * placeable.width / 2 else 0F).toInt(),
                        y = (-item.drawPosition.y * placeable.height + if (item.scaleWithMap) (1 - 1 / 2F.pow(item.zoom - item.zoomToFix)) * placeable.height / 2 else 0F).toInt(),
                        zIndex = item.zIndex
                    ) {
                        if (item.scaleWithMap) {
                            scaleX = 2F.pow(item.zoom - item.zoomToFix)
                            scaleY = 2F.pow(item.zoom - item.zoomToFix)
                        }
                        if (item.rotateWithMap) {
                            val center = CanvasPosition(
                                -(placeable.width) / 2.0,
                                -(placeable.height) / 2.0
                            )
                            val place = CanvasPosition.Zero.rotateCentered(center, item.angle.toRadians())
                            translationX = place.horizontal.toFloat()
                            translationY = place.vertical.toFloat()
                            rotationZ = item.angle.toFloat()
                        }
                    }
                }
            }
        )
    }

    @Composable
    fun tileCanvas(
        zIndex: Float,
        alpha: Float,
        gestureListener: GestureInterface,
        tileCanvasStateModel: TileCanvasStateModel,
    ) = Layout(
        modifier = Modifier.componentData(MapComponentData(Offset.Zero, zIndex, DrawPosition.TOP_LEFT, 0.0))
            .fillMaxSize()
            .alpha(alpha)
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
            }.drawBehind {
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
                                        (tileCanvasStateModel.tileSize * tile.row * adjustedTileSize + tileCanvasStateModel.positionOffset.horizontal).dp.toPx()
                                            .toIntFloor(),
                                        (tileCanvasStateModel.tileSize * tile.col * adjustedTileSize + tileCanvasStateModel.positionOffset.vertical).dp.toPx()
                                            .toIntFloor()
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
                                        (tileCanvasStateModel.tileSize * tile.row + tileCanvasStateModel.positionOffset.horizontal).dp.toPx()
                                            .toIntFloor(),
                                        (tileCanvasStateModel.tileSize * tile.col + tileCanvasStateModel.positionOffset.vertical).dp.toPx()
                                            .toIntFloor()
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
            },
        measurePolicy = { _, constraints ->
            with(constraints) {
                layout(maxWidth, maxHeight) {}
            }
        }
    )

    companion object : KMaPScope
}