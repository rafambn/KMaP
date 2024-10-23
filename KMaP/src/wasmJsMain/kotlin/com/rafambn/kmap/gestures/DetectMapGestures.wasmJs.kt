package com.rafambn.kmap.gestures

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isOutOfBounds
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.properties.Delegates


/**
 * [detectMapGestures] detects all kinds of gestures needed for KMaP
 */
internal actual suspend fun PointerInputScope.detectMapGestures(
    onTap: (Offset) -> Unit,
    onDoubleTap: (Offset) -> Unit,
    onLongPress: (Offset) -> Unit,
    onTapLongPress: (Offset) -> Unit,
    onTapSwipe: (centroid: Offset, zoom: Float) -> Unit,
    onDrag: (dragAmount: Offset) -> Unit,
    currentGestureFlow: MutableStateFlow<GestureState>?,

    onTwoFingersTap: (Offset) -> Unit, //There isn't a call for this method in Js
    onGesture: (centroid: Offset, pan: Offset, zoom: Float, rotation: Float) -> Unit, //There isn't a call for this method in Js

    onHover: (Offset) -> Unit,
    onScroll: (mouseOffset: Offset, scrollAmount: Float) -> Unit,
    onCtrlGesture: (rotation: Float) -> Unit
) = coroutineScope {
    launch {
        awaitMapGesture {
            do {
                val event = awaitPointerEvent()
                if (event.type == PointerEventType.Scroll) {
                    event.changes.forEach { it.consume() }
                    onScroll.invoke(event.changes[0].position, event.changes[0].scrollDelta.y / 300)
                    continue
                }
            } while (this@coroutineScope.isActive && !event.changes.any { it.customIsOutOfBounds(size, extendedTouchPadding) })
        }
    }
    awaitMapGesture {
        //Parameters
        val longPressTimeout = viewConfiguration.longPressTimeoutMillis
        val doubleTapTimeout = viewConfiguration.doubleTapTimeoutMillis
        val touchSlop = viewConfiguration.touchSlop
        var panSlop: Offset
        val zoomScale = 100F

        //Gets first event
        var gestureState by Delegates.observable(GestureState.START_GESTURE) { _, _, newValue ->
            currentGestureFlow?.tryEmit(newValue)
        }
        var previousEvent = awaitPointerEvent()
        var event = previousEvent
        gestureState = GestureState.HOVER
        var firstGestureEvent: PointerEvent? = null

        onHover(event.changes[0].position)
        do {
            when (gestureState) {
                GestureState.HOVER -> {
                    while (this@coroutineScope.isActive && !event.changes.any { it.customIsOutOfBounds(size, extendedTouchPadding) }) {
                        previousEvent = event
                        event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                        val eventChanges = getGestureStateChanges(event, previousEvent)

                        if (eventChanges.contains(GestureChangeState.PRESS) && !event.keyboardModifiers.isCtrlPressed) {
                            gestureState = GestureState.WAITING_UP
                            break
                        }
                        if (eventChanges.any { it == GestureChangeState.PRESS } && event.keyboardModifiers.isCtrlPressed) {
                            gestureState = GestureState.CTRL
                            break
                        }
                        onHover.invoke(event.changes[0].position)
                    }
                }

                GestureState.WAITING_UP -> {
                    var timePassed = 0L
                    panSlop = Offset.Zero
                    while (this@coroutineScope.isActive && !event.changes.any { it.customIsOutOfBounds(size, extendedTouchPadding) }) {
                        try {
                            previousEvent = event
                            event = withTimeout(longPressTimeout - timePassed) {
                                awaitPointerEvent()
                            }
                            timePassed += event.changes[0].uptimeMillis - previousEvent.changes[0].uptimeMillis
                            event.changes.forEach { it.consume() }

                            if (event.type == PointerEventType.Move) {
                                panSlop += event.calculatePan()
                                if (panSlop.getDistance() > touchSlop) {
                                    gestureState = GestureState.DRAG
                                    break
                                }
                            }
                            if (getGestureStateChanges(event, previousEvent).any { it == GestureChangeState.RELEASE }) {
                                gestureState = GestureState.WAITING_DOWN
                                break
                            }
                        } catch (_: PointerEventTimeoutCancellationException) {
                            onLongPress.invoke(event.changes[0].position)
                            gestureState = GestureState.HOVER
                            break
                        }
                    }
                }

                GestureState.WAITING_DOWN -> {
                    var timePassed = 0L
                    panSlop = Offset.Zero
                    while (this@coroutineScope.isActive && !event.changes.any { it.customIsOutOfBounds(size, extendedTouchPadding) }) {
                        try {
                            previousEvent = event
                            event = withTimeout(doubleTapTimeout - timePassed) {
                                awaitPointerEvent()
                            }
                            timePassed += event.changes[0].uptimeMillis - previousEvent.changes[0].uptimeMillis
                            event.changes.forEach { it.consume() }

                            if (event.type == PointerEventType.Move) {
                                panSlop += event.calculatePan()
                                if (panSlop.getDistance() > touchSlop) {
                                    onTap.invoke(event.changes[0].position)
                                    gestureState = GestureState.HOVER
                                    break
                                }
                            }
                            if (getGestureStateChanges(event, previousEvent).any { it == GestureChangeState.PRESS }) {
                                gestureState = GestureState.WAITING_UP_AFTER_TAP
                                break
                            }
                        } catch (_: PointerEventTimeoutCancellationException) {
                            onTap.invoke(event.changes[0].position)
                            gestureState = GestureState.HOVER
                            break
                        }
                    }
                }

                GestureState.WAITING_UP_AFTER_TAP -> {
                    var timePassed = 0L
                    panSlop = Offset.Zero
                    while (this@coroutineScope.isActive && !event.changes.any { it.customIsOutOfBounds(size, extendedTouchPadding) }) {
                        try {
                            previousEvent = event
                            event = withTimeout(doubleTapTimeout - timePassed) {
                                awaitPointerEvent()
                            }
                            timePassed += event.changes[0].uptimeMillis - previousEvent.changes[0].uptimeMillis
                            event.changes.forEach { it.consume() }

                            if (event.type == PointerEventType.Move) {
                                panSlop += event.calculatePan()
                                if (panSlop.getDistance() > touchSlop) {
                                    gestureState = GestureState.TAP_SWIPE
                                    firstGestureEvent = event
                                    break
                                }
                            }
                            if (getGestureStateChanges(event, previousEvent).any { it == GestureChangeState.RELEASE }) {
                                onDoubleTap.invoke(event.changes[0].position)
                                gestureState = GestureState.HOVER
                                break
                            }
                        } catch (_: PointerEventTimeoutCancellationException) {
                            gestureState = GestureState.TAP_LONG_PRESS
                            break
                        }
                    }
                }

                GestureState.DRAG -> {
                    while (this@coroutineScope.isActive && !event.changes.any { it.customIsOutOfBounds(size, extendedTouchPadding) }) {
                        previousEvent = event
                        event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                        val eventChanges = getGestureStateChanges(event, previousEvent)

                        if (eventChanges.any { it == GestureChangeState.CTRL_PRESS }) {
                            gestureState = GestureState.CTRL
                            break
                        }

                        if (eventChanges.any { it == GestureChangeState.RELEASE }) {
                            gestureState = GestureState.HOVER
                            break
                        }

                        onDrag.invoke(event.changes[0].position - previousEvent.changes[0].position)
                    }
                }

                GestureState.CTRL -> {
                    while (this@coroutineScope.isActive && !event.changes.any { it.customIsOutOfBounds(size, extendedTouchPadding) }) {
                        previousEvent = event
                        event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                        val eventChanges = getGestureStateChanges(event, previousEvent)

                        if (eventChanges.any { it == GestureChangeState.CTRL_RELEASE }) {
                            firstGestureEvent = event
                            gestureState = GestureState.DRAG
                            break
                        }

                        if (eventChanges.any { it == GestureChangeState.RELEASE }) {
                            gestureState = GestureState.HOVER
                            break
                        }
                        handleGestureWithCtrl(event, previousEvent, size / 2) { rotationChange ->
                            onCtrlGesture.invoke(rotationChange)
                        }
                    }
                }

                GestureState.TAP_LONG_PRESS -> {
                    while (this@coroutineScope.isActive && !event.changes.any { it.customIsOutOfBounds(size, extendedTouchPadding) }) {
                        previousEvent = event
                        event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                        val eventChanges = getGestureStateChanges(event, previousEvent)

                        if (eventChanges.any { it == GestureChangeState.RELEASE }) {
                            gestureState = GestureState.HOVER
                            break
                        }

                        onTapLongPress.invoke(event.changes[0].position - previousEvent.changes[0].position)
                    }

                    onTapLongPress.invoke(event.changes[0].position - previousEvent.changes[0].position)
                }

                GestureState.TAP_SWIPE -> {
                    while (this@coroutineScope.isActive && !event.changes.any { it.customIsOutOfBounds(size, extendedTouchPadding) }) {
                        previousEvent = event
                        event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                        val eventChanges = getGestureStateChanges(event, previousEvent)

                        if (eventChanges.any { it == GestureChangeState.RELEASE }) {
                            gestureState = GestureState.HOVER
                            break
                        }
                        onTapSwipe.invoke(
                            firstGestureEvent!!.changes[0].position,
                            (event.changes[0].position.y - previousEvent.changes[0].position.y) / zoomScale
                        )
                    }
                }

                else -> continue
            }
        } while (this@coroutineScope.isActive && !event.changes.any { it.customIsOutOfBounds(size, extendedTouchPadding) })
    }
}

//This is necessary because for js method isOutOfBounds doesn't work properly at all
fun PointerInputChange.customIsOutOfBounds(size: IntSize, extendedTouchPadding: Size): Boolean {
    val position = position
    val x = position.x
    val y = position.y
    val minX = -extendedTouchPadding.width
    val maxX = size.width + extendedTouchPadding.width
    val minY = -extendedTouchPadding.height
    val maxY = size.height + extendedTouchPadding.height
    return x <= minX || x >= maxX || y <= minY || y >= maxY
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
    val currentContext = currentCoroutineContext()
    awaitPointerEventScope {
        while (currentContext.isActive) {
            try {
                block()

                awaitForGestureReset()
            } catch (e: CancellationException) {
                if (currentContext.isActive) {
                    // The current gesture was canceled. Wait for all fingers to be "up" before
                    // looping again.
                    awaitForGestureReset()
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
internal actual suspend fun AwaitPointerEventScope.awaitForGestureReset() {
    if (currentEvent.changes.any { it.isOutOfBounds(size, extendedTouchPadding) }) {
        do {
            val events = awaitPointerEvent(PointerEventPass.Final)
        } while (events.changes.any { it.isOutOfBounds(size, extendedTouchPadding) })
    }
}