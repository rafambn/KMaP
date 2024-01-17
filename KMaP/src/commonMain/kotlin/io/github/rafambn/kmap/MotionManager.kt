package io.github.rafambn.kmap

import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Velocity
import io.github.rafambn.kmap.gestures.GestureInterface
import io.github.rafambn.kmap.gestures.detectMapGestures
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
internal fun MotionManager(
    modifier: Modifier = Modifier,
//    cameraState: CameraState,
    content: @Composable () -> Unit
) {
    val topLeft = mutableStateOf(Offset(0F, 0F))
    val scale = mutableStateOf(1F)
    val angle = mutableStateOf(0F)

    val gestureListener = object : GestureInterface {
        override fun onTap(offset: Offset) {

        }

        override fun onDoubleTap(offset: Offset) {
            scale.value -= 1
        }

        override fun onTwoFingersTap(offset: Offset) {
            scale.value += 1
        }

        override fun onLongPress(offset: Offset) {

        }

        override fun onTapLongPress(offset: Offset) {
            topLeft.value += offset
        }

        override fun onTapSwipe(offset: Offset) {
            scale.value += offset.x
        }

        override fun onGesture(centroid: Offset, pan: Offset?, zoom: Float, rotation: Float) {
            angle.value -= rotation
            scale.value *= zoom
            pan?.let { topLeft.value += pan }
        }

        override fun onDrag(dragAmount: Offset) {
            topLeft.value += dragAmount
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
            scale.value -= offset.y
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
                    onFlingZoom = { velocity -> gestureListener.onFling(velocity)  },
                    onFlingRotation = { velocity -> gestureListener.onFling(velocity)  },
                    onHover = { offset -> gestureListener.onHover(offset) },
                    onScroll = { offset -> gestureListener.onScroll(offset)}
                )
            }

    ) { measurables, constraints ->
        val placeables = measurables.map { measurable ->
            measurable.measure(constraints)
        }
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeables.forEach { placeable ->
                placeable.placeRelativeWithLayer(x = topLeft.value.x.toInt(), y = topLeft.value.y.toInt()) {
                    rotationZ = angle.value
                    scaleX = scale.value
                    scaleY = scale.value
                }
            }
        }
    }
}