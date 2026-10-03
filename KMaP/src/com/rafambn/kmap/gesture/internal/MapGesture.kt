package com.rafambn.kmap.gesture.internal

import androidx.compose.foundation.gestures.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
import com.rafambn.kmap.geometry.angle.Degrees
import com.rafambn.kmap.geometry.plane.DifferentialScreenOffset
import com.rafambn.kmap.geometry.plane.ScreenOffset
import com.rafambn.kmap.geometry.plane.asDifferentialScreenOffset
import com.rafambn.kmap.geometry.plane.asScreenOffset
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.abs

suspend fun PointerInputScope.detectMapGestures(
    // common use
    onTap: ((position: ScreenOffset) -> Unit)? = null,
    onDoubleTap: ((position: ScreenOffset) -> Unit)? = null,
    onLongPress: ((position: ScreenOffset) -> Unit)? = null,
    onTapLongPress: ((position: ScreenOffset) -> Unit)? = null,
    onTapSwipe: ((zoomFactor: Float, rotationDelta: Degrees) -> Unit)? = null,
    onTransform: ((
        centroid: ScreenOffset,
        panDelta: DifferentialScreenOffset,
        zoomFactor: Float,
        rotationDelta: Degrees,
    ) -> Unit)? = null,

    // mobile use
    onTwoFingerTap: ((position: ScreenOffset) -> Unit)? = null,

    // jvm/web use
    onHover: ((position: ScreenOffset) -> Unit)? = null,
) = coroutineScope {
    awaitEachGesture {
        val longPressTimeout = viewConfiguration.longPressTimeoutMillis
        val doubleTapTimeout = viewConfiguration.doubleTapTimeoutMillis
        val touchSlop = viewConfiguration.touchSlop
        var panSlop: Offset

        var mapGestureState: MapGestureState

        var event: PointerEvent
        do {
            event = awaitPointerEventWithTimeout()
        } while (
            event.type != PointerEventType.Scroll &&
            !(event.type == PointerEventType.Press && (onTap != null || onDoubleTap != null || onLongPress != null ||
                    onTapLongPress != null || onTapSwipe != null || onTwoFingerTap != null || onTransform != null)) &&
            !(event.type == PointerEventType.Move && onHover != null)
        )

        when (event.type) {
            PointerEventType.Press -> {
                mapGestureState = MapGestureState.WAITING_UP
            }

            PointerEventType.Move -> {
                event.changes.forEach {
                    if (!it.isOutOfBounds(size, extendedTouchPadding)) {
                        onHover?.invoke(it.position.asScreenOffset())
                    }
                }
                mapGestureState = MapGestureState.HOVER
            }

            else -> return@awaitEachGesture
        }

        do {
            when (mapGestureState) {
                MapGestureState.HOVER -> {
                    do {
                        event = awaitPointerEventWithTimeout()

                        when (event.type) {
                            PointerEventType.Press -> {
                                if (onTap != null || onDoubleTap != null || onLongPress != null || onTapLongPress != null ||
                                    onTapSwipe != null || onTwoFingerTap != null || onTransform != null
                                ) {
                                    mapGestureState = MapGestureState.WAITING_UP
                                    break
                                }
                            }

                            PointerEventType.Move -> {
                                event.changes.forEach {
                                    if (!it.isOutOfBounds(size, extendedTouchPadding)) {
                                        onHover?.invoke(it.position.asScreenOffset())
                                    }
                                }
                            }
                        }
                    } while (!event.changes.all { it.isOutOfBounds(size, extendedTouchPadding) })
                }

                MapGestureState.WAITING_UP -> {
                    var timePassed = 0L
                    panSlop = Offset.Zero
                    do {
                        try {
                            event = awaitPointerEventWithTimeout(longPressTimeout - timePassed)
                            timePassed += event.changes.minOf { it.uptimeMillis - it.previousUptimeMillis }

                            when (event.type) {
                                PointerEventType.Press -> {
                                    if (onTwoFingerTap == null && onTransform == null) {
                                        continue
                                    }

                                    mapGestureState =
                                        if (onTwoFingerTap != null) MapGestureState.WAITING_UP_AFTER_TWO_PRESS else MapGestureState.GESTURE
                                    break
                                }

                                PointerEventType.Release -> {
                                    if (event.changes.size == 1) {
                                        if (onDoubleTap != null || onTapLongPress != null || onTapSwipe != null) {
                                            mapGestureState = MapGestureState.WAITING_DOWN
                                            break
                                        }
                                        if (onTap != null) {
                                            onTap.invoke(event.changes[0].position.asScreenOffset())
                                            return@awaitEachGesture
                                        }
                                    }
                                }

                                PointerEventType.Move -> {
                                    panSlop += event.calculatePan()
                                    if (onTransform != null && panSlop.getDistance() > touchSlop) {
                                        mapGestureState = MapGestureState.GESTURE
                                        break
                                    }
                                }
                            }
                        } catch (_: PointerEventTimeoutCancellationException) {
                            onLongPress?.invoke(event.changes[0].position.asScreenOffset())
                            return@awaitEachGesture
                        }
                    } while (!event.changes.all { it.isOutOfBounds(size, extendedTouchPadding) })
                }

                MapGestureState.WAITING_DOWN -> {
                    var timePassed = 0L
                    panSlop = Offset.Zero
                    do {
                        try {
                            event = awaitPointerEventWithTimeout(doubleTapTimeout - timePassed)
                            timePassed += event.changes.minOf { it.uptimeMillis - it.previousUptimeMillis }

                            when (event.type) {
                                PointerEventType.Press -> {
                                    if (onDoubleTap != null || onTapSwipe != null || onTapLongPress != null)
                                        mapGestureState = MapGestureState.WAITING_UP_AFTER_TAP
                                    else {
                                        onTap?.invoke(event.changes[0].position.asScreenOffset())
                                        mapGestureState = MapGestureState.WAITING_UP
                                    }
                                    break
                                }

                                PointerEventType.Move -> {
                                    panSlop += event.calculatePan()
                                    if (onTap != null && panSlop.getDistance() > touchSlop) {
                                        onTap.invoke(event.changes[0].position.asScreenOffset())
                                        return@awaitEachGesture
                                    }
                                }
                            }
                        } catch (_: PointerEventTimeoutCancellationException) {
                            onTap?.invoke(event.changes[0].position.asScreenOffset())
                            return@awaitEachGesture
                        }
                    } while (!event.changes.all { it.isOutOfBounds(size, extendedTouchPadding) })
                }

                MapGestureState.WAITING_UP_AFTER_TAP -> {
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
                                        mapGestureState = MapGestureState.TAP_SWIPE
                                        break
                                    }
                                }
                            }
                        } catch (_: PointerEventTimeoutCancellationException) {
                            onTapLongPress?.invoke(event.changes[0].position.asScreenOffset())
                            return@awaitEachGesture
                        }
                    } while (!event.changes.all { it.isOutOfBounds(size, extendedTouchPadding) })
                }

                MapGestureState.TAP_SWIPE -> {
                    do {
                        event = awaitPointerEventWithTimeout()

                        when (event.type) {
                            PointerEventType.Release -> {
                                return@awaitEachGesture
                            }

                            PointerEventType.Move -> {
                                val pointerInputEvent = event.changes.first { it.pressed }

                                val screenCenter = Offset(size.width / 2f, size.height / 2f)
                                val currentCentroid = pointerInputEvent.position - screenCenter
                                val previousCentroid = pointerInputEvent.previousPosition - screenCenter
                                val currentRadius = currentCentroid.getDistance()
                                val previousRadius = previousCentroid.getDistance()
                                val zoomFactor = if (currentRadius == 0f || previousRadius == 0f) 1f
                                    else currentRadius / previousRadius
                                val rotationDelta = previousCentroid.angle() - currentCentroid.angle()

                                onTapSwipe?.invoke(
                                    zoomFactor,
                                    Degrees((rotationDelta.value + 180.0).mod(360.0) - 180.0),
                                )
                            }
                        }
                    } while (!event.changes.all { it.isOutOfBounds(size, extendedTouchPadding) })
                }

                MapGestureState.WAITING_UP_AFTER_TWO_PRESS -> {
                    var timePassed = 0L
                    var zoomSlop = 1f
                    var rotationSlop = 0f
                    panSlop = Offset.Zero
                    do {
                        try {
                            event = awaitPointerEventWithTimeout(longPressTimeout - timePassed)
                            timePassed += event.changes.minOf { it.uptimeMillis - it.previousUptimeMillis }

                            when (event.type) {
                                PointerEventType.Release -> {
                                    if (event.changes.size == 1 && !event.changes[0].pressed) {
                                        onTwoFingerTap?.invoke(event.changes[0].position.asScreenOffset())
                                        return@awaitEachGesture
                                    }
                                }

                                PointerEventType.Move -> {
                                    if (onTransform != null) {
                                        panSlop += event.calculatePan()
                                        zoomSlop *= event.calculateZoom()
                                        rotationSlop += event.calculateRotation()
                                        val radius = event.calculateCentroidSize(useCurrent = false)
                                        val zoomMotion = abs(1f - zoomSlop) * radius
                                        val rotationMotion = abs(rotationSlop) * PI / 180.0 * radius
                                        if (panSlop.getDistance() > touchSlop || zoomMotion > touchSlop || rotationMotion > touchSlop) {
                                            mapGestureState = MapGestureState.GESTURE
                                            break
                                        }
                                    }
                                }
                            }
                        } catch (_: PointerEventTimeoutCancellationException) {
                            if (onTransform != null) {
                                mapGestureState = MapGestureState.GESTURE
                                break
                            } else {
                                onLongPress?.invoke(event.changes[0].position.asScreenOffset())
                                return@awaitEachGesture
                            }
                        }
                    } while (!event.changes.all { it.isOutOfBounds(size, extendedTouchPadding) })
                }

                MapGestureState.GESTURE -> {
                    do {
                        event = awaitPointerEventWithTimeout()

                        when (event.type) {
                            PointerEventType.Release -> {
                                if (event.changes.size == 1 && !event.changes[0].pressed)
                                    return@awaitEachGesture
                            }

                            PointerEventType.Move -> {
                                val zoomFactor = event.calculateZoom()
                                val rotationDelta = Degrees(event.calculateRotation().toDouble())
                                val panDelta = event.calculatePan()
                                val centroid = event.calculateCentroid(useCurrent = false)
                                if (centroid != Offset.Unspecified) {
                                    onTransform?.invoke(
                                        centroid.asScreenOffset(),
                                        panDelta.asDifferentialScreenOffset(),
                                        zoomFactor,
                                        rotationDelta,
                                    )
                                }
                            }
                        }
                    } while (!event.changes.all { it.isOutOfBounds(size, extendedTouchPadding) })
                }
            }
        } while (this@coroutineScope.isActive && !event.changes.any { it.isOutOfBounds(size, extendedTouchPadding) })
    }
}
