package com.rafambn.kmap.gestures

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
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

        var event: FilteredPointerEvent
        var firstGestureEvent: PointerEvent? = null
        do {
            event = awaitUnconsumedPointerEvent()
        } while (
            (event.pointerEvent.type == PointerEventType.Scroll && onScroll != null) ||
            (event.pointerEvent.type == PointerEventType.Press && (onTap != null || onDoubleTap != null || onLongPress != null ||
                    onTapLongPress != null || onTapSwipe != null || onDrag != null || onTwoFingersTap != null || onGesture != null || onCtrlGesture != null)) ||
            (event.pointerEvent.type == PointerEventType.Move && onHover != null)
        )

        when (event.pointerEvent.type) {
            PointerEventType.Scroll -> {
                event.filteredInputs[0].consume()
                onScroll?.invoke(event.filteredInputs[0].position.asScreenOffset(), event.filteredInputs[0].scrollDelta.y)
                return@awaitEachGesture
            }

            PointerEventType.Press -> {
                gestureState = if (event.pointerEvent.keyboardModifiers.isCtrlPressed) {
                    GestureState.CTRL
                } else
                    GestureState.WAITING_UP
            }

            PointerEventType.Move -> {
                event.filteredInputs.forEach {
                    if (!it.isOutOfBounds(size, extendedTouchPadding)) {
                        it.consume()
                        onHover?.invoke(it.position.asScreenOffset())
                    }
                }
                gestureState = GestureState.HOVER
            }

            else -> return@awaitEachGesture
        }

        do {
            when (gestureState) {
                GestureState.HOVER -> {
                    do {
                        event = awaitUnconsumedPointerEvent()

                        when (event.pointerEvent.type) {
                            PointerEventType.Press -> {
                                if (onCtrlGesture != null && event.pointerEvent.keyboardModifiers.isCtrlPressed) {
                                    firstGestureEvent = event.pointerEvent
                                    gestureState = GestureState.CTRL
                                    break
                                }
                                if (onTap != null || onDoubleTap != null || onLongPress != null || onTapLongPress != null || onTapSwipe != null ||
                                    onDrag != null || onTwoFingersTap != null || onGesture != null
                                ) {
                                    event.filteredInputs.forEach { it.consume() }
                                    gestureState = GestureState.WAITING_UP
                                    break
                                }
                            }

                            PointerEventType.Move -> {
                                event.filteredInputs.forEach {
                                    if (!it.isOutOfBounds(size, extendedTouchPadding)) {
                                        it.consume()
                                        onHover?.invoke(it.position.asScreenOffset())
                                    }
                                }
                            }

                            PointerEventType.Scroll -> {
                                onScroll?.let { scrollUnit ->
                                    event.filteredInputs.forEach {
                                        if (it.scrollDelta.y != 0F) {
                                            it.consume()
                                            scrollUnit.invoke(it.position.asScreenOffset(), it.scrollDelta.y)
                                        }
                                    }
                                }
                            }
                        }
                    } while (!event.pointerEvent.changes.all { it.isOutOfBounds(size, extendedTouchPadding) })
                }

                GestureState.WAITING_UP -> {
                    var timePassed = 0L
                    panSlop = Offset.Zero
                    do {
                        try {
                            event = awaitUnconsumedPointerEvent(longPressTimeout - timePassed)

                            timePassed += event.pointerEvent.changes.minOf { it.uptimeMillis - it.previousUptimeMillis }

                            when (event.pointerEvent.type) {
                                PointerEventType.Press -> {
                                    if (onTwoFingersTap == null && onGesture == null) {
                                        continue
                                    }

                                    event.filteredInputs.forEach { it.consume() }
                                    gestureState = if (onTwoFingersTap != null) GestureState.WAITING_UP_AFTER_TWO_PRESS else GestureState.MOBILE
                                    break
                                }


                                PointerEventType.Release -> {
                                    if (event.filteredInputs.size == 1) {
                                        if (onDoubleTap != null || onTapLongPress != null || onTapSwipe != null) {
                                            event.filteredInputs.forEach { it.consume() }
                                            gestureState = GestureState.WAITING_DOWN
                                            break
                                        }
                                        if (onTap != null) {
                                            event.filteredInputs.forEach { it.consume() }
                                            onTap.invoke(event.filteredInputs[0].position.asScreenOffset())
                                            return@awaitEachGesture
                                        }
                                    }
                                }

                                PointerEventType.Move -> {
                                    if (onCtrlGesture != null && event.pointerEvent.keyboardModifiers.isCtrlPressed) {
                                        event.filteredInputs.forEach { it.consume() }
                                        firstGestureEvent = event.pointerEvent
                                        gestureState = GestureState.CTRL
                                        break
                                    }
                                    if (onDrag != null) {
                                        panSlop += event.pointerEvent.calculatePan()
                                        event.filteredInputs.forEach { it.consume() }
                                        if (panSlop.getDistance() > touchSlop) {
                                            gestureState = GestureState.DRAG
                                            break
                                        }
                                    }
                                }

                                PointerEventType.Scroll -> {
                                    event.filteredInputs.forEach {
                                        if (it.scrollDelta.y != 0F) {
                                            it.consume()
                                            onScroll?.invoke(it.position.asScreenOffset(), it.scrollDelta.y)
                                        }
                                    }
                                }
                            }
                        } catch (_: PointerEventTimeoutCancellationException) {
                            event.filteredInputs.forEach { it.consume() }
                            onLongPress?.invoke(event.filteredInputs[0].position.asScreenOffset())
                            return@awaitEachGesture
                        }
                    } while (!event.pointerEvent.changes.all { it.isOutOfBounds(size, extendedTouchPadding) })
                }

                GestureState.CTRL -> {
                    do {
                        event = awaitUnconsumedPointerEvent()

                        when (event.pointerEvent.type) {
                            PointerEventType.Release -> {
                                event.filteredInputs.forEach { it.consume() }
                                return@awaitEachGesture
                            }

                            PointerEventType.Move -> {
                                if (event.pointerEvent.keyboardModifiers.isCtrlPressed) {
                                    handleGestureWithCtrl(event.filteredInputs.first { it.pressed }, size / 2) { rotationChange ->
                                        event.filteredInputs.first { it.pressed }.consume()
                                        onCtrlGesture?.invoke(rotationChange)
                                    }
                                } else {
                                    event.filteredInputs.forEach { it.consume() }
                                    if (onDrag != null) {
                                        gestureState = GestureState.DRAG
                                        break
                                    } else
                                        return@awaitEachGesture
                                }
                            }

                            PointerEventType.Scroll -> {
                                event.filteredInputs.forEach {
                                    if (it.scrollDelta.y != 0F) {
                                        it.consume()
                                        onScroll?.invoke(it.position.asScreenOffset(), it.scrollDelta.y)
                                    }
                                }
                            }
                        }
                    } while (!event.pointerEvent.changes.all { it.isOutOfBounds(size, extendedTouchPadding) })
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
        } while (this@coroutineScope.isActive && !event.filteredInputs.any { it.isOutOfBounds(size, extendedTouchPadding) })
    }
}

data class FilteredPointerEvent(
    val pointerEvent: PointerEvent,
    val filteredInputs: List<PointerInputChange>
)

suspend fun AwaitPointerEventScope.awaitUnconsumedPointerEvent(timeOut: Long? = null): FilteredPointerEvent {
    var event: PointerEvent
    var filteredInputs: List<PointerInputChange>
    do {
        event = if (timeOut == null)
            awaitPointerEvent()
        else {
            withTimeout(timeOut) {
                awaitPointerEvent()
            }
        }
        filteredInputs = event.changes.filter { !it.isConsumed }
    } while (filteredInputs.isEmpty())
    return FilteredPointerEvent(pointerEvent = event, filteredInputs = filteredInputs)
}

fun handleGestureWithCtrl(
    event: PointerInputChange,
    intSize: IntSize,
    result: (rotationChange: Float) -> Unit
) {
    val screenCenter = Offset(intSize.width.toFloat(), intSize.height.toFloat())
    val currentCentroid = event.position - screenCenter
    val previousCentroid = event.previousPosition - screenCenter
    result(previousCentroid.angle() - currentCentroid.angle())
}

private fun Offset.angle(): Float =
    if (x == 0f && y == 0f) 0f else atan2(x, y) * 180f / PI.toFloat()