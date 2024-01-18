package io.github.rafambn.kmap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import io.github.rafambn.kmap.gestures.GestureInterface
import io.github.rafambn.kmap.gestures.detectMapGestures

@Composable
internal fun MotionManager(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {

    val cameraState = rememberCameraState()

    val gestureListener = object : GestureInterface {
        override fun onTap(offset: Offset) {

        }

        override fun onDoubleTap(offset: Offset) {
            cameraState.zoom -= 1
        }

        override fun onTwoFingersTap(offset: Offset) {
            cameraState.zoom += 1
        }

        override fun onLongPress(offset: Offset) {

        }

        override fun onTapLongPress(offset: Offset) {
            cameraState.position += offset
        }

        override fun onTapSwipe(offset: Offset) {
            cameraState.zoom += offset.x
        }

        override fun onGesture(centroid: Offset, pan: Offset?, zoom: Float, rotation: Float) {
            cameraState.rotation -= rotation
            cameraState.zoom *= zoom
            pan?.let { cameraState.position += pan }
        }

        override fun onDrag(dragAmount: Offset) {
            cameraState.position += dragAmount
        }

        override fun onDragStart(offset: Offset) {

        }

        override fun onDragEnd() {

        }

        override fun onFling(velocity: Float) {

        }

        override fun onFlingZoom(velocity: Float) {

        }

        //        override fun onFlingRotation(velocity: Velocity) {
        override fun onFlingRotation(velocity: Float) {

        }

        override fun onHover(offset: Offset) {

        }

        override fun onScroll(offset: Offset) {
            cameraState.zoom -= offset.y
        }
    }

    Layout(
        content = content,
        modifier
            .background(Color.Gray)
            .clipToBounds()
            .fillMaxSize()
            .pointerInput(true) {
                detectMapGestures(
                    onTap = { offset -> gestureListener.onTap(offset) },
                    onDoubleTap = { offset -> gestureListener.onDoubleTap(offset) },
                    onTwoFingersTap = { offset -> gestureListener.onTwoFingersTap(offset) },
                    onLongPress = { offset -> gestureListener.onLongPress(offset) },
                    onTapLongPress = { offset -> gestureListener.onTapLongPress(offset) },
                    onTapSwipe = { offset -> gestureListener.onTapSwipe(offset) },
                    onGesture = { centroid, pan, zoom, rotation ->
                        gestureListener.onGesture(centroid, pan, zoom, rotation)
                    },
                    onDrag = { dragAmount -> gestureListener.onDrag(dragAmount) },
                    onDragStart = { offset -> gestureListener.onDragStart(offset) },
                    onDragEnd = { gestureListener.onDragEnd() },
                    onFling = { velocity -> gestureListener.onFling(velocity) },
                    onFlingZoom = { velocity -> gestureListener.onFling(velocity) },
                    onFlingRotation = { velocity -> gestureListener.onFling(velocity) },
                    onHover = { offset -> gestureListener.onHover(offset) },
                    onScroll = { offset -> gestureListener.onScroll(offset) }
                )
            }
            .onGloballyPositioned { coordinates ->
                cameraState.mapSize = coordinates.size
            }
    ) { measurables, constraints ->
        val placeables = measurables.map { measurable ->
            measurable.measure(constraints)
        }
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeables.forEach { placeable ->
                placeable.placeRelativeWithLayer(
                    x = cameraState._rawPosition.value.x.toInt(),
                    y = cameraState._rawPosition.value.y.toInt()
                ) {
                    rotationZ = cameraState.rotation
                    scaleX = cameraState.zoom
                    scaleY = cameraState.zoom
                }
            }
        }
    }
}