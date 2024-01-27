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
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.VelocityTracker1D
import androidx.compose.ui.unit.IntSize
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
        var gestureState = GestureState.HOVER

        var firstCtrlEvent: PointerEvent? = null
        var firstTapEvent: PointerEvent? = null
        val longPressTimeout = viewConfiguration.longPressTimeoutMillis
        val doubleTapTimeout = viewConfiguration.doubleTapTimeoutMillis
        val touchSlop = viewConfiguration.touchSlop
        var timeoutCount = 0L
        var panSlop = Offset.Zero
        val panVelocityTracker = VelocityTracker()
        val zoomVelocityTracker = VelocityTracker1D(isDataDifferential = false)
        val rotationVelocityTracker = VelocityTracker1D(isDataDifferential = false)

        val flingVelocityThreshold = 200.dp.toPx().pow(2)
        val flingVelocityMaxRange = -8000f..8000f

        val flingZoomThreshold = 200.dp.toPx()
        val flingZoomMaxRange = -1000f..1000f

        val flingRotationThreshold = 50.dp.toPx()
        val flingRotationMaxRange = -1000f..1000f

        onGestureStart.invoke(GestureState.HOVER, event.changes[0].position)
        do {
            //Awaits for a pointer event
            try {
                previousEvent = event

                event = withTimeout(10L) {
                    awaitPointerEvent()
                }

                if (event.type == PointerEventType.Scroll) {
                    event.changes.forEach { it.consume() }
                    onScroll.invoke(event.changes[0].position, event.changes[0].scrollDelta.y)
                    continue
                }

                //If doesn't timeout them checks if there is any changes in the state
                val eventChanges = getGestureStateChanges(event, previousEvent)

                //Here are the cases that leads to an gesture

                if (eventChanges.any { it == GestureChangeState.PRESS } && !event.keyboardModifiers.isCtrlPressed && gestureState == GestureState.HOVER) {
                    onGestureEnd.invoke(gestureState)
                    event.changes.forEach { it.consume() }
                    gestureState = GestureState.WAITING_UP
                    timeoutCount = longPressTimeout
                    continue
                }

                if (gestureState == GestureState.WAITING_UP) {
                    if (eventChanges.any { it == GestureChangeState.RELEASE }) {
                        event.changes.forEach { it.consume() }
                        gestureState = GestureState.WAITING_DOWN
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
                        onGestureEnd.invoke(gestureState)
                        onLongPress.invoke(event.changes[0].position)
                        event.changes.forEach { it.consume() }
                        gestureState = GestureState.HOVER
                        onGestureStart.invoke(gestureState, event.changes[0].position)
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
                        onGestureEnd.invoke(gestureState)
                        onTap.invoke(event.changes[0].position)
                        event.changes.forEach { it.consume() }
                        gestureState = GestureState.HOVER
                        onGestureStart.invoke(gestureState, event.changes[0].position)
                        continue
                    }
                    continue
                }

                if (gestureState == GestureState.WAITING_UP_AFTER_TAP) {
                    if (eventChanges.any { it == GestureChangeState.RELEASE }) {
                        onDoubleTap.invoke(event.changes[0].position)
                        event.changes.forEach { it.consume() }
                        gestureState = GestureState.HOVER
                        onGestureStart.invoke(gestureState, event.changes[0].position)
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

                if (eventChanges.any { it == GestureChangeState.PRESS } && event.keyboardModifiers.isCtrlPressed && gestureState == GestureState.HOVER) {
                    onGestureEnd.invoke(gestureState)
                    firstCtrlEvent = event
                    event.changes.forEach { it.consume() }
                    gestureState = GestureState.CTRL
                    onGestureStart.invoke(gestureState, event.changes[0].position)
                    continue
                }

                if (eventChanges.any { it == GestureChangeState.CTRL_PRESS } && gestureState == GestureState.DRAG) {
                    onGestureEnd.invoke(gestureState)
                    firstCtrlEvent = event
                    event.changes.forEach { it.consume() }
                    gestureState = GestureState.CTRL
                    onGestureStart.invoke(gestureState, event.changes[0].position)
                    continue
                }

                //Here are the cases that exits of an gesture to another
                if (eventChanges.any { it == GestureChangeState.CTRL_RELEASE } && gestureState == GestureState.CTRL) {
                    onGestureEnd.invoke(gestureState)
                    event.changes.forEach { it.consume() }
                    gestureState = GestureState.DRAG
                    onGestureStart.invoke(gestureState, event.changes[0].position)
                    continue
                }

                if (eventChanges.any { it == GestureChangeState.RELEASE } && gestureState == GestureState.CTRL) {
                    onGestureEnd.invoke(gestureState)
                    handleGestureWithCtrl(event, previousEvent, firstCtrlEvent!!, touchSlop) { rotationChange, centroid ->
                        val velocity = runCatching {
                            rotationVelocityTracker.calculateVelocity()
                        }.getOrDefault(0F)
                        val rotationCapped = velocity.coerceIn(flingRotationMaxRange)
                        println("$rotationCapped  ---- $flingRotationThreshold")
                        if (rotationCapped > flingRotationThreshold) {
                            onFlingRotation(firstCtrlEvent.changes[0].position, rotationCapped / 100F)
                        }
                    }
                    event.changes.forEach { it.consume() }
                    gestureState = GestureState.HOVER
                    onGestureStart.invoke(gestureState, event.changes[0].position)
                    continue
                }

                if (gestureState == GestureState.TAP_LONG_PRESS && eventChanges.any { it == GestureChangeState.RELEASE }) {
                    onGestureEnd.invoke(gestureState)
                    event.changes.forEach { it.consume() }
                    gestureState = GestureState.HOVER
                    onGestureStart.invoke(gestureState, event.changes[0].position)
                    continue
                }

                if (gestureState == GestureState.TAP_SWIPE && event.changes.all { !it.pressed }) {
                    onGestureEnd.invoke(gestureState)
                    val velocity = runCatching {
                        zoomVelocityTracker.calculateVelocity()
                    }.getOrDefault(0F)
                    val zoomCapped = velocity.coerceIn(flingZoomMaxRange)
                    println("$zoomCapped  ---- $flingZoomThreshold")
                    if (zoomCapped > flingZoomThreshold) {
                        onFlingZoom(event.changes[0].position, zoomCapped / 1000F)
                    }
                    event.changes.forEach { it.consume() }
                    gestureState = GestureState.HOVER
                    onGestureStart.invoke(gestureState, event.changes[0].position)
                    continue
                }

                if (gestureState == GestureState.DRAG && eventChanges.any { it == GestureChangeState.RELEASE }) {
                    onGestureEnd.invoke(gestureState)
                    val velocity = runCatching {
                        panVelocityTracker.calculateVelocity()
                    }.getOrDefault(Velocity.Zero)
                    val velocitySquared = velocity.x.pow(2) + velocity.y.pow(2)
                    val velocityCapped = Velocity(
                        velocity.x.coerceIn(flingVelocityMaxRange),
                        velocity.y.coerceIn(flingVelocityMaxRange)
                    )
                    if (velocitySquared > flingVelocityThreshold) {
                        onFling(velocityCapped / 10F)
                    }
                    event.changes.forEach { it.consume() }
                    gestureState = GestureState.HOVER
                    onGestureStart.invoke(gestureState, event.changes[0].position)
                    continue
                }

                //Finally, here are the gestures
                if (gestureState == GestureState.CTRL) {
                    handleGestureWithCtrl(event, previousEvent, firstCtrlEvent!!, touchSlop) { rotationChange, centroid ->
                        onGesture.invoke(centroid, Offset.Zero, 0F, rotationChange)
                        rotationVelocityTracker.addDataPoint(event.changes[0].uptimeMillis, rotationChange)
                    }
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
                    onTapSwipe.invoke(firstTapEvent!!.changes[0].position, event.changes[0].position.y - previousEvent.changes[0].position.y)
                    zoomVelocityTracker.addDataPoint(event.changes[0].uptimeMillis, event.changes[0].position.y)
                    event.changes.forEach { it.consume() }
                    continue
                }

                if (gestureState == GestureState.HOVER) {
                    onHover.invoke(event.changes[0].position)
                }

            } catch (_: PointerEventTimeoutCancellationException) {
                //It case of a timeout them just check the case where timeout is necessary
                timeoutCount -= 10L
                if (gestureState == GestureState.WAITING_UP || gestureState == GestureState.WAITING_DOWN || gestureState == GestureState.WAITING_UP_AFTER_TAP) {
                    if (timeoutCount < 0) {
                        onGestureEnd.invoke(gestureState)
                        onLongPress.invoke(event.changes[0].position)
                        event.changes.forEach { it.consume() }
                        gestureState = if (gestureState == GestureState.WAITING_UP_AFTER_TAP) GestureState.TAP_LONG_PRESS else GestureState.HOVER
                        onGestureStart.invoke(gestureState, event.changes[0].position)
                        continue
                    }
                }
            }
        } while (this@coroutineScope.isActive && !event.changes.any { it.customIsOutOfBounds(size, extendedTouchPadding) })

        onGestureEnd.invoke(gestureState)
    }
}

//This is necessary because for jvm method isOutOfBounds doesn't work properly for bottom and right edges
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

internal actual suspend fun AwaitPointerEventScope.awaitForGestureReset() {
    if (currentEvent.changes.any { it.isOutOfBounds(size, extendedTouchPadding) }) {
        do {
            val events = awaitPointerEvent(PointerEventPass.Final)
        } while (events.changes.any { it.isOutOfBounds(size, extendedTouchPadding) })
    }
}