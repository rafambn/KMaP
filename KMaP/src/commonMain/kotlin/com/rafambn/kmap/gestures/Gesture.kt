package com.rafambn.kmap.gestures

import androidx.compose.foundation.gestures.awaitAllPointersUp
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isOutOfBounds
import androidx.compose.ui.unit.IntSize
import com.rafambn.kmap.utils.DifferentialScreenOffset
import com.rafambn.kmap.utils.ScreenOffset
import com.rafambn.kmap.utils.asScreenOffset
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.atan2

suspend fun PointerInputScope.detectMapGestures(
    // common use
    onTap: ((screenOffset: ScreenOffset) -> Unit)? = null,
    onDoubleTap: ((screenOffset: ScreenOffset) -> Unit)? = null,
    onLongPress: ((screenOffset: ScreenOffset) -> Unit)? = null,
    onTapLongPress: ((screenOffsetDiff: DifferentialScreenOffset) -> Unit)? = null,
    onTapSwipe: ((screenOffset: ScreenOffset, zoom: Float) -> Unit)? = null,
    onDrag: ((screenOffsetDiff: DifferentialScreenOffset) -> Unit)? = null,

    // mobile use
    onTwoFingersTap: ((screenOffset: ScreenOffset) -> Unit)? = null,
    onGesture: ((screenOffset: ScreenOffset, screenOffsetDiff: DifferentialScreenOffset, zoom: Float, rotation: Float) -> Unit)? = null,

    // jvm/web use
    onHover: ((screenOffset: ScreenOffset) -> Unit)? = null,
    onScroll: ((screenOffset: ScreenOffset, scrollAmount: Float) -> Unit)? = null,
    onCtrlGesture: ((rotation: Float) -> Unit)? = null
) = coroutineScope {
    awaitEachGesture {
        //Parameters
        val longPressTimeout = viewConfiguration.longPressTimeoutMillis
        val doubleTapTimeout = viewConfiguration.doubleTapTimeoutMillis
        val touchSlop = viewConfiguration.touchSlop
        var panSlop: Offset
        val zoomScale = 100F  //TODO verify this scale

        var gestureState: GestureState

        var event: PointerEvent
        var firstGestureEvent: PointerEvent? = null
        do {
            event = awaitPointerEvent()
        } while (
            !event.changes.all { it.changedToDown() } ||
            (onHover != null && !event.changes.all { it.isConsumed }) ||
            (onScroll != null && event.type == PointerEventType.Scroll)
        )

        if (!event.changes.all { it.changedToDown() }) {
            gestureState = if (event.keyboardModifiers.isCtrlPressed) {
                GestureState.CTRL
            } else
                GestureState.WAITING_UP

        } else if ((onHover != null && !event.changes.all { it.isConsumed })) {
            event.changes.forEach {
                if (!it.isConsumed && !it.isOutOfBounds(size, extendedTouchPadding)) {
                    it.consume()
                    onHover.invoke(it.position.asScreenOffset())
                }
            }
            gestureState = GestureState.HOVER
        } else {
            event.changes.forEach {
                if (it.scrollDelta.y != 0F) {
                    it.consume()
                    onScroll?.invoke(it.position.asScreenOffset(), it.scrollDelta.y)
                }
            }
            return@awaitEachGesture
        }

        do {
            when (gestureState) {
                GestureState.HOVER -> {
                    do {
                        event = awaitPointerEvent()//TODO improve this part to get only unconsumed event
                        if (event.changes.all { it.isConsumed })
                            continue

                        when (event.type) {
                            PointerEventType.Press -> {
                                gestureState = if (event.keyboardModifiers.isCtrlPressed) {
                                    GestureState.CTRL
                                } else
                                    GestureState.WAITING_UP
                                break
                            }

                            PointerEventType.Move -> {
                                event.changes.forEach {
                                    if (!it.isConsumed && !it.isOutOfBounds(size, extendedTouchPadding)) {
                                        it.consume()
                                        onHover?.invoke(it.position.asScreenOffset())
                                    }
                                }
                            }

                            PointerEventType.Scroll -> {
                                onScroll?.let { onScroll ->
                                    event.changes.forEach {
                                        if (it.scrollDelta != Offset.Zero)
                                            onScroll.invoke(it.position.asScreenOffset(), it.scrollDelta.y)
                                    }
                                }
                            }
                        }
                    } while (!event.changes.all { it.isOutOfBounds(size, extendedTouchPadding) })
                }

                GestureState.WAITING_UP -> {
                    var timePassed = 0L
                    panSlop = Offset.Zero
                    do {
                        try {
                            event = withTimeout(longPressTimeout - timePassed) {
                                awaitPointerEvent()
                            }
                            timePassed += event.changes.minOf { it.uptimeMillis - it.previousUptimeMillis }
                            if (event.changes.all { it.isConsumed })
                                continue

                            when (event.type) {
                                PointerEventType.Press -> {//TODO check all events for consumption
                                    gestureState = GestureState.WAITING_UP_AFTER_TWO_PRESS
                                    break
                                }

                                PointerEventType.Release -> {
                                    gestureState = GestureState.WAITING_DOWN
                                    break
                                }

                                PointerEventType.Move -> {
                                    panSlop += event.calculatePan()
                                    if (panSlop.getDistance() > touchSlop) {
                                        gestureState = GestureState.DRAG
                                        break
                                    }
                                }

                                PointerEventType.Scroll -> {
                                    onScroll?.let { onScroll ->
                                        event.changes.forEach {
                                            if (it.scrollDelta != Offset.Zero)
                                                onScroll.invoke(it.position.asScreenOffset(), it.scrollDelta.y)
                                        }
                                    }
                                }
                            }
                        } catch (_: PointerEventTimeoutCancellationException) {
                            onLongPress?.invoke(event.changes[0].position.asScreenOffset())
                            if (onHover != null) {
                                gestureState = GestureState.HOVER
                                break
                            } else if (onScroll != null) {
                                gestureState = GestureState.SCROLL
                                break
                            } else
                                return@awaitEachGesture
                        }
                    } while (!event.changes.all { it.isOutOfBounds(size, extendedTouchPadding) })
                }

                GestureState.CTRL -> {
                    do {
                        event = awaitPointerEvent()
                        if (event.changes.all { it.isConsumed })
                            continue

                        when (event.type) {

                            PointerEventType.Release -> {
                                gestureState = GestureState.HOVER
                                break
                            }

                            PointerEventType.Move -> {
                                if (event.keyboardModifiers.isCtrlPressed) {
                                    handleGestureWithCtrl(event, size / 2) { rotationChange ->
                                        onCtrlGesture?.invoke(rotationChange)
                                    }//TODO validate all nulls
                                } else {
                                    gestureState = GestureState.DRAG
                                    break
                                }
                            }

                            PointerEventType.Scroll -> {
                                onScroll?.let { onScroll ->
                                    event.changes.forEach {
                                        if (it.scrollDelta != Offset.Zero)
                                            onScroll.invoke(it.position.asScreenOffset(), it.scrollDelta.y)
                                    }
                                }
                            }
                        }
                    } while (!event.changes.all { it.isOutOfBounds(size, extendedTouchPadding) })
                }

                GestureState.WAITING_DOWN -> {
                    var timePassed = 0L
                    panSlop = Offset.Zero

                    do {
                        try {
                            event = withTimeout(longPressTimeout - timePassed) {
                                awaitPointerEvent()
                            }
                            timePassed += event.changes.minOf { it.uptimeMillis - it.previousUptimeMillis }
                            if (event.changes.all { it.isConsumed })
                                continue

                            when (event.type) {
                                PointerEventType.Press -> {
                                    gestureState = GestureState.WAITING_UP_AFTER_TAP
                                    break
                                }

                                PointerEventType.Move -> {
                                    panSlop += event.calculatePan()
                                    if (panSlop.getDistance() > touchSlop) {
                                        onTap?.invoke(event.changes[0].position.asScreenOffset())
                                        gestureState = GestureState.HOVER
                                        break
                                    }
                                }

                                PointerEventType.Scroll -> {
                                    onScroll?.let { onScroll ->
                                        event.changes.forEach {
                                            if (it.scrollDelta != Offset.Zero)
                                                onScroll.invoke(it.position.asScreenOffset(), it.scrollDelta.y)
                                        }
                                    }
                                }
                            }
                        } catch (_: PointerEventTimeoutCancellationException) {
                            onTap?.invoke(event.changes[0].position.asScreenOffset())
                            gestureState = GestureState.HOVER
                            break
                        }
                    } while (!event.changes.all { it.isOutOfBounds(size, extendedTouchPadding) })
                }

                GestureState.WAITING_UP_AFTER_TAP -> {
                    var timePassed = 0L
                    panSlop = Offset.Zero
                    do {
                        try {
                            event = withTimeout(longPressTimeout - timePassed) {
                                awaitPointerEvent()
                            }
                            timePassed += event.changes.minOf { it.uptimeMillis - it.previousUptimeMillis }
                            if (event.changes.all { it.isConsumed })
                                continue

                            when (event.type) {
                                PointerEventType.Release -> {
                                    onDoubleTap?.invoke(event.changes[0].position.asScreenOffset())
                                    gestureState = GestureState.HOVER
                                    break
                                }

                                PointerEventType.Move -> {
                                    panSlop += event.calculatePan()
                                    if (panSlop.getDistance() > touchSlop) {
                                        gestureState = GestureState.TAP_SWIPE
                                        firstGestureEvent = event
                                        break
                                    }
                                }

                                PointerEventType.Scroll -> {
                                    onScroll?.let { onScroll ->
                                        event.changes.forEach {
                                            if (it.scrollDelta != Offset.Zero)
                                                onScroll.invoke(it.position.asScreenOffset(), it.scrollDelta.y)
                                        }
                                    }
                                }
                            }
                        } catch (_: PointerEventTimeoutCancellationException) {
                            gestureState = GestureState.TAP_LONG_PRESS
                            break
                        }
                    } while (this@coroutineScope.isActive && !event.changes.any { it.isOutOfBounds(size, extendedTouchPadding) })
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

                GestureState.SCROLL -> {
//                    while (this@coroutineScope.isActive && !event.changes.any { it.customIsOutOfBounds(size, extendedTouchPadding) }) {
//                        previousEvent = event
//                        event = awaitPointerEvent()
//                        event.changes.forEach { it.consume() }
//                        val eventChanges = getGestureStateChanges(event, previousEvent)
//
//                        if (eventChanges.any { it == GestureChangeState.RELEASE }) {
//                            gestureState = GestureState.HOVER
//                            break
//                        }
//                        onTapSwipe.invoke(
//                            firstGestureEvent!!.changes[0].position,
//                            (event.changes[0].position.y - previousEvent.changes[0].position.y) / zoomScale
//                        )
//                    }
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
            }
        } while (this@coroutineScope.isActive && !event.changes.any { it.isOutOfBounds(size, extendedTouchPadding) })

    }
}

fun handleGestureWithCtrl(
    event: PointerEvent,
    intSize: IntSize,
    result: (rotationChange: Float) -> Unit
) {
    val screenCenter = Offset(intSize.width.toFloat(), intSize.height.toFloat())
    val currentCentroid = event.changes[0].position - screenCenter
    val previousCentroid = event.changes[0].previousPosition - screenCenter
    result(previousCentroid.angle() - currentCentroid.angle())
}

private fun Offset.angle(): Float =
    if (x == 0f && y == 0f) 0f else atan2(x, y) * 180f / PI.toFloat()