package com.rafambn.kmap.gestures

import androidx.compose.foundation.gestures.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
import com.rafambn.kmap.utils.DifferentialScreenOffset
import com.rafambn.kmap.utils.ScreenOffset
import com.rafambn.kmap.utils.asDifferentialScreenOffset
import com.rafambn.kmap.utils.asScreenOffset
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive

suspend fun PointerInputScope.detectMapGestures(
    // common use
    onTap: ((screenOffset: ScreenOffset) -> Unit)? = null,
    onDoubleTap: ((screenOffset: ScreenOffset) -> Unit)? = null,
    onLongPress: ((screenOffset: ScreenOffset) -> Unit)? = null,
    onTapLongPress: ((screenOffset: ScreenOffset) -> Unit)? = null,
    onTapSwipe: ((zoomChange: Float, rotationChange: Double) -> Unit)? = null,
    onGesture: ((screenOffset: ScreenOffset, screenOffsetDiff: DifferentialScreenOffset, zoom: Float, rotation: Float) -> Unit)? = null,

    // mobile use
    onTwoFingersTap: ((screenOffset: ScreenOffset) -> Unit)? = null,

    // jvm/web use
    onHover: ((screenOffset: ScreenOffset) -> Unit)? = null,
) = coroutineScope {
    awaitEachGesture {
        //Parameters
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
                    onTapLongPress != null || onTapSwipe != null || onTwoFingersTap != null || onGesture != null)) &&
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
                                    onTapSwipe != null || onTwoFingersTap != null || onGesture != null
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
                                    if (onTwoFingersTap == null && onGesture == null) {
                                        continue
                                    }

                                    mapGestureState =
                                        if (onTwoFingersTap != null) MapGestureState.WAITING_UP_AFTER_TWO_PRESS else MapGestureState.GESTURE
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
                                    if (onGesture != null && panSlop.getDistance() > touchSlop) {
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
                                val screenSize = size / 2
                                val pointerInputEvent = event.changes.first { it.pressed }

                                val screenCenter = Offset(screenSize.width.toFloat(), screenSize.height.toFloat())
                                val currentCentroid = pointerInputEvent.position - screenCenter
                                val previousCentroid = pointerInputEvent.previousPosition - screenCenter

                                onTapSwipe?.invoke(
                                    currentCentroid.getDistance() - previousCentroid.getDistance(),
                                    previousCentroid.angle() - currentCentroid.angle()
                                )
                            }
                        }
                    } while (!event.changes.all { it.isOutOfBounds(size, extendedTouchPadding) })
                }

                MapGestureState.WAITING_UP_AFTER_TWO_PRESS -> {
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
                                        if (panSlop.getDistance() > touchSlop) {
                                            mapGestureState = MapGestureState.GESTURE
                                            break
                                        }
                                    }
                                }
                            }
                        } catch (_: PointerEventTimeoutCancellationException) {
                            if (onGesture != null) {
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
                                val eventZoomCentroid = event.calculateCentroidSize()
                                val previousEventZoomCentroid = event.calculateCentroidSize(false)
                                var zoomChange = eventZoomCentroid - previousEventZoomCentroid
                                if (eventZoomCentroid == 0f || previousEventZoomCentroid == 0f)
                                    zoomChange = 0.0F

                                val rotationChange = event.calculateRotation()

                                val panChange = event.calculatePan()
                                val centroid = event.calculateCentroid()
                                if (centroid != Offset.Unspecified) {
                                    onGesture?.invoke(
                                        centroid.asScreenOffset(),
                                        panChange.asDifferentialScreenOffset(),
                                        zoomChange,
                                        rotationChange
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
