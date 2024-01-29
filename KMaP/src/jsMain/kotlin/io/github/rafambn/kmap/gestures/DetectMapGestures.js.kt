package io.github.rafambn.kmap.gestures

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isOutOfBounds
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.pow

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
    onFling: (velocity: Velocity) -> Unit,
    onFlingZoom: (centroid: Offset, velocity: Float) -> Unit,
    onFlingRotation: (centroid: Offset, velocity: Float) -> Unit,
    onHover: (Offset) -> Unit,
    onScroll: (mouseOffset: Offset, scrollAmount: Float) -> Unit
) = coroutineScope {

    awaitMapGesture {
        var previousEvent = awaitPointerEvent()
        var event = previousEvent
        var gestureState = io.github.rafambn.kmap.gestures.GestureState.HOVER

        var firstCtrlEvent: androidx.compose.ui.input.pointer.PointerEvent? = null
        var firstTapEvent: androidx.compose.ui.input.pointer.PointerEvent? = null
        val longPressTimeout = viewConfiguration.longPressTimeoutMillis
        val doubleTapTimeout = viewConfiguration.doubleTapTimeoutMillis
        val touchSlop = viewConfiguration.touchSlop
        var timeoutCount = 0L
        var panSlop = androidx.compose.ui.geometry.Offset.Zero
        val panVelocityTracker = androidx.compose.ui.input.pointer.util.VelocityTracker()
        val zoomVelocityTracker = androidx.compose.ui.input.pointer.util.VelocityTracker1D(isDataDifferential = false)
        val rotationVelocityTracker = androidx.compose.ui.input.pointer.util.VelocityTracker1D(isDataDifferential = true)

        val flingVelocityThreshold = 100.dp.toPx().pow(2)
        val flingVelocityMaxRange = -300F..300F
        val flingVelocityScale = 10F

        val flingZoomThreshold = 0.1.dp.toPx()
        val flingZoomMaxRange = -0.5F..0.5F
        val flingZoomScale = 50F

        val flingRotationThreshold = 2F
        val flingRotationMaxRange = -7.5F..7.5F
        val flingRotationScale = 100F

        val tapSwipeScale = 100F

        onGestureStart.invoke(io.github.rafambn.kmap.gestures.GestureState.HOVER, event.changes[0].position)
        do {
            //Awaits for a pointer event
            try {
                previousEvent = event

                event = withTimeout(10L) {
                    awaitPointerEvent()
                }

                if (event.type == androidx.compose.ui.input.pointer.PointerEventType.Scroll) {
                    event.changes.forEach { it.consume() }
                    onScroll.invoke(event.changes[0].position, event.changes[0].scrollDelta.y)
                    continue
                }

                //If doesn't timeout them checks if there is any changes in the state
                val eventChanges = io.github.rafambn.kmap.gestures.getGestureStateChanges(event, previousEvent)

                //Here are the cases that leads to an gesture

                if (eventChanges.any { it == io.github.rafambn.kmap.gestures.GestureChangeState.PRESS } && !event.keyboardModifiers.isCtrlPressed && gestureState == io.github.rafambn.kmap.gestures.GestureState.HOVER) {
                    onGestureEnd.invoke(gestureState)
                    event.changes.forEach { it.consume() }
                    gestureState = io.github.rafambn.kmap.gestures.GestureState.WAITING_UP
                    timeoutCount = longPressTimeout
                    continue
                }

                if (gestureState == io.github.rafambn.kmap.gestures.GestureState.WAITING_UP) {
                    if (eventChanges.any { it == io.github.rafambn.kmap.gestures.GestureChangeState.RELEASE }) {
                        event.changes.forEach { it.consume() }
                        gestureState = io.github.rafambn.kmap.gestures.GestureState.WAITING_DOWN
                        timeoutCount = doubleTapTimeout
                        continue
                    }
                    if (event.type == androidx.compose.ui.input.pointer.PointerEventType.Move) {
                        panSlop += event.calculatePan()
                        if (panSlop.getDistance() > touchSlop) {
                            event.changes.forEach { it.consume() }
                            gestureState = io.github.rafambn.kmap.gestures.GestureState.DRAG
                            onGestureStart.invoke(gestureState, event.changes[0].position)
                            panSlop = androidx.compose.ui.geometry.Offset.Zero
                            continue
                        }
                    }
                    timeoutCount -= event.changes[0].uptimeMillis - previousEvent.changes[0].uptimeMillis
                    if (timeoutCount < 0) {
                        onGestureEnd.invoke(gestureState)
                        onLongPress.invoke(event.changes[0].position)
                        event.changes.forEach { it.consume() }
                        gestureState = io.github.rafambn.kmap.gestures.GestureState.HOVER
                        onGestureStart.invoke(gestureState, event.changes[0].position)
                        continue
                    }
                    continue
                }

                if (gestureState == io.github.rafambn.kmap.gestures.GestureState.WAITING_DOWN) {
                    if (eventChanges.any { it == io.github.rafambn.kmap.gestures.GestureChangeState.PRESS }) {
                        event.changes.forEach { it.consume() }
                        gestureState = io.github.rafambn.kmap.gestures.GestureState.WAITING_UP_AFTER_TAP
                        timeoutCount = longPressTimeout
                        continue
                    }
                    timeoutCount -= event.changes[0].uptimeMillis - previousEvent.changes[0].uptimeMillis
                    if (timeoutCount < 0) {
                        onGestureEnd.invoke(gestureState)
                        onTap.invoke(event.changes[0].position)
                        event.changes.forEach { it.consume() }
                        gestureState = io.github.rafambn.kmap.gestures.GestureState.HOVER
                        onGestureStart.invoke(gestureState, event.changes[0].position)
                        continue
                    }
                    continue
                }

                if (gestureState == io.github.rafambn.kmap.gestures.GestureState.WAITING_UP_AFTER_TAP) {
                    if (eventChanges.any { it == io.github.rafambn.kmap.gestures.GestureChangeState.RELEASE }) {
                        onDoubleTap.invoke(event.changes[0].position)
                        event.changes.forEach { it.consume() }
                        gestureState = io.github.rafambn.kmap.gestures.GestureState.HOVER
                        onGestureStart.invoke(gestureState, event.changes[0].position)
                        continue
                    }
                    if (event.type == androidx.compose.ui.input.pointer.PointerEventType.Move) {
                        panSlop += event.calculatePan()
                        if (panSlop.getDistance() > touchSlop) {
                            firstTapEvent = event
                            event.changes.forEach { it.consume() }
                            gestureState = io.github.rafambn.kmap.gestures.GestureState.TAP_SWIPE
                            onGestureStart.invoke(gestureState, event.changes[0].position)
                            panSlop = androidx.compose.ui.geometry.Offset.Zero
                            continue
                        }
                    }
                    timeoutCount -= event.changes[0].uptimeMillis - previousEvent.changes[0].uptimeMillis
                    if (timeoutCount < 0) {
                        event.changes.forEach { it.consume() }
                        gestureState = io.github.rafambn.kmap.gestures.GestureState.TAP_LONG_PRESS
                        onGestureStart.invoke(gestureState, event.changes[0].position)
                        continue
                    }
                    continue
                }

                if (eventChanges.any { it == io.github.rafambn.kmap.gestures.GestureChangeState.PRESS } && event.keyboardModifiers.isCtrlPressed && gestureState == io.github.rafambn.kmap.gestures.GestureState.HOVER) {
                    onGestureEnd.invoke(gestureState)
                    firstCtrlEvent = event
                    event.changes.forEach { it.consume() }
                    gestureState = io.github.rafambn.kmap.gestures.GestureState.CTRL
                    onGestureStart.invoke(gestureState, event.changes[0].position)
                    continue
                }

                if (eventChanges.any { it == io.github.rafambn.kmap.gestures.GestureChangeState.CTRL_PRESS } && gestureState == io.github.rafambn.kmap.gestures.GestureState.DRAG) {
                    onGestureEnd.invoke(gestureState)
                    firstCtrlEvent = event
                    event.changes.forEach { it.consume() }
                    gestureState = io.github.rafambn.kmap.gestures.GestureState.CTRL
                    onGestureStart.invoke(gestureState, event.changes[0].position)
                    continue
                }

                //Here are the cases that exits of an gesture to another
                if (eventChanges.any { it == io.github.rafambn.kmap.gestures.GestureChangeState.CTRL_RELEASE } && gestureState == io.github.rafambn.kmap.gestures.GestureState.CTRL) {
                    onGestureEnd.invoke(gestureState)
                    event.changes.forEach { it.consume() }
                    gestureState = io.github.rafambn.kmap.gestures.GestureState.DRAG
                    onGestureStart.invoke(gestureState, event.changes[0].position)
                    continue
                }

                if (eventChanges.any { it == io.github.rafambn.kmap.gestures.GestureChangeState.RELEASE } && gestureState == io.github.rafambn.kmap.gestures.GestureState.CTRL) {
                    onGestureEnd.invoke(gestureState)
                    val velocity = runCatching {
                        rotationVelocityTracker.calculateVelocity()
                    }.getOrDefault(0F)
                    val rotationCapped = (velocity / flingRotationScale).coerceIn(flingRotationMaxRange)
                    if (kotlin.math.abs(rotationCapped) > flingRotationThreshold) {
                        onFlingRotation(firstCtrlEvent!!.changes[0].position, rotationCapped)
                    }
                    event.changes.forEach { it.consume() }
                    gestureState = io.github.rafambn.kmap.gestures.GestureState.HOVER
                    onGestureStart.invoke(gestureState, event.changes[0].position)
                    continue
                }

                if (gestureState == io.github.rafambn.kmap.gestures.GestureState.TAP_LONG_PRESS && eventChanges.any { it == io.github.rafambn.kmap.gestures.GestureChangeState.RELEASE }) {
                    onGestureEnd.invoke(gestureState)
                    event.changes.forEach { it.consume() }
                    gestureState = io.github.rafambn.kmap.gestures.GestureState.HOVER
                    onGestureStart.invoke(gestureState, event.changes[0].position)
                    continue
                }

                if (gestureState == io.github.rafambn.kmap.gestures.GestureState.TAP_SWIPE && event.changes.all { !it.pressed }) {
                    onGestureEnd.invoke(gestureState)
                    val velocity = runCatching {
                        zoomVelocityTracker.calculateVelocity()
                    }.getOrDefault(0F)
                    val zoomCapped = (velocity / (flingZoomScale * tapSwipeScale)).coerceIn(flingZoomMaxRange)
                    if (kotlin.math.abs(zoomCapped) > flingZoomThreshold) {
                        onFlingZoom(event.changes[0].position, zoomCapped)
                    }
                    event.changes.forEach { it.consume() }
                    gestureState = io.github.rafambn.kmap.gestures.GestureState.HOVER
                    onGestureStart.invoke(gestureState, event.changes[0].position)
                    continue
                }

                if (gestureState == io.github.rafambn.kmap.gestures.GestureState.DRAG && eventChanges.any { it == io.github.rafambn.kmap.gestures.GestureChangeState.RELEASE }) {
                    onGestureEnd.invoke(gestureState)
                    val velocity = runCatching {
                        panVelocityTracker.calculateVelocity()
                    }.getOrDefault(androidx.compose.ui.unit.Velocity.Zero)
                    val velocityCapped = androidx.compose.ui.unit.Velocity(
                        (velocity.x / flingVelocityScale).coerceIn(flingVelocityMaxRange),
                        (velocity.y / flingVelocityScale).coerceIn(flingVelocityMaxRange)
                    )
                    val velocitySquared = velocityCapped.x.pow(2) + velocityCapped.y.pow(2)
                    kotlin.io.println("$velocitySquared --- $flingVelocityThreshold --- $velocityCapped")
                    if (velocitySquared > flingVelocityThreshold) {
                        onFling(velocityCapped)
                    }
                    event.changes.forEach { it.consume() }
                    gestureState = io.github.rafambn.kmap.gestures.GestureState.HOVER
                    onGestureStart.invoke(gestureState, event.changes[0].position)
                    continue
                }

                //Finally, here are the gestures
                if (gestureState == io.github.rafambn.kmap.gestures.GestureState.CTRL) {
                    io.github.rafambn.kmap.gestures.handleGestureWithCtrl(event, previousEvent, firstCtrlEvent!!, touchSlop) { rotationChange, centroid ->
                        onGesture.invoke(centroid, androidx.compose.ui.geometry.Offset.Zero, 0F, rotationChange)
                        rotationVelocityTracker.addDataPoint(event.changes[0].uptimeMillis, rotationChange)
                    }
                    event.changes.forEach { it.consume() }
                    continue
                }

                if (gestureState == io.github.rafambn.kmap.gestures.GestureState.DRAG) {
                    onDrag.invoke(event.changes[0].position - previousEvent.changes[0].position)
                    panVelocityTracker.addPosition(event.changes[0].uptimeMillis, event.changes[0].position)
                    event.changes.forEach { it.consume() }
                    continue
                }

                if (gestureState == io.github.rafambn.kmap.gestures.GestureState.TAP_LONG_PRESS) {
                    onTapLongPress.invoke(event.changes[0].position - previousEvent.changes[0].position)
                    event.changes.forEach { it.consume() }
                    continue
                }

                if (gestureState == io.github.rafambn.kmap.gestures.GestureState.TAP_SWIPE) {
                    onTapSwipe.invoke(firstTapEvent!!.changes[0].position, (event.changes[0].position.y - previousEvent.changes[0].position.y) / tapSwipeScale)
                    zoomVelocityTracker.addDataPoint(event.changes[0].uptimeMillis, event.changes[0].position.y)
                    event.changes.forEach { it.consume() }
                    continue
                }

                if (gestureState == io.github.rafambn.kmap.gestures.GestureState.HOVER) {
                    onHover.invoke(event.changes[0].position)
                }

            } catch (_: androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException) {
                //It case of a timeout them just check the case where timeout is necessary
                timeoutCount -= 10L
                if (gestureState == io.github.rafambn.kmap.gestures.GestureState.WAITING_UP || gestureState == io.github.rafambn.kmap.gestures.GestureState.WAITING_DOWN || gestureState == io.github.rafambn.kmap.gestures.GestureState.WAITING_UP_AFTER_TAP) {
                    if (timeoutCount < 0) {
                        onGestureEnd.invoke(gestureState)
                        onLongPress.invoke(event.changes[0].position)
                        event.changes.forEach { it.consume() }
                        gestureState =
                            if (gestureState == io.github.rafambn.kmap.gestures.GestureState.WAITING_UP_AFTER_TAP) io.github.rafambn.kmap.gestures.GestureState.TAP_LONG_PRESS else io.github.rafambn.kmap.gestures.GestureState.HOVER
                        onGestureStart.invoke(gestureState, event.changes[0].position)
                        continue
                    }
                }
            }
        } while (this@coroutineScope.isActive && !event.changes.any { it.isOutOfBounds(size, extendedTouchPadding) })

        onGestureEnd.invoke(gestureState)
    }
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