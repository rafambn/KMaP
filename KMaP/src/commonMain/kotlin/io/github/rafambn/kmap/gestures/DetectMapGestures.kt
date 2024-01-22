package io.github.rafambn.kmap.gestures

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.isCtrlPressed
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2

/**
 * [detectMapGestures] detects all kinds of gestures needed for KMaP
 */
internal suspend fun PointerInputScope.detectMapGestures(   //TODO There are probably a lot of optimization or better ways to implements this function.
    onTap: ((Offset) -> Unit)?,
    onDoubleTap: ((Offset) -> Unit)?,
    onTwoFingersTap: ((Offset) -> Unit)?,
    onLongPress: ((Offset) -> Unit)?,
    onTapLongPress: ((Offset) -> Unit)?,
    onTapSwipe: ((Offset) -> Unit)?,

    onGesture: (centroid: Offset, pan: Offset, zoom: Float, rotation: Float) -> Unit,

    onDrag: (dragAmount: Offset) -> Unit,
    onDragStart: (Offset) -> Unit = { },
    onDragEnd: () -> Unit = { },

    onFling: ((velocity: Float) -> Unit) = {},
    onFlingZoom: ((velocity: Float) -> Unit) = { },
    onFlingRotation: ((velocity: Float) -> Unit) = { },

    onHover: ((Offset) -> Unit)? = null,
    onScroll: ((mouseOffset: Offset, scrollAmount: Float) -> Unit)? = null
) = coroutineScope {

    // Launches separately coroutine checking if any event is not consumed or pressed indicating that the mouse is just hovering
    if (onHover != null)
        launch {
            awaitMapGesture {
                var event: PointerEvent
                do {
                    event = awaitPointerEvent()
                    if (event.type == PointerEventType.Move && !event.changes.any { it.isConsumed || it.pressed }) {
                        onHover.invoke(event.changes[0].position)
                    }
                } while (this@coroutineScope.isActive)
            }
        }

    // Launches separately coroutine if any event is of type scroll indicating that the user scrolled his mouse
    if (onScroll != null)
        launch {
            awaitMapGesture {
                var event: PointerEvent
                do {
                    event = awaitPointerEvent()
                    if (event.type == PointerEventType.Scroll) {
                        event.changes.forEach { it.consume() }
                        onScroll.invoke(event.changes[0].position, event.changes[0].scrollDelta.y)
                    }
                } while (this@coroutineScope.isActive)
            }
        }

    awaitMapGesture {
        lateinit var event: PointerEvent
        var previousEvent: PointerEvent? = null
        var gestureState = GestureState.HOVER

        var firstCtrlEvent: PointerEvent? = null
        val longPressTimeout = viewConfiguration.longPressTimeoutMillis
        val doubleTapTimeout = viewConfiguration.doubleTapTimeoutMillis
        val touchSlop = viewConfiguration.touchSlop
        var timeoutCount = 0L
        var rotationSlop = 0f
        var zoomSlop = 1f
        var panSlop = Offset.Zero
        var hasTimeOut = false

        val firsPointerEvent = awaitPointerEvent()
        do {
            //Awaits for a pointer event
            if (previousEvent != null) {
                try {
                    val tempEvent = withTimeout(10L) {
                        awaitPointerEvent()
                    }
                    hasTimeOut = false
                    previousEvent = event
                    event = tempEvent
                } catch (_: PointerEventTimeoutCancellationException) {
                    hasTimeOut = true
                    previousEvent = event
                }
            }

            if (previousEvent == null) {
                previousEvent = firsPointerEvent
                event = firsPointerEvent
            }

            //Click event Types
            val eventChanges = getGestureStateChanges(event, previousEvent)

            //Here are the cases that leads to an gesture

            if (eventChanges.any { it == GestureChangeState.PRESS } && !event.keyboardModifiers.isCtrlPressed && gestureState == GestureState.HOVER) {
                gestureState = GestureState.WAITING_UP
                timeoutCount = longPressTimeout
                continue
            }

            if (gestureState == GestureState.WAITING_UP) {
                timeoutCount -= if (hasTimeOut)
                    10L
                else
                    event.changes[0].uptimeMillis - previousEvent.changes[0].uptimeMillis
                if (timeoutCount < 0) {
                    onLongPress?.invoke(event.changes[0].position)
                    gestureState = GestureState.HOVER
                    continue
                }
                if (eventChanges.any { it == GestureChangeState.RELEASE }) {
                    gestureState = GestureState.WAITING_DOWN
                    timeoutCount = doubleTapTimeout
                    continue
                }
                if (eventChanges.any { it == GestureChangeState.TWO_PRESS }) {
                    gestureState = GestureState.WAITING_UP_AFTER_TWO_PRESS
                    timeoutCount = longPressTimeout
                    continue
                }
                if (event.type == PointerEventType.Move) {
                    panSlop += event.calculatePan()
                    if (panSlop.getDistance() > touchSlop) {
                        onDragStart.invoke(event.changes[0].position)
                        gestureState = GestureState.DRAG
                        panSlop = Offset.Zero
                    }
                    continue
                }
                continue
            }

            if (gestureState == GestureState.WAITING_DOWN) {
                timeoutCount -= if (hasTimeOut)
                    10L
                else
                    event.changes[0].uptimeMillis - previousEvent.changes[0].uptimeMillis
                if (timeoutCount < 0) {
                    onTap?.invoke(event.changes[0].position)
                    gestureState = GestureState.HOVER
                    continue
                }
                if (eventChanges.any { it == GestureChangeState.PRESS }) {
                    gestureState = GestureState.WAITING_UP_AFTER_TAP
                    timeoutCount = longPressTimeout
                    continue
                }
                continue
            }

            if (gestureState == GestureState.WAITING_UP_AFTER_TAP) {
                timeoutCount -= if (hasTimeOut)
                    10L
                else
                    event.changes[0].uptimeMillis - previousEvent.changes[0].uptimeMillis
                if (timeoutCount < 0) {
                    gestureState = GestureState.TAP_LONG_PRESS
                    continue
                }
                if (eventChanges.any { it == GestureChangeState.RELEASE }) {
                    onDoubleTap?.invoke(event.changes[0].position)
                    gestureState = GestureState.HOVER
                    continue
                }
                if (event.type == PointerEventType.Move) {
                    panSlop += event.calculatePan()
                    if (panSlop.getDistance() > touchSlop) {
                        gestureState = GestureState.TAP_MAP
                        panSlop = Offset.Zero
                    }
                    continue
                }
                continue
            }

            if (gestureState == GestureState.WAITING_UP_AFTER_TWO_PRESS) {
                timeoutCount -= if (hasTimeOut)
                    10L
                else
                    event.changes[0].uptimeMillis - previousEvent.changes[0].uptimeMillis
                if (timeoutCount < 0) {
                    gestureState = GestureState.MOBILE
                    continue
                }
                if (event.changes.all { !it.pressed }) {
                    onTwoFingersTap?.invoke(event.changes[0].position)
                    gestureState = GestureState.HOVER
                    continue
                }
                if (event.type == PointerEventType.Move) {
                    zoomSlop *= event.calculateZoom()
                    rotationSlop += event.calculateRotation()
                    panSlop += event.calculatePan()
                    val centroidSize = event.calculateCentroidSize(useCurrent = false)
                    val zoomMotion = abs(1 - zoomSlop) * centroidSize
                    val rotationMotion = abs(rotationSlop * PI.toFloat() * centroidSize / 180f)
                    val panMotion = panSlop.getDistance()
                    if (zoomMotion > touchSlop ||
                        rotationMotion > touchSlop ||
                        panMotion > touchSlop
                    ) {
                        gestureState = GestureState.MOBILE
                        rotationSlop = 0f
                        zoomSlop = 1f
                        panSlop = Offset.Zero
                    }
                    continue
                }
                continue
            }

            if (eventChanges.any { it == GestureChangeState.TWO_PRESS } && gestureState == GestureState.DRAG) {
                gestureState = GestureState.MOBILE
                onDragEnd.invoke()
                continue
            }

            if (eventChanges.any { it == GestureChangeState.PRESS } && event.keyboardModifiers.isCtrlPressed && gestureState == GestureState.HOVER) {
                gestureState = GestureState.CTRL
                firstCtrlEvent = event
                continue
            }

            if (eventChanges.any { it == GestureChangeState.CTRL_PRESS } && gestureState == GestureState.DRAG) {
                onDragEnd.invoke()
                gestureState = GestureState.CTRL
                firstCtrlEvent = event
                continue
            }

            //Here are the cases that exits of an gesture to another
            if (eventChanges.any { it == GestureChangeState.CTRL_RELEASE } && gestureState == GestureState.CTRL) {
                onDragStart.invoke(event.changes[0].position)
                gestureState = GestureState.DRAG
                continue
            }

            if (eventChanges.any { it == GestureChangeState.RELEASE } && gestureState == GestureState.CTRL) {
                gestureState = GestureState.HOVER
                handleGestureWithCtrl(event, previousEvent, firstCtrlEvent!!, touchSlop) { rotationChange, centroid ->
                    val rotationVelocity = abs(rotationChange) / (previousEvent.changes[0].uptimeMillis - previousEvent.changes[0].previousUptimeMillis)

                    if (rotationVelocity > 0.5) {
                        onFlingRotation.invoke(rotationChange)
                    }
                }
                continue
            }

            if (gestureState == GestureState.MOBILE && eventChanges.any { it == GestureChangeState.TWO_RELEASE }) {
                val zoomChange = event.calculateZoom()
                val rotationChange = event.calculateRotation()
                val panChange = event.calculatePan()

                val panVelocity = abs(panChange.getDistance()) / (previousEvent.changes[0].uptimeMillis - previousEvent.changes[0].previousUptimeMillis)
                val zoomVelocity = abs(zoomChange - 1) / (previousEvent.changes[0].uptimeMillis - previousEvent.changes[0].previousUptimeMillis)
                val rotationVelocity = abs(rotationChange) / (previousEvent.changes[0].uptimeMillis - previousEvent.changes[0].previousUptimeMillis)

                if (event.changes[0].pressed) {
                    onFlingZoom.invoke(zoomVelocity)
                    onFlingRotation.invoke(rotationVelocity)

                    onDragStart.invoke(event.changes[0].position)
                    gestureState = GestureState.DRAG
                    continue
                } else {
                    onFling.invoke(panVelocity)
                    onFlingZoom.invoke(zoomVelocity)
                    onFlingRotation.invoke(rotationVelocity)

                    gestureState = GestureState.HOVER
                    continue
                }
            }

            if (gestureState == GestureState.TAP_LONG_PRESS && eventChanges.any { it == GestureChangeState.RELEASE }) {
                gestureState = GestureState.HOVER
                continue
            }

            if (gestureState == GestureState.TAP_MAP && event.changes.all { !it.pressed }) {
                onFling.invoke(
                    abs(
                        event.calculatePan().getDistance()
                    ) / (previousEvent.changes[0].uptimeMillis - previousEvent.changes[0].previousUptimeMillis)
                )
                gestureState = GestureState.HOVER
                continue
            }

            if (gestureState == GestureState.DRAG && eventChanges.any { it == GestureChangeState.RELEASE }) {
                onDragEnd.invoke()
                onFling.invoke(
                    abs(
                        event.calculatePan().getDistance()
                    ) / (previousEvent.changes[0].uptimeMillis - previousEvent.changes[0].previousUptimeMillis)
                )
                gestureState = GestureState.HOVER
                continue
            }

            //Finally, here are the gestures
            if (gestureState == GestureState.CTRL) {
                handleGestureWithCtrl(event, previousEvent, firstCtrlEvent!!, touchSlop) { rotationChange, centroid ->
                    onGesture.invoke(centroid, Offset.Zero, 0F, rotationChange)
                }
                continue
            }

            if (gestureState == GestureState.DRAG) {
                onDrag.invoke(event.changes[0].position - previousEvent.changes[0].position)
                continue
            }

            if (gestureState == GestureState.MOBILE) {
                val zoomChange = event.calculateZoom()
                val rotationChange = -event.calculateRotation()
                val panChange = event.calculatePan()
                val centroid = event.calculateCentroid()

                onGesture(centroid, panChange, zoomChange, rotationChange)
                continue
            }

            if (gestureState == GestureState.TAP_LONG_PRESS) {
                onTapLongPress?.invoke(event.changes[0].position - previousEvent.changes[0].position)
                continue
            }

            if (gestureState == GestureState.TAP_MAP) {
                onTapSwipe?.invoke(event.changes[0].position - previousEvent.changes[0].position)
                continue
            }

        } while (this@coroutineScope.isActive)
    }
}

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
 * not [awaitAllPointersUp].
 *
 * Repeatedly calls [block] to handle gestures. If there is a [CancellationException],
 * it will wait until all pointers are raised before another gesture is detected, or it
 * exits if [isActive] is `false`.
 */

