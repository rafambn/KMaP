package io.github.rafambn.kmap.gestures

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculateCentroid
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs
import kotlin.math.pow

/**
 * [detectMapGestures] detects all kinds of gestures needed for KMaP
 */
internal actual suspend fun PointerInputScope.detectMapGestures(  //Not tested
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
    onFlingRotation: (centroid: Offset, velocity: Float) -> Unit,
    onHover: (Offset) -> Unit,
    onScroll: (mouseOffset: Offset, scrollAmount: Float) -> Unit
) = coroutineScope {

    awaitMapGesture {
        var previousEvent = awaitFirstEvent(requireUnconsumed = false)
        var event = previousEvent
        var gestureState = GestureState.WAITING_UP

        var firstTapEvent: PointerEvent? = null
        val longPressTimeout = viewConfiguration.longPressTimeoutMillis
        val doubleTapTimeout = viewConfiguration.doubleTapTimeoutMillis
        val touchSlop = viewConfiguration.touchSlop
        var timeoutCount = longPressTimeout
        var panSlop = Offset.Zero
        val panVelocityTracker = VelocityTracker()
        val zoomVelocityTracker = VelocityTracker1D(isDataDifferential = false)
        val rotationVelocityTracker = VelocityTracker1D(isDataDifferential = true)

        val flingVelocityThreshold = 100.dp.toPx().pow(2)
        val flingVelocityMaxRange = -1000F..1000F
        val flingVelocityScale = 10F

        val flingZoomThreshold = 0.1.dp.toPx()
        val flingZoomMaxRange = -0.5F..0.5F
        val flingZoomScale = 20F

        val flingRotationThreshold = 2F
        val flingRotationMaxRange = -5F..5F
        val flingRotationScale = 300F

        val tapSwipeScale = 400F

        do {
            //Awaits for a pointer event
            try {
                previousEvent = event

                event = withTimeout(10L) {
                    awaitPointerEvent()
                }

                //If doesn't timeout them checks if there is any changes in the state
                val eventChanges = getGestureStateChanges(event, previousEvent)

                //Here are the cases that leads to an gesture
                if (gestureState == GestureState.WAITING_UP) {
                    if (eventChanges.any { it == GestureChangeState.RELEASE }) {
                        event.changes.forEach { it.consume() }
                        gestureState = GestureState.WAITING_DOWN
                        timeoutCount = doubleTapTimeout
                        continue
                    }
                    if (eventChanges.any { it == GestureChangeState.TWO_PRESS }) {
                        event.changes.forEach { it.consume() }
                        gestureState = GestureState.WAITING_UP_AFTER_TWO_PRESS
                        timeoutCount = doubleTapTimeout
                        continue
                    }
                    if (event.type == PointerEventType.Move) {
                        panSlop += event.calculatePan()
                        if (panSlop.getDistance() > touchSlop) {
                            event.changes.forEach { it.consume() }
                            gestureState = GestureState.DRAG
                            onGestureStart.invoke(gestureState, event.changes[0].position)
                            panSlop = Offset.Zero
                            continue
                        }
                    }
                    timeoutCount -= event.changes[0].uptimeMillis - previousEvent.changes[0].uptimeMillis
                    if (timeoutCount < 0) {
                        onLongPress.invoke(event.changes[0].position)
                        event.changes.forEach { it.consume() }
                        continue
                    }
                    continue
                }

                if (gestureState == GestureState.WAITING_DOWN) {
                    if (eventChanges.any { it == GestureChangeState.PRESS }) {
                        event.changes.forEach { it.consume() }
                        gestureState = GestureState.WAITING_UP_AFTER_TAP
                        timeoutCount = longPressTimeout
                        continue
                    }
                    timeoutCount -= event.changes[0].uptimeMillis - previousEvent.changes[0].uptimeMillis
                    if (timeoutCount < 0) {
                        onTap.invoke(event.changes[0].position)
                        event.changes.forEach { it.consume() }
                        continue
                    }
                    continue
                }

                if (gestureState == GestureState.WAITING_UP_AFTER_TAP) {
                    if (eventChanges.any { it == GestureChangeState.RELEASE }) {
                        onDoubleTap.invoke(event.changes[0].position)
                        event.changes.forEach { it.consume() }
                        continue
                    }
                    if (event.type == PointerEventType.Move) {
                        panSlop += event.calculatePan()
                        if (panSlop.getDistance() > touchSlop) {
                            firstTapEvent = event
                            event.changes.forEach { it.consume() }
                            gestureState = GestureState.TAP_SWIPE
                            onGestureStart.invoke(gestureState, event.changes[0].position)
                            panSlop = Offset.Zero
                            continue
                        }
                    }
                    timeoutCount -= event.changes[0].uptimeMillis - previousEvent.changes[0].uptimeMillis
                    if (timeoutCount < 0) {
                        event.changes.forEach { it.consume() }
                        gestureState = GestureState.TAP_LONG_PRESS
                        onGestureStart.invoke(gestureState, event.changes[0].position)
                        continue
                    }
                    continue
                }

                if (gestureState == GestureState.WAITING_UP_AFTER_TWO_PRESS) {
                    if (event.changes.all { !it.pressed }) {
                        event.changes.forEach { it.consume() }
                        onTwoFingersTap.invoke(event.changes[0].position)
                        break
                    }
                    if (event.type == PointerEventType.Move) {
                        panSlop += event.calculatePan()
                        if (panSlop.getDistance() > touchSlop) {
                            event.changes.forEach { it.consume() }
                            gestureState = GestureState.MOBILE
                            onGestureStart.invoke(gestureState, event.changes[0].position)
                            panSlop = Offset.Zero
                            continue
                        }
                    }
                    timeoutCount -= event.changes[0].uptimeMillis - previousEvent.changes[0].uptimeMillis
                    if (timeoutCount < 0) {
                        event.changes.forEach { it.consume() }
                        gestureState = GestureState.MOBILE
                        onGestureStart.invoke(gestureState, event.changes[0].position)
                        continue
                    }
                    continue
                }

                if (gestureState == GestureState.WAITING_UP_AFTER_TWO_RELEASE) {
                    if (event.changes.all { !it.pressed }) {
                        event.changes.forEach { it.consume() }
                        val velocity = runCatching {
                            panVelocityTracker.calculateVelocity()
                        }.getOrDefault(Velocity.Zero)
                        val velocityCapped = Velocity(
                            (velocity.x / flingVelocityScale).coerceIn(flingVelocityMaxRange),
                            (velocity.y / flingVelocityScale).coerceIn(flingVelocityMaxRange)
                        )
                        val velocitySquared = velocityCapped.x.pow(2) + velocityCapped.y.pow(2)
                        if (velocitySquared > flingVelocityThreshold) {
                            onFling(velocityCapped)
                        }
                        break
                    }
                    if (event.type == PointerEventType.Move) {
                        panSlop += event.calculatePan()
                        if (panSlop.getDistance() > touchSlop) {
                            event.changes.forEach { it.consume() }
                            gestureState = GestureState.DRAG
                            onGestureStart.invoke(gestureState, event.changes[0].position)
                            panSlop = Offset.Zero
                            continue
                        }
                    }
                    timeoutCount -= event.changes[0].uptimeMillis - previousEvent.changes[0].uptimeMillis
                    if (timeoutCount < 0) {
                        event.changes.forEach { it.consume() }
                        gestureState = GestureState.DRAG
                        onGestureStart.invoke(gestureState, event.changes[0].position)
                        continue
                    }
                    continue
                }

                //Here are the cases that exits of an gesture to another
                if (gestureState == GestureState.TAP_LONG_PRESS && eventChanges.any { it == GestureChangeState.RELEASE }) {
                    onGestureEnd.invoke(gestureState)
                    event.changes.forEach { it.consume() }
                    break
                }

                if (gestureState == GestureState.TAP_SWIPE && eventChanges.any { it == GestureChangeState.RELEASE }) {
                    onGestureEnd.invoke(gestureState)
                    val velocity = runCatching {
                        zoomVelocityTracker.calculateVelocity()
                    }.getOrDefault(0F)
                    val zoomCapped = (velocity / (flingZoomScale * tapSwipeScale)).coerceIn(flingZoomMaxRange)
                    if (abs(zoomCapped) > flingZoomThreshold) {
                        onFlingZoom(event.changes[0].position, zoomCapped)
                    }
                    event.changes.forEach { it.consume() }
                    break
                }

                if (gestureState == GestureState.DRAG && eventChanges.any { it == GestureChangeState.RELEASE }) {
                    onGestureEnd.invoke(gestureState)
                    val velocity = runCatching {
                        panVelocityTracker.calculateVelocity()
                    }.getOrDefault(Velocity.Zero)
                    val velocityCapped = Velocity(
                        (velocity.x / flingVelocityScale).coerceIn(flingVelocityMaxRange),
                        (velocity.y / flingVelocityScale).coerceIn(flingVelocityMaxRange)
                    )
                    val velocitySquared = velocityCapped.x.pow(2) + velocityCapped.y.pow(2)
                    if (velocitySquared > flingVelocityThreshold) {
                        onFling(velocityCapped)
                    }
                    event.changes.forEach { it.consume() }
                    break
                }

                if (gestureState == GestureState.DRAG && eventChanges.any { it == GestureChangeState.TWO_PRESS }) {
                    onGestureEnd.invoke(gestureState)
                    event.changes.forEach { it.consume() }
                    gestureState = GestureState.MOBILE
                    onGestureStart.invoke(gestureState, event.changes[0].position)
                    continue
                }

                if (gestureState == GestureState.MOBILE && eventChanges.any { it == GestureChangeState.TWO_RELEASE }) {
                    onGestureEnd.invoke(gestureState)
//                   val zoomVelocity = runCatching {
//                        -zoomVelocityTracker.calculateVelocity()
//                    }.getOrDefault(0F)
//                    val zoomCapped = (zoomVelocity / flingZoomScale).coerceIn(flingZoomMaxRange)
//                    if (abs(zoomCapped) > flingZoomThreshold) {
//                        onFlingZoom(event.changes[0].position, zoomCapped)
//                    }
                    val rotationVelocity = runCatching {
                        rotationVelocityTracker.calculateVelocity()
                    }.getOrDefault(0F)
                    val rotationCapped = (rotationVelocity / flingRotationScale).coerceIn(flingRotationMaxRange)
                    if (abs(rotationCapped) > flingRotationThreshold) {
                        onFlingRotation(event.changes[0].position, rotationCapped)
                    }
                    println("$rotationVelocity ----- $rotationCapped ---- ${abs(rotationCapped)} ----- $flingRotationThreshold")
                    event.changes.forEach { it.consume() }
                    gestureState = GestureState.WAITING_UP_AFTER_TWO_RELEASE
                    timeoutCount = doubleTapTimeout
                    panSlop = Offset.Zero
                    continue
                }

                //Finally, here are the gestures
                if (gestureState == GestureState.MOBILE) {
                    val zoomChange = event.calculateZoom()
                    val rotationChange = event.calculateRotation()
                    val panChange = event.calculatePan()
                    val centroid = event.calculateCentroid()
                    panVelocityTracker.addPosition(event.changes[0].uptimeMillis, event.changes[0].position)
                    zoomVelocityTracker.addDataPoint(event.changes[0].uptimeMillis, zoomChange - 1)
                    rotationVelocityTracker.addDataPoint(event.changes[0].uptimeMillis, rotationChange)
                    if (centroid != Offset.Unspecified)
                        onGesture(centroid, panChange, zoomChange - 1, rotationChange)
                    event.changes.forEach { it.consume() }
                    continue
                }

                if (gestureState == GestureState.DRAG) {
                    onDrag.invoke(event.changes[0].position - previousEvent.changes[0].position)
                    panVelocityTracker.addPosition(event.changes[0].uptimeMillis, event.changes[0].position)
                    event.changes.forEach { it.consume() }
                    continue
                }

                if (gestureState == GestureState.TAP_LONG_PRESS) {
                    onTapLongPress.invoke(event.changes[0].position - previousEvent.changes[0].position)
                    event.changes.forEach { it.consume() }
                    continue
                }

                if (gestureState == GestureState.TAP_SWIPE) {
                    onTapSwipe.invoke(firstTapEvent!!.changes[0].position, (event.changes[0].position.y - previousEvent.changes[0].position.y) / tapSwipeScale)
                    zoomVelocityTracker.addDataPoint(event.changes[0].uptimeMillis, event.changes[0].position.y)
                    event.changes.forEach { it.consume() }
                    continue
                }

            } catch (_: PointerEventTimeoutCancellationException) {
                //It case of a timeout them just check the case where timeout is necessary
                timeoutCount -= 10L

                if (gestureState == GestureState.WAITING_UP) {
                    if (timeoutCount < 0) {
                        onLongPress.invoke(event.changes[0].position)
                        event.changes.forEach { it.consume() }
                        break
                    }
                }

                if (gestureState == GestureState.WAITING_DOWN) {
                    if (timeoutCount < 0) {
                        onTap.invoke(event.changes[0].position)
                        event.changes.forEach { it.consume() }
                        break
                    }
                }
                if (gestureState == GestureState.WAITING_UP_AFTER_TAP) {
                    if (timeoutCount < 0) {
                        event.changes.forEach { it.consume() }
                        gestureState = GestureState.TAP_LONG_PRESS
                        onGestureStart.invoke(gestureState, event.changes[0].position)
                        continue
                    }
                }
                if (gestureState == GestureState.WAITING_UP_AFTER_TWO_PRESS) {
                    if (timeoutCount < 0) {
                        gestureState = GestureState.MOBILE
                        onGestureStart.invoke(gestureState, event.changes[0].position)
                        continue
                    }
                }
                if (gestureState == GestureState.WAITING_UP_AFTER_TWO_RELEASE) {
                    if (timeoutCount < 0) {
                        event.changes.forEach { it.consume() }
                        gestureState = GestureState.DRAG
                        onGestureStart.invoke(gestureState, event.changes[0].position)
                        continue
                    }
                }
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
