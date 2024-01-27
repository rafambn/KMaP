package io.github.rafambn.kmap.gestures

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.coroutineScope

/**
 * [detectMapGestures] detects all kinds of gestures needed for KMaP
 */
internal actual suspend fun PointerInputScope.detectMapGestures(
    onTap: (Offset) -> Unit,
    onDoubleTap: (Offset) -> Unit,
    onTwoFingersTap: (Offset) -> Unit,
    onLongPress: (Offset) -> Unit,
    onTapLongPress: (Offset) -> Unit,
    onTapSwipe: (centroid: Offset, zoom: Float) -> Unit,
    onGesture: (centroid: Offset, pan: Offset, zoom: Float, rotation: Float) -> Unit,
    onDrag: (dragAmount: Offset) -> Unit,
    onGestureStart: (gestureType: GestureState, offset: Offset) -> Unit,
    onGestureEnd: (gestureType: GestureState) -> Unit,
    onFling: (velocity: Velocity) -> Unit,
    onFlingZoom: (centroid: Offset, velocity: Float) -> Unit,
    onFlingRotation: (centroid: Offset,velocity: Float) -> Unit,
    onHover: (Offset) -> Unit,
    onScroll: (mouseOffset: Offset, scrollAmount: Float) -> Unit
) = coroutineScope {
}

/**
 * [awaitMapGesture] is a version of [awaitEachGesture] where after the gestures ends it does
 * not [awaitForGestureReset].
 *
 * Repeatedly calls [block] to handle gestures. If there is a [CancellationException],
 * it will wait until all pointers are raised before another gesture is detected, or it
 * exits if [isActive] is `false`.
 */
internal actual suspend fun PointerInputScope.awaitMapGesture(block: suspend AwaitPointerEventScope.() -> Unit) {
}

/**
 * Same version as [androidx.compose.foundation.gestures.awaitAllPointersUp] because the original is
 * internal
 */
internal actual suspend fun AwaitPointerEventScope.awaitForGestureReset() {
}