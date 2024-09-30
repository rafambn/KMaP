package com.rafambn.kmapdemo.gestures

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.PI
import kotlin.math.atan2

/**
 * [detectMapGestures] detects all kinds of gestures needed for KMaP
 */
internal expect suspend fun PointerInputScope.detectMapGestures(
    onTap: (Offset) -> Unit, //common use
    onDoubleTap: (Offset) -> Unit,
    onLongPress: (Offset) -> Unit,
    onTapLongPress: (Offset) -> Unit,
    onTapSwipe: (centroid: Offset, zoom: Float) -> Unit,
    onDrag: (dragAmount: Offset) -> Unit,
    currentGestureFlow: MutableStateFlow<GestureState>? = null,

    onTwoFingersTap: (Offset) -> Unit, //mobile use
    onGesture: (centroid: Offset, pan: Offset, zoom: Float, rotation: Float) -> Unit,

    onHover: (Offset) -> Unit, //jvm/web use
    onScroll: (mouseOffset: Offset, scrollAmount: Float) -> Unit,
    onCtrlGesture: (rotation: Float) -> Unit
)

/**
 * [getGestureStateChanges] detects what type of change happened from the last input to the new one.
 */
fun getGestureStateChanges(
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

    //Goes from two click to one clicks or no click
    if (currentEvent.changes.size == 2 && currentEvent.changes.any { !it.pressed && it.previousPressed }) {
        if (currentEvent.changes.all { it.pressed })
            gestureChangeStates.add(GestureChangeState.TWO_RELEASE)
        else {
            gestureChangeStates.add(GestureChangeState.TWO_RELEASE)
            gestureChangeStates.add(GestureChangeState.RELEASE)
        }
    }

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
    intSize: IntSize,
    result: (rotationChange: Float) -> Unit
) {
    val screenCenter = Offset(intSize.width.toFloat(), intSize.height.toFloat())
    val currentCentroid = screenCenter.calculateCentroidWithCtrl(event)
    val previousCentroid = screenCenter.calculateCentroidWithCtrl(previousEvent)
    result(previousCentroid.angle() - currentCentroid.angle())
}

private fun Offset.angle(): Float =
    if (x == 0f && y == 0f) 0f else atan2(x, y) * 180f / PI.toFloat()

fun Offset.calculateCentroidWithCtrl(
    pointerEvent: PointerEvent
): Offset {
    return pointerEvent.changes[0].position - this
}