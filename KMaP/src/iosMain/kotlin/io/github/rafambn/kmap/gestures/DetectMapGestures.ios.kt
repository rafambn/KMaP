package io.github.rafambn.kmap.gestures

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.isOutOfBounds
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
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

    onTwoFingersTap: (Offset) -> Unit,
    onGesture: (centroid: Offset, pan: Offset, zoom: Float, rotation: Float) -> Unit,

    onHover: (Offset) -> Unit, //There isn't a call for this method in Mobile
    onScroll: (mouseOffset: Offset, scrollAmount: Float) -> Unit, //There isn't a call for this method in Mobile
    onCtrlGesture: (rotation: Float) -> Unit //There isn't a call for this method in Mobile
) = coroutineScope {
    awaitMapGesture {
        //Parameters
        val longPressTimeout = viewConfiguration.longPressTimeoutMillis
        val doubleTapTimeout = viewConfiguration.doubleTapTimeoutMillis
        val touchSlop = viewConfiguration.touchSlop
        var panSlop: Offset
        val zoomScale = 300F

        //Gets first event
        var gestureState by Delegates.observable(GestureState.START_GESTURE) { _, _, newValue ->
            currentGestureFlow?.tryEmit(newValue)
        }
        var previousEvent = awaitFirstEvent()
        var event = previousEvent
        gestureState = GestureState.WAITING_UP
        var firstGestureEvent: PointerEvent? = null

        do {
            when (gestureState) {
                GestureState.FINISH_GESTURE -> break

                GestureState.WAITING_UP -> {
                    var timePassed = 0L
                    panSlop = Offset.Zero
                    while (this@coroutineScope.isActive && !event.changes.any { it.isOutOfBounds(size, extendedTouchPadding) }) {
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
                            if (getGestureStateChanges(event, previousEvent).any { it == GestureChangeState.TWO_PRESS }) {
                                gestureState = GestureState.WAITING_UP_AFTER_TWO_PRESS
                                break
                            }
                        } catch (_: PointerEventTimeoutCancellationException) {
                            onLongPress.invoke(event.changes[0].position)
                            gestureState = GestureState.FINISH_GESTURE
                            break
                        }
                    }
                }

                GestureState.WAITING_DOWN -> {
                    var timePassed = 0L
                    while (this@coroutineScope.isActive && !event.changes.any { it.isOutOfBounds(size, extendedTouchPadding) }) {
                        try {
                            previousEvent = event
                            event = withTimeout(doubleTapTimeout - timePassed) {
                                awaitPointerEvent()
                            }
                            timePassed += event.changes[0].uptimeMillis - previousEvent.changes[0].uptimeMillis
                            event.changes.forEach { it.consume() }

                            if (getGestureStateChanges(event, previousEvent).any { it == GestureChangeState.PRESS }) {
                                gestureState = GestureState.WAITING_UP_AFTER_TAP
                                break
                            }
                        } catch (_: PointerEventTimeoutCancellationException) {
                            onTap.invoke(event.changes[0].position)
                            gestureState = GestureState.FINISH_GESTURE
                            break
                        }
                    }
                }

                GestureState.WAITING_UP_AFTER_TAP -> {
                    var timePassed = 0L
                    panSlop = Offset.Zero
                    while (this@coroutineScope.isActive && !event.changes.any { it.isOutOfBounds(size, extendedTouchPadding) }) {
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
                                gestureState = GestureState.FINISH_GESTURE
                                break
                            }
                        } catch (_: PointerEventTimeoutCancellationException) {
                            gestureState = GestureState.TAP_LONG_PRESS
                            break
                        }
                    }
                }

                GestureState.WAITING_UP_AFTER_TWO_PRESS -> {
                    var timePassed = 0L
                    panSlop = Offset.Zero
                    while (this@coroutineScope.isActive && !event.changes.any { it.isOutOfBounds(size, extendedTouchPadding) }) {
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
                                    gestureState = GestureState.MOBILE
                                    break
                                }
                            }
                            if (getGestureStateChanges(event, previousEvent).any { it == GestureChangeState.TWO_RELEASE }) {
                                if (getGestureStateChanges(event, previousEvent).any { it == GestureChangeState.RELEASE }) {
                                    onTwoFingersTap.invoke(event.changes[0].position)
                                    gestureState = GestureState.FINISH_GESTURE
                                } else {
                                    gestureState = GestureState.WAITING_UP_AFTER_TWO_RELEASE
                                }
                                break
                            }
                        } catch (_: PointerEventTimeoutCancellationException) {
                            gestureState = GestureState.MOBILE
                            break
                        }
                    }
                }

                GestureState.WAITING_UP_AFTER_TWO_RELEASE -> {
                    var timePassed = 0L
                    panSlop = Offset.Zero
                    while (this@coroutineScope.isActive && !event.changes.any { it.isOutOfBounds(size, extendedTouchPadding) }) {
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
                                    gestureState = GestureState.DRAG
                                    break
                                }
                            }
                            if (getGestureStateChanges(event, previousEvent).any { it == GestureChangeState.RELEASE }) {
                                onTwoFingersTap.invoke(event.changes[0].position)
                                gestureState = GestureState.FINISH_GESTURE
                                break
                            }
                        } catch (_: PointerEventTimeoutCancellationException) {
                            onLongPress.invoke(event.changes[0].position)
                            gestureState = GestureState.FINISH_GESTURE
                            break
                        }
                    }
                }

                GestureState.DRAG -> {
                    while (this@coroutineScope.isActive && !event.changes.any { it.isOutOfBounds(size, extendedTouchPadding) }) {
                        previousEvent = event
                        event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                        val eventChanges = getGestureStateChanges(event, previousEvent)

                        if (eventChanges.any { it == GestureChangeState.TWO_PRESS }) {
                            gestureState = GestureState.MOBILE
                            break
                        }

                        if (eventChanges.any { it == GestureChangeState.RELEASE }) {
                            gestureState = GestureState.FINISH_GESTURE
                            break
                        }

                        onDrag.invoke(event.changes[0].position - previousEvent.changes[0].position)
                    }
                }

                GestureState.TAP_LONG_PRESS -> {
                    while (this@coroutineScope.isActive && !event.changes.any { it.isOutOfBounds(size, extendedTouchPadding) }) {
                        previousEvent = event
                        event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                        val eventChanges = getGestureStateChanges(event, previousEvent)

                        if (eventChanges.any { it == GestureChangeState.RELEASE }) {
                            gestureState = GestureState.FINISH_GESTURE
                            break
                        }

                        if (eventChanges.any { it == GestureChangeState.TWO_PRESS }) {
                            gestureState = GestureState.MOBILE
                            break
                        }

                        onTapLongPress.invoke(event.changes[0].position - previousEvent.changes[0].position)
                    }
                }

                GestureState.TAP_SWIPE -> {
                    while (this@coroutineScope.isActive && !event.changes.any { it.isOutOfBounds(size, extendedTouchPadding) }) {
                        previousEvent = event
                        event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                        val eventChanges = getGestureStateChanges(event, previousEvent)

                        if (eventChanges.any { it == GestureChangeState.RELEASE }) {
                            gestureState = GestureState.FINISH_GESTURE
                            break
                        }

                        if (eventChanges.any { it == GestureChangeState.TWO_PRESS }) {
                            gestureState = GestureState.MOBILE
                            break
                        }

                        onTapSwipe.invoke(
                            firstGestureEvent!!.changes[0].position,
                            (event.changes[0].position.y - previousEvent.changes[0].position.y) / zoomScale
                        )
                    }
                }
                GestureState.MOBILE -> {
                    while (this@coroutineScope.isActive && !event.changes.any { it.isOutOfBounds(size, extendedTouchPadding) }) {
                        previousEvent = event
                        event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }

                        if (getGestureStateChanges(event, previousEvent).any { it == GestureChangeState.TWO_RELEASE }) {
                            gestureState = GestureState.WAITING_UP_AFTER_MOBILE_RELEASE
                            break
                        }
                        val eventZoomCentroid = event.calculateCentroidSize()
                        val previousEventZoomCentroid = previousEvent.calculateCentroidSize()
                        var zoomChange = eventZoomCentroid - previousEventZoomCentroid
                        if (eventZoomCentroid == 0f || previousEventZoomCentroid == 0f)
                            zoomChange = 0.0F

                        val rotationChange = event.calculateRotation()

                        val panChange = event.calculatePan()
                        val centroid = event.calculateCentroid()
                        if (centroid != Offset.Unspecified)
                            onGesture(centroid, panChange, zoomChange / zoomScale, rotationChange)
                    }
                }

                GestureState.WAITING_UP_AFTER_MOBILE_RELEASE -> {
                    var timePassed = 0L
                    panSlop = Offset.Zero
                    while (this@coroutineScope.isActive && !event.changes.any { it.isOutOfBounds(size, extendedTouchPadding) }) {
                        try {
                            previousEvent = event
                            event = withTimeout(doubleTapTimeout - timePassed) {
                                awaitPointerEvent()
                            }
                            timePassed += event.changes[0].uptimeMillis - previousEvent.changes[0].uptimeMillis
                            event.changes.forEach { it.consume() }

                            if (event.changes.all { !it.pressed }) {
                                gestureState = GestureState.FINISH_GESTURE
                                break
                            }
                            if (event.type == PointerEventType.Move) {
                                panSlop += event.calculatePan()
                                if (panSlop.getDistance() > (touchSlop * 2)) {
                                    gestureState = GestureState.DRAG
                                    break
                                }
                            }
                            if (getGestureStateChanges(event, previousEvent).any { it == GestureChangeState.TWO_PRESS }) {
                                gestureState = GestureState.MOBILE
                                break
                            }
                        } catch (_: PointerEventTimeoutCancellationException) {
                            gestureState = GestureState.DRAG
                            break
                        }
                    }
                }

                else -> continue
            }
        } while (this@coroutineScope.isActive && !event.changes.any { it.isOutOfBounds(size, extendedTouchPadding) })
    }
}

suspend fun AwaitPointerEventScope.awaitFirstEvent(
    requireUnconsumed: Boolean = true,
    pass: PointerEventPass = PointerEventPass.Main,
): PointerEvent {
    var event: PointerEvent
    do {
        event = awaitPointerEvent(pass)
    } while (
        !event.changes.all {
            if (requireUnconsumed) it.changedToDown() else it.changedToDownIgnoreConsumed()
        }
    )
    return event
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
                    awaitForGestureReset()
                } else {
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
    if (!currentEvent.changes.any { it.pressed }) {
        do {
            val events = awaitPointerEvent(PointerEventPass.Final)
        } while (events.changes.any { it.pressed })
    }
}
