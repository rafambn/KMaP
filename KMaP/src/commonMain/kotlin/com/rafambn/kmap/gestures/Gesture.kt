package com.rafambn.kmap.gestures

import androidx.compose.foundation.gestures.awaitAllPointersUp
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isOutOfBounds
import com.rafambn.kmap.utils.DifferentialScreenOffset
import com.rafambn.kmap.utils.ScreenOffset
import com.rafambn.kmap.utils.asScreenOffset
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlin.coroutines.cancellation.CancellationException

suspend fun PointerInputScope.detectMapGesturesNew(
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
    awaitMapGesture {
        //Parameters
        val longPressTimeout = viewConfiguration.longPressTimeoutMillis
        val doubleTapTimeout = viewConfiguration.doubleTapTimeoutMillis
        val touchSlop = viewConfiguration.touchSlop
        var panSlop: Offset
        val zoomScale = 100F  //TODO verify this scale

        var gestureState = GestureState.START_GESTURE

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
                firstGestureEvent = event
                GestureState.CTRL
            } else
                GestureState.WAITING_UP

        } else if ((onHover != null && !event.changes.all { it.isConsumed })) {
            event.changes.forEach {
                if (!it.isConsumed && !it.isOutOfBounds(size, extendedTouchPadding)) {
                    it.consume()
                    onHover.invoke(it.position.asScreenOffset())
                    gestureState = GestureState.HOVER
                }
            }
        } else {
            event.changes.forEach {
                if (it.scrollDelta.y != 0F) {
                    it.consume()
                    onScroll?.invoke(it.position.asScreenOffset(), it.scrollDelta.y)
                    return@awaitMapGesture
                }
            }
        }

        do {
            when (gestureState) {
                GestureState.HOVER -> {
                    do {
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

                        event = awaitPointerEvent()
                    } while (!event.changes.any { it.isOutOfBounds(size, extendedTouchPadding) })
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

/**
 * [awaitMapGesture] is a version of [awaitEachGesture] where after the gestures ends it does
 * not [awaitAllPointersUp].
 *
 * Repeatedly calls [block] to handle gestures. If there is a [CancellationException],
 * it will exits if [isActive] is `false`.
 */
internal suspend fun PointerInputScope.awaitMapGesture(block: suspend AwaitPointerEventScope.() -> Unit) {
    val currentContext = currentCoroutineContext()
    awaitPointerEventScope {
        while (currentContext.isActive) {
            try {
                block()
            } catch (e: CancellationException) {
                if (!currentContext.isActive)
                    throw e
            }
        }
    }
}