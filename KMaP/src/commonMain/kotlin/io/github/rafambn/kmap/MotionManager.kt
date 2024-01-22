package io.github.rafambn.kmap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onGloballyPositioned
import io.github.rafambn.kmap.enums.MapComponentType
import io.github.rafambn.kmap.gestures.GestureInterface
import io.github.rafambn.kmap.gestures.detectMapGestures
import kotlinx.coroutines.delay

@Composable
internal fun MotionManager(
    modifier: Modifier = Modifier,
    cameraState: CameraState,
    content: @Composable () -> Unit
) {
    LaunchedEffect(true) {
        delay(2000L)
        cameraState.rotate(Offset(100F, 100F), 45F)
//        delay(2000L)
//        cameraState.rotate(Offset(100F, 100F), 45F)
//        delay(2000L)
//        cameraState.rotate(Offset(100F, 100F), 45F)
    }
    val gestureListener = object : GestureInterface {
        override fun onTap(offset: Offset) {

        }

        override fun onDoubleTap(offset: Offset) {
            cameraState.scale(offset, -1 / 3F)
        }

        override fun onTwoFingersTap(offset: Offset) {
            cameraState.scale(offset, 1 / 3F)
        }

        override fun onLongPress(offset: Offset) {

        }

        override fun onTapLongPress(offset: Offset) {
            cameraState.move(offset)
        }

        override fun onTapSwipe(offset: Offset) {
            cameraState.scale(cameraState._rawPosition.value, offset.x / 30) //TODO use tileCenter and improve this scale change
        }

        override fun onGesture(centroid: Offset, pan: Offset, zoom: Float, rotation: Float) {
            cameraState.rotate(centroid, rotation)
            cameraState.scale(centroid, zoom / 3)
            cameraState.move(pan)
        }

        override fun onDrag(dragAmount: Offset) {
            cameraState.move(dragAmount)
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

        override fun onScroll(mouseOffset: Offset, scrollAmount: Float) {
            cameraState.scale(mouseOffset, scrollAmount / 3)
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
                    onScroll = { mouseOffset, scrollAmount -> gestureListener.onScroll(mouseOffset, scrollAmount) }
                )
            }
            .onGloballyPositioned { coordinates ->
                cameraState.mapSize = coordinates.size
//                println(coordinates.size)
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