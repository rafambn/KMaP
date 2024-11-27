package com.rafambn.kmap.gestures

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
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
import com.rafambn.kmap.utils.asDifferentialScreenOffset
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
    onTapLongPress: ((screenOffset: ScreenOffset) -> Unit)? = null,
    onTapSwipe: ((zoom: Float) -> Unit)? = null,
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
        do {
            event = awaitPointerEventWithTimeout()
        } while (
            (event.type == PointerEventType.Scroll && onScroll != null) ||
            (event.type == PointerEventType.Press && (onTap != null || onDoubleTap != null || onLongPress != null ||
                    onTapLongPress != null || onTapSwipe != null || onDrag != null || onTwoFingersTap != null || onGesture != null || onCtrlGesture != null)) ||
            (event.type == PointerEventType.Move && onHover != null)
        )

        when (event.type) {
            PointerEventType.Scroll -> {
                onScroll?.let { scrollLambda ->
                    event.changes.forEach {
                        if (it.scrollDelta.y != 0F) {
                            it.consume()
                            scrollLambda.invoke(it.position.asScreenOffset(), it.scrollDelta.y)
                        }
                    }
                }
                return@awaitEachGesture
            }

            PointerEventType.Press -> {
                gestureState = if (event.keyboardModifiers.isCtrlPressed && onCtrlGesture != null) {
                    GestureState.CTRL
                } else
                    GestureState.WAITING_UP
            }

            PointerEventType.Move -> {
                event.changes.forEach {
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
                        event = awaitPointerEventWithTimeout()

                        when (event.type) {
                            PointerEventType.Press -> {
                                if (onCtrlGesture != null && event.keyboardModifiers.isCtrlPressed) {
                                    gestureState = GestureState.CTRL
                                    break
                                }
                                if (onTap != null || onDoubleTap != null || onLongPress != null || onTapLongPress != null || onTapSwipe != null ||
                                    onDrag != null || onTwoFingersTap != null || onGesture != null
                                ) {
                                    event.changes.forEach { it.consume() }
                                    gestureState = GestureState.WAITING_UP
                                    break
                                }
                            }

                            PointerEventType.Move -> {
                                event.changes.forEach {
                                    if (!it.isOutOfBounds(size, extendedTouchPadding)) {
                                        it.consume()
                                        onHover?.invoke(it.position.asScreenOffset())
                                    }
                                }
                            }

                            PointerEventType.Scroll -> {
                                onScroll?.let { scrollLambda ->
                                    event.changes.forEach {
                                        if (it.scrollDelta.y != 0F) {
                                            it.consume()
                                            scrollLambda.invoke(it.position.asScreenOffset(), it.scrollDelta.y)
                                        }
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
                            event = awaitPointerEventWithTimeout(longPressTimeout - timePassed)
                            timePassed += event.changes.minOf { it.uptimeMillis - it.previousUptimeMillis }

                            when (event.type) {
                                PointerEventType.Press -> {
                                    if (onTwoFingersTap == null && onGesture == null) {
                                        continue
                                    }

                                    event.changes.forEach { it.consume() }
                                    gestureState = if (onTwoFingersTap != null) GestureState.WAITING_UP_AFTER_TWO_PRESS else GestureState.MOBILE
                                    break
                                }

                                PointerEventType.Release -> {
                                    if (event.changes.size == 1) {
                                        if (onDoubleTap != null || onTapLongPress != null || onTapSwipe != null) {
                                            event.changes.forEach { it.consume() }
                                            gestureState = GestureState.WAITING_DOWN
                                            break
                                        }
                                        if (onTap != null) {
                                            event.changes.forEach { it.consume() }
                                            onTap.invoke(event.changes[0].position.asScreenOffset())
                                            return@awaitEachGesture
                                        }
                                    }
                                }

                                PointerEventType.Move -> {
                                    panSlop += event.calculatePan()
                                    event.changes.forEach { it.consume() }
                                    if (onCtrlGesture != null && event.keyboardModifiers.isCtrlPressed && panSlop.getDistance() > touchSlop) {
                                        gestureState = GestureState.CTRL
                                        break
                                    }
                                    if (onDrag != null && panSlop.getDistance() > touchSlop) {
                                        gestureState = GestureState.DRAG
                                        break
                                    }
                                }

                                PointerEventType.Scroll -> {
                                    onScroll?.let { scrollLambda ->
                                        event.changes.forEach {
                                            if (it.scrollDelta.y != 0F) {
                                                it.consume()
                                                scrollLambda.invoke(it.position.asScreenOffset(), it.scrollDelta.y)
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (_: PointerEventTimeoutCancellationException) {
                            event.changes.forEach { it.consume() }
                            onLongPress?.invoke(event.changes[0].position.asScreenOffset())
                            return@awaitEachGesture
                        }
                    } while (!event.changes.all { it.isOutOfBounds(size, extendedTouchPadding) })
                }

                GestureState.CTRL -> {
                    do {
                        event = awaitPointerEventWithTimeout()

                        when (event.type) {
                            PointerEventType.Release -> {
                                event.changes.forEach { it.consume() }
                                return@awaitEachGesture
                            }

                            PointerEventType.Move -> {
                                if (event.keyboardModifiers.isCtrlPressed) {
                                    handleGestureWithCtrl(event.changes.first { it.pressed }, size / 2) { rotationChange ->
                                        event.changes.first { it.pressed }.consume()
                                        onCtrlGesture?.invoke(rotationChange)
                                    }
                                } else {
                                    event.changes.forEach { it.consume() }
                                    if (onDrag != null) {
                                        gestureState = GestureState.DRAG
                                        break
                                    } else
                                        return@awaitEachGesture
                                }
                            }

                            PointerEventType.Scroll -> {
                                onScroll?.let { scrollLambda ->
                                    event.changes.forEach {
                                        if (it.scrollDelta.y != 0F) {
                                            it.consume()
                                            scrollLambda.invoke(it.position.asScreenOffset(), it.scrollDelta.y)
                                        }
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
                            event = awaitPointerEventWithTimeout(doubleTapTimeout - timePassed)
                            timePassed += event.changes.minOf { it.uptimeMillis - it.previousUptimeMillis }

                            when (event.type) {
                                PointerEventType.Press -> {
                                    if (onDoubleTap != null || onTapSwipe != null || onTapLongPress != null)
                                        gestureState = GestureState.WAITING_UP_AFTER_TAP
                                    else {
                                        event.changes.forEach { it.consume() }
                                        onTap?.invoke(event.changes[0].position.asScreenOffset())
                                        gestureState = GestureState.WAITING_UP
                                    }
                                    break
                                }

                                PointerEventType.Move -> {
                                    panSlop += event.calculatePan()
                                    event.changes.forEach { it.consume() }
                                    if (onTap != null && panSlop.getDistance() > touchSlop) {
                                        onTap.invoke(event.changes[0].position.asScreenOffset())
                                        return@awaitEachGesture
                                    }
                                }

                                PointerEventType.Scroll -> {
                                    onScroll?.let { scrollLambda ->
                                        event.changes.forEach {
                                            if (it.scrollDelta.y != 0F) {
                                                it.consume()
                                                scrollLambda.invoke(it.position.asScreenOffset(), it.scrollDelta.y)
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (_: PointerEventTimeoutCancellationException) {
                            if (onTap != null) {
                                event.changes.forEach { it.consume() }
                                onTap.invoke(event.changes[0].position.asScreenOffset())
                                return@awaitEachGesture
                            }
                        }
                    } while (!event.changes.all { it.isOutOfBounds(size, extendedTouchPadding) })
                }

                GestureState.WAITING_UP_AFTER_TAP -> {
                    var timePassed = 0L
                    panSlop = Offset.Zero
                    do {
                        try {
                            event = awaitPointerEventWithTimeout(longPressTimeout - timePassed)
                            timePassed += event.changes.minOf { it.uptimeMillis - it.previousUptimeMillis }

                            when (event.type) {
                                PointerEventType.Release -> {
                                    onDoubleTap?.invoke(event.changes[0].position.asScreenOffset())
                                    return@awaitEachGesture
                                }

                                PointerEventType.Move -> {
                                    panSlop += event.calculatePan()
                                    if (panSlop.getDistance() > touchSlop && onTapSwipe != null) {
                                        gestureState = GestureState.TAP_SWIPE
                                        break
                                    }
                                }

                                PointerEventType.Scroll -> {
                                    onScroll?.let { scrollLambda ->
                                        event.changes.forEach {
                                            if (it.scrollDelta.y != 0F) {
                                                it.consume()
                                                scrollLambda.invoke(it.position.asScreenOffset(), it.scrollDelta.y)
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (_: PointerEventTimeoutCancellationException) {
                            onTapLongPress?.invoke(event.changes[0].position.asScreenOffset())
                            return@awaitEachGesture
                        }
                    } while (!event.changes.all { it.isOutOfBounds(size, extendedTouchPadding) })
                }

                GestureState.DRAG -> {
                    do {
                        event = awaitPointerEventWithTimeout()

                        when (event.type) {
                            PointerEventType.Release -> {
                                event.changes.forEach { it.consume() }
                                return@awaitEachGesture
                            }

                            PointerEventType.Press -> {
                                if (onGesture != null) {
                                    gestureState = GestureState.MOBILE
                                    break
                                }
                            }

                            PointerEventType.Move -> {
                                if (event.keyboardModifiers.isCtrlPressed) {
                                    if (onCtrlGesture != null) {
                                        gestureState = GestureState.CTRL
                                        break
                                    }
                                } else {
                                    onDrag?.invoke(event.calculatePan().asDifferentialScreenOffset())
                                    if (onDrag != null) {
                                        gestureState = GestureState.DRAG
                                        break
                                    } else
                                        return@awaitEachGesture
                                }
                            }

                            PointerEventType.Scroll -> {
                                onScroll?.let { scrollLambda ->
                                    event.changes.forEach {
                                        if (it.scrollDelta.y != 0F) {
                                            it.consume()
                                            scrollLambda.invoke(it.position.asScreenOffset(), it.scrollDelta.y)
                                        }
                                    }
                                }
                            }
                        }
                    } while (!event.changes.all { it.isOutOfBounds(size, extendedTouchPadding) })
                }

                GestureState.TAP_SWIPE -> {
                    do {
                        event = awaitPointerEventWithTimeout()

                        when (event.type) {
                            PointerEventType.Release -> {
                                event.changes.forEach { it.consume() }
                                return@awaitEachGesture
                            }

                            PointerEventType.Move -> {
                                onTapSwipe?.invoke((event.calculatePan().getDistance()))
                            }

                            PointerEventType.Scroll -> {
                                onScroll?.let { scrollLambda ->
                                    event.changes.forEach {
                                        if (it.scrollDelta.y != 0F) {
                                            it.consume()
                                            scrollLambda.invoke(it.position.asScreenOffset(), it.scrollDelta.y)
                                        }
                                    }
                                }
                            }
                        }
                    } while (!event.changes.all { it.isOutOfBounds(size, extendedTouchPadding) })
                }

                GestureState.WAITING_UP_AFTER_TWO_PRESS -> {
                    var timePassed = 0L
                    panSlop = Offset.Zero
                    do {
                        try {
                            event = awaitPointerEventWithTimeout(longPressTimeout - timePassed)
                            timePassed += event.changes.minOf { it.uptimeMillis - it.previousUptimeMillis }

                            when (event.type) {
                                PointerEventType.Release -> {
                                    if (event.changes.size == 1 && !event.changes[0].pressed) {
                                        onTwoFingersTap?.invoke(event.changes[0].position.asScreenOffset())
                                        return@awaitEachGesture
                                    }
                                }

                                PointerEventType.Move -> {
                                    if (onGesture != null) {
                                        panSlop += event.calculatePan()
                                        event.changes.forEach { it.consume() }
                                        if (panSlop.getDistance() > touchSlop) {
                                            gestureState = GestureState.MOBILE
                                            break
                                        }
                                    }
                                }

                                PointerEventType.Scroll -> {
                                    onScroll?.let { scrollLambda ->
                                        event.changes.forEach {
                                            if (it.scrollDelta.y != 0F) {
                                                it.consume()
                                                scrollLambda.invoke(it.position.asScreenOffset(), it.scrollDelta.y)
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (_: PointerEventTimeoutCancellationException) {
                            if (onGesture != null) {
                                gestureState = GestureState.MOBILE
                                break
                            }
                        }
                    } while (!event.changes.all { it.isOutOfBounds(size, extendedTouchPadding) })
                }

                GestureState.MOBILE -> {
                    do {
                        event = awaitPointerEventWithTimeout()

                        when (event.type) {
                            PointerEventType.Release -> {
                                if (onDrag != null) {
                                    event.changes.forEach { it.consume() }
                                    gestureState = GestureState.DRAG
                                    break
                                } else if (event.changes.size == 1 && !event.changes[0].pressed)
                                    return@awaitEachGesture
                            }

                            PointerEventType.Move -> {
                                val eventZoomCentroid = event.calculateCentroidSize()
                                val previousEventZoomCentroid = event.calculateCentroidSize(false)
                                var zoomChange = eventZoomCentroid - previousEventZoomCentroid
                                if (eventZoomCentroid == 0f || previousEventZoomCentroid == 0f)
                                    zoomChange = 0.0F

                                val rotationChange = event.calculateRotation()

                                val panChange = event.calculatePan()
                                val centroid = event.calculateCentroid()
                                if (centroid != Offset.Unspecified)
                                    onGesture?.invoke(centroid.asScreenOffset(), panChange.asDifferentialScreenOffset(), zoomChange / zoomScale, rotationChange)
                            }

                            PointerEventType.Scroll -> {
                                onScroll?.let { scrollLambda ->
                                    event.changes.forEach {
                                        if (it.scrollDelta.y != 0F) {
                                            it.consume()
                                            scrollLambda.invoke(it.position.asScreenOffset(), it.scrollDelta.y)
                                        }
                                    }
                                }
                            }
                        }
                    } while (!event.changes.all { it.isOutOfBounds(size, extendedTouchPadding) })
                }
            }
        } while (this@coroutineScope.isActive && !event.changes.any { it.isOutOfBounds(size, extendedTouchPadding) })
    }
}

suspend fun AwaitPointerEventScope.awaitPointerEventWithTimeout(timeOut: Long? = null): PointerEvent {
    return if (timeOut == null)
        awaitPointerEvent()
    else {
        withTimeout(timeOut) {
            awaitPointerEvent()
        }
    }
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