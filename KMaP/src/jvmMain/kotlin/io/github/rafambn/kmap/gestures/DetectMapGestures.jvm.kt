package io.github.rafambn.kmap.gestures

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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/**
 * [detectMapGestures] detects all kinds of gestures needed for KMaP
 */
internal actual suspend fun PointerInputScope.detectMapGestures(
    onTap: (Offset) -> Unit,
    onDoubleTap: (Offset) -> Unit,
    onTwoFingersTap: (Offset) -> Unit, //There isn't a call for this method in Desktop
    onLongPress: (Offset) -> Unit,
    onTapLongPress: (Offset) -> Unit,
    onTapSwipe: (centroid: Offset, zoom: Float) -> Unit,
    onGesture: (centroid: Offset, pan: Offset, zoom: Float, rotation: Float) -> Unit,
    onDrag: (dragAmount: Offset) -> Unit,
    onGestureStart: (gestureType: GestureState, offset: Offset) -> Unit,
    onGestureEnd: (gestureType: GestureState) -> Unit,
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
                    onScroll.invoke(event.changes[0].position, event.changes[0].scrollDelta.y / 3)
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
        var previousEvent = awaitPointerEvent()
        var event = previousEvent
        var gestureState = GestureState.HOVER
        var firstGestureEvent: PointerEvent? = null

        onGestureStart.invoke(GestureState.HOVER, event.changes[0].position)
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
                            onGestureEnd.invoke(gestureState)
                            gestureState = GestureState.WAITING_UP
                            break
                        }
                        if (eventChanges.any { it == GestureChangeState.PRESS } && event.keyboardModifiers.isCtrlPressed) {
                            onGestureEnd.invoke(gestureState)
                            gestureState = GestureState.CTRL
                            onGestureStart.invoke(gestureState, event.changes[0].position)
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
                                    onGestureStart.invoke(gestureState, event.changes[0].position)
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
                            onGestureStart.invoke(gestureState, event.changes[0].position)
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
                                    onGestureStart.invoke(gestureState, event.changes[0].position)
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
                            onGestureStart.invoke(gestureState, event.changes[0].position)
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
                                    onGestureStart.invoke(gestureState, event.changes[0].position)
                                    break
                                }
                            }
                            if (getGestureStateChanges(event, previousEvent).any { it == GestureChangeState.RELEASE }) {
                                onDoubleTap.invoke(event.changes[0].position)
                                gestureState = GestureState.HOVER
                                onGestureStart.invoke(gestureState, event.changes[0].position)
                                break
                            }
                        } catch (_: PointerEventTimeoutCancellationException) {
                            gestureState = GestureState.TAP_LONG_PRESS
                            onGestureStart.invoke(gestureState, event.changes[0].position)
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
                            onGestureEnd.invoke(gestureState)
                            gestureState = GestureState.CTRL
                            onGestureStart.invoke(gestureState, event.changes[0].position)
                            break
                        }

                        if (eventChanges.any { it == GestureChangeState.RELEASE }) {
                            onGestureEnd.invoke(gestureState)
                            gestureState = GestureState.HOVER
                            onGestureStart.invoke(gestureState, event.changes[0].position)
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
                            onGestureEnd.invoke(gestureState)
                            firstGestureEvent = event
                            gestureState = GestureState.DRAG
                            onGestureStart.invoke(gestureState, event.changes[0].position)
                            break
                        }

                        if (eventChanges.any { it == GestureChangeState.RELEASE }) {
                            onGestureEnd.invoke(gestureState)
                            gestureState = GestureState.HOVER
                            onGestureStart.invoke(gestureState, event.changes[0].position)
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
                            onGestureEnd.invoke(gestureState)
                            gestureState = GestureState.HOVER
                            onGestureStart.invoke(gestureState, event.changes[0].position)
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
                            onGestureEnd.invoke(gestureState)
                            gestureState = GestureState.HOVER
                            onGestureStart.invoke(gestureState, event.changes[0].position)
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
        onGestureEnd.invoke(gestureState)
    }
}

//This is necessary because for jvm method isOutOfBounds doesn't work properly for bottom and right edges. v1.6.0
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
                    awaitForGestureReset()
                } else {
                    throw e
                }
            }
        }
    }
}

internal actual suspend fun AwaitPointerEventScope.awaitForGestureReset() {
    if (currentEvent.changes.any { it.isOutOfBounds(size, extendedTouchPadding) }) {
        do {
            val events = awaitPointerEvent(PointerEventPass.Final)
        } while (events.changes.any { it.isOutOfBounds(size, extendedTouchPadding) })
    }
}