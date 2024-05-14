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
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.isOutOfBounds
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.VelocityTracker1D
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlin.coroutines.cancellation.CancellationException

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
    onFlingRotation: (centroid: Offset?, velocity: Float) -> Unit,
    onHover: (Offset) -> Unit,
    onScroll: (mouseOffset: Offset, scrollAmount: Float) -> Unit,
    onCtrlGesture: (rotation: Float) -> Unit
) = coroutineScope {
    awaitMapGesture {
        //Parameters
        val longPressTimeout = viewConfiguration.longPressTimeoutMillis
        val doubleTapTimeout = viewConfiguration.doubleTapTimeoutMillis
        val touchSlop = viewConfiguration.touchSlop
        var panSlop: Offset

        val panVelocityTracker = VelocityTracker()
        val zoomVelocityTracker = VelocityTracker1D(isDataDifferential = true)
        val rotationVelocityTracker = VelocityTracker1D(isDataDifferential = true)

        val flingVelocityMaxRange = 500F
        val flingVelocityScale = 2F

        val flingZoomMaxRange = 3000F
        val flingZoomScale = 5F
        val zoomScale = 300F

        val flingRotationMaxRange = 1800F
        val flingRotationScale = 10F

        //Gets first event
        var previousEvent = awaitFirstEvent()
        var event = previousEvent
        var gestureState = GestureState.WAITING_UP
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
                                    onGestureStart.invoke(gestureState, event.changes[0].position)
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
                                    onGestureStart.invoke(gestureState, event.changes[0].position)
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
                            onGestureStart.invoke(gestureState, event.changes[0].position)
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
                                    onGestureStart.invoke(gestureState, event.changes[0].position)
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
                            onGestureStart.invoke(gestureState, event.changes[0].position)
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
                                    onGestureStart.invoke(gestureState, event.changes[0].position)
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
                    panVelocityTracker.resetTracking()
                    while (this@coroutineScope.isActive && !event.changes.any { it.isOutOfBounds(size, extendedTouchPadding) }) {
                        previousEvent = event
                        event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                        val eventChanges = getGestureStateChanges(event, previousEvent)

                        if (eventChanges.any { it == GestureChangeState.TWO_PRESS }) {
                            onGestureEnd.invoke(gestureState)
                            gestureState = GestureState.MOBILE
                            onGestureStart.invoke(gestureState, event.changes[0].position)
                            break
                        }

                        if (eventChanges.any { it == GestureChangeState.RELEASE }) {
                            onGestureEnd.invoke(gestureState)
                            panVelocityTracker.addPosition(event.changes[0].uptimeMillis, event.changes[0].position)
                            onFling(
                                panVelocityTracker.calculateVelocity(
                                    Velocity(
                                        flingVelocityMaxRange,
                                        flingVelocityMaxRange
                                    )
                                ) / flingVelocityScale
                            )
                            gestureState = GestureState.FINISH_GESTURE
                            break
                        }

                        onDrag.invoke(event.changes[0].position - previousEvent.changes[0].position)
                        panVelocityTracker.addPosition(event.changes[0].uptimeMillis, event.changes[0].position)
                    }
                }

                GestureState.TAP_LONG_PRESS -> {
                    while (this@coroutineScope.isActive && !event.changes.any { it.isOutOfBounds(size, extendedTouchPadding) }) {
                        previousEvent = event
                        event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                        val eventChanges = getGestureStateChanges(event, previousEvent)

                        if (eventChanges.any { it == GestureChangeState.RELEASE }) {
                            onGestureEnd.invoke(gestureState)
                            gestureState = GestureState.FINISH_GESTURE
                            break
                        }

                        if (eventChanges.any { it == GestureChangeState.TWO_PRESS }) {
                            onGestureEnd.invoke(gestureState)
                            gestureState = GestureState.MOBILE
                            onGestureStart.invoke(gestureState, event.changes[0].position)
                            break
                        }

                        onTapLongPress.invoke(event.changes[0].position - previousEvent.changes[0].position)
                    }
                }

                GestureState.TAP_SWIPE -> {
                    zoomVelocityTracker.resetTracking()
                    while (this@coroutineScope.isActive && !event.changes.any { it.isOutOfBounds(size, extendedTouchPadding) }) {
                        previousEvent = event
                        event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                        val eventChanges = getGestureStateChanges(event, previousEvent)

                        if (eventChanges.any { it == GestureChangeState.RELEASE }) {
                            onGestureEnd.invoke(gestureState)
                            zoomVelocityTracker.addDataPoint(
                                event.changes[0].uptimeMillis,
                                event.changes[0].position.y - previousEvent.changes[0].position.y
                            )
                            onFlingZoom(
                                firstGestureEvent!!.changes[0].position,
                                zoomVelocityTracker.calculateVelocity(flingZoomMaxRange) / (flingZoomScale * zoomScale)
                            )
                            gestureState = GestureState.FINISH_GESTURE
                            break
                        }

                        if (eventChanges.any { it == GestureChangeState.TWO_PRESS }) {
                            onGestureEnd.invoke(gestureState)
                            gestureState = GestureState.MOBILE
                            onGestureStart.invoke(gestureState, event.changes[0].position)
                            break
                        }

                        onTapSwipe.invoke(
                            firstGestureEvent!!.changes[0].position,
                            (event.changes[0].position.y - previousEvent.changes[0].position.y) / zoomScale
                        )
                        zoomVelocityTracker.addDataPoint(
                            event.changes[0].uptimeMillis,
                            event.changes[0].position.y - previousEvent.changes[0].position.y
                        )
                    }
                }
                GestureState.MOBILE -> {
                    panVelocityTracker.resetTracking()
                    zoomVelocityTracker.resetTracking()
                    rotationVelocityTracker.resetTracking()
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
                        panVelocityTracker.addPosition(event.changes[0].uptimeMillis, event.changes[0].position)
                        zoomVelocityTracker.addDataPoint(event.changes[0].uptimeMillis, zoomChange)
                        rotationVelocityTracker.addDataPoint(event.changes[0].uptimeMillis, rotationChange)
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
                                onFling(
                                    panVelocityTracker.calculateVelocity(
                                        Velocity(
                                            flingVelocityMaxRange,
                                            flingVelocityMaxRange
                                        )
                                    ) / flingVelocityScale
                                )
                                println(zoomVelocityTracker.calculateVelocity())
                                onFlingZoom(
                                    event.changes[0].position,
                                    zoomVelocityTracker.calculateVelocity(flingZoomMaxRange) / (flingZoomScale * zoomScale)
                                )
                                onFlingRotation(
                                    event.changes[0].position,
                                    rotationVelocityTracker.calculateVelocity(flingRotationMaxRange) / flingRotationScale
                                )
                                gestureState = GestureState.FINISH_GESTURE
                                break
                            }
                            if (event.type == PointerEventType.Move) {
                                panSlop += event.calculatePan()
                                if (panSlop.getDistance() > (touchSlop * 2)) {
                                    onGestureEnd.invoke(gestureState)
                                    gestureState = GestureState.DRAG
                                    onGestureStart.invoke(gestureState, event.changes[0].position)
                                    break
                                }
                            }
                            if (getGestureStateChanges(event, previousEvent).any { it == GestureChangeState.TWO_PRESS }) {
                                gestureState = GestureState.MOBILE
                                break
                            }
                        } catch (_: PointerEventTimeoutCancellationException) {
                            onGestureEnd.invoke(gestureState)
                            gestureState = GestureState.DRAG
                            onGestureStart.invoke(gestureState, event.changes[0].position)
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