internal suspend fun PointerInputScope.awaitMapGesture(block: suspend AwaitPointerEventScope.() -> Unit) {
    val currentContext = currentCoroutineContext()
    awaitPointerEventScope {
        while (currentContext.isActive) {
            try {
                block()
            } catch (e: CancellationException) {
                if (currentContext.isActive) {
                    // The current gesture was canceled. Wait for all fingers to be "up" before
                    // looping again.
                    awaitAllPointersUp()
                } else {
                    // detectGesture was cancelled externally. Rethrow the cancellation exception to
                    // propagate it upwards.
                    throw e
                }
            }
        }
    }
}

/**
 * Same version as [androidx.compose.foundation.gestures.awaitAllPointersUp] because the original is
 * internal
 */
internal suspend fun AwaitPointerEventScope.awaitAllPointersUp() {
    if (currentEvent.changes.any { it.pressed }) {
        do {
            val events = awaitPointerEvent(PointerEventPass.Final)
        } while (events.changes.any { it.pressed })
    }
}

fun handleGestureWithCtrl(
    event: PointerEvent,
    previousEvent: PointerEvent,
    firstCtrlEvent: PointerEvent,
    touchSlop: Float,
    result: (rotationChange: Float, centroid: Offset) -> Unit
) {
    val rotationChange = firstCtrlEvent.calculateRotationWithCtrl(previousEvent, event)
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