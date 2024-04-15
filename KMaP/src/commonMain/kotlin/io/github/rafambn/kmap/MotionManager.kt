package io.github.rafambn.kmap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.toSize
import io.github.rafambn.kmap.gestures.GestureInterface
import io.github.rafambn.kmap.gestures.GestureState
import io.github.rafambn.kmap.gestures.detectMapGestures

@Composable
internal fun MotionManager(
    modifier: Modifier = Modifier,
    mapState: MapState,
    content: @Composable () -> Unit
) {

    val gestureListener = object : GestureInterface {
        override fun onTap(offset: Offset) {
        }

        override fun onDoubleTap(centroid: Offset) {
            mapState.zoomBy(-1 / 3F, mapState.screenOffsetToMapReference(centroid))
        }

        override fun onTwoFingersTap(centroid: Offset) {
            mapState.zoomBy(1 / 3F, mapState.screenOffsetToMapReference(centroid))
        }

        override fun onLongPress(offset: Offset) {
        }

        override fun onTapLongPress(offset: Offset) {
            mapState.moveBy(mapState.offsetToMapReference(offset))
        }

        override fun onTapSwipe(centroid: Offset, zoom: Float) {
            mapState.zoomBy(zoom, mapState.offsetToMapReference(centroid))
        }

        override fun onGesture(centroid: Offset, pan: Offset, zoom: Float, rotation: Float) {
            mapState.rotateBy(rotation.toDouble(), mapState.offsetToMapReference(centroid))
            mapState.zoomBy(zoom, mapState.offsetToMapReference(centroid))
            mapState.moveBy(mapState.offsetToMapReference(centroid))
        }

        override fun onCtrlGesture(centroid: Offset, rotation: Float) {
            mapState.rotateBy(rotation.toDouble(), mapState.offsetToMapReference(centroid))
        }

        override fun onDrag(offset: Offset) {
            mapState.moveBy(mapState.offsetToMapReference(offset))
        }

        override fun onGestureStart(gestureType: GestureState, offset: Offset) {
        }

        override fun onGestureEnd(gestureType: GestureState) {
        }

        override fun onFling(velocity: Velocity) { //TODO add fling back again
        }

        override fun onFlingZoom(centroid: Offset, velocity: Float) {
        }

        override fun onFlingRotation(centroid: Offset, velocity: Float) {
        }

        override fun onHover(offset: Offset) {

        }

        override fun onScroll(mouseOffset: Offset, scrollAmount: Float) {
            mapState.zoomBy(scrollAmount, mapState.screenOffsetToMapReference(mouseOffset))
        }
    }

    Layout(
        content = content,
        modifier
            .background(Color.Gray)
            .clipToBounds()
            .fillMaxSize()
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
                    onCtrlGesture = {centroid, rotation -> gestureListener.onCtrlGesture(centroid, rotation)}
                )
            }
            .onGloballyPositioned { coordinates ->
                mapState.onCanvasSizeChanged(coordinates.size.toSize().toRect().bottomRight)
            }
    ) { measurables, constraints ->

        val canvasPlaceable = measurables.first {
            it.layoutId == MapComponentType.CANVAS
        }.measure(constraints)

        layout(constraints.maxWidth, constraints.maxHeight) {
            canvasPlaceable.placeRelativeWithLayer(x = 0, y = 0, zIndex = 0F)
        }
    }
}