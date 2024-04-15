package io.github.rafambn.kmap.gestures

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.isActive
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.PI
import kotlin.math.atan2

/**
 * [detectMapGestures] detects all kinds of gestures needed for KMaP
 */
internal expect suspend fun PointerInputScope.detectMapGestures(
    onTap: (Offset) -> Unit,
    onDoubleTap: (Offset) -> Unit,
    onTwoFingersTap: (Offset) -> Unit,
    onLongPress: (Offset) -> Unit,
    onTapLongPress: (Offset) -> Unit,
    onTapSwipe: (centroid: Offset, zoom: Float) -> Unit,

    onGesture: (centroid: Offset, pan: Offset, zoom: Float, rotation: Float) -> Unit,

    onDrag: (dragAmount: Offset) -> Unit,
    onGestureStart: (gestureType: GestureState, offset: Offset) -> Unit = { _, _ -> },
    onGestureEnd: (gestureType: GestureState) -> Unit = { },

    onFling: (velocity: Velocity) -> Unit = {},
    onFlingZoom: (centroid: Offset, velocity: Float) -> Unit = { _, _ -> },
    onFlingRotation: (centroid: Offset, velocity: Float) -> Unit = { _, _ -> },

    onHover: (Offset) -> Unit,
    onScroll: (mouseOffset: Offset, scrollAmount: Float) -> Unit,
    onCtrlGesture: (centroid: Offset, rotation: Float) -> Unit
)

/**
 * [getGestureStateChanges] detects what type of change happened from the last input to the new one.
 */
fun getGestureStateChanges( //TODO Verify if its possible to use only the parameters of PointerEvent to represent this states
    currentEvent: PointerEvent,
    previousEvent: PointerEvent
): List<GestureChangeState> {
    val gestureChangeStates = mutableListOf<GestureChangeState>()

    //Goes from no click to one click
    if (previousEvent.changes.all { !it.pressed } && currentEvent.changes[0].pressed)
        gestureChangeStates.add(GestureChangeState.PRESS)

    //Goes from one click to no click
    if (previousEvent.changes.size == 1 && previousEvent.changes[0].pressed && currentEvent.changes.size == 1 && !currentEvent.changes[0].pressed)
        gestureChangeStates.add(GestureChangeState.RELEASE)

    //Goes from one click to two clicks
    if (previousEvent.changes.size == 1 && currentEvent.changes.size == 2)
        gestureChangeStates.add(GestureChangeState.TWO_PRESS)


    //Goes from two click to one clicks
    if (previousEvent.changes.size == 2 && currentEvent.changes.size == 1)
        gestureChangeStates.add(GestureChangeState.TWO_RELEASE)

    //Goes from ctrl pressed to ctrl not pressed
    if (previousEvent.keyboardModifiers.isCtrlPressed && !currentEvent.keyboardModifiers.isCtrlPressed)
        gestureChangeStates.add(GestureChangeState.CTRL_RELEASE)

    //Goes from ctrl not pressed to ctrl pressed
    if (!previousEvent.keyboardModifiers.isCtrlPressed && currentEvent.keyboardModifiers.isCtrlPressed)
        gestureChangeStates.add(GestureChangeState.CTRL_PRESS)

    return gestureChangeStates
}

/**
 * [awaitMapGesture] is a version of [awaitEachGesture] where after the gestures ends it does
 * not [awaitForGestureReset].
 *
 * Repeatedly calls [block] to handle gestures. If there is a [CancellationException],
 * it will wait until all pointers are raised before another gesture is detected, or it
 * exits if [isActive] is `false`.
 */

internal expect suspend fun PointerInputScope.awaitMapGesture(block: suspend AwaitPointerEventScope.() -> Unit)

/**
 * Same version as [androidx.compose.foundation.gestures.awaitAllPointersUp] because the original is
 * internal
 */
internal expect suspend fun AwaitPointerEventScope.awaitForGestureReset()

fun handleGestureWithCtrl(
    event: PointerEvent,
    previousEvent: PointerEvent,
    firstCtrlEvent: PointerEvent,
    touchSlop: Float,
    result: (rotationChange: Float, centroid: Offset) -> Unit
) {
    val rotationChange = firstCtrlEvent.calculateRotationWithCtrl(previousEvent, event) //TODO improve this logic
    val centroidSize = firstCtrlEvent.calculateCentroidWithCtrl(event)

    if (touchSlop < centroidSize.getDistance())
        result(rotationChange, firstCtrlEvent.changes[0].position)
    else
        result(0F, Offset.Zero)
}

fun PointerEvent.calculateRotationWithCtrl(previousEvent: PointerEvent, currentEvent: PointerEvent): Float {
    val currentCentroid = this.calculateCentroidWithCtrl(currentEvent)
    val previousCentroid = this.calculateCentroidWithCtrl(previousEvent)

    return previousCentroid.angle() - currentCentroid.angle()
}

private fun Offset.angle(): Float =
    if (x == 0f && y == 0f) 0f else atan2(x, y) * 180f / PI.toFloat()

fun PointerEvent.calculateCentroidWithCtrl(
    currentEvent: PointerEvent
): Offset {
    return currentEvent.changes[0].position - this.changes[0].position
}