package com.rafambn.kmap.gestures

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathHitTester
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.isOutOfBounds
import com.rafambn.kmap.utils.ProjectedCoordinates
import com.rafambn.kmap.utils.ScreenOffset
import com.rafambn.kmap.utils.asScreenOffset
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive

suspend fun PointerInputScope.detectPathGestures(
    onTap: ((ProjectedCoordinates) -> Unit)? = null,
    onDoubleTap: ((ProjectedCoordinates) -> Unit)? = null,
    onLongPress: ((ProjectedCoordinates) -> Unit)? = null,
    onHover: ((ProjectedCoordinates) -> Unit)? = null,
    convertScreenOffsetToProjectedCoordinates: (ScreenOffset) -> ProjectedCoordinates,
    path: Path,
    threshold: Float = 10f,
    checkForInsideClick: Boolean
) = coroutineScope {
    val pathMeasure = PathMeasure()
    pathMeasure.setPath(path, false)
    val pathHitTester = PathHitTester(path, threshold)
    val tester = PathTester(pathHitTester, pathMeasure, threshold, checkForInsideClick, path.getBounds().topLeft)

    awaitEachGesture {
        val longPressTimeout = viewConfiguration.longPressTimeoutMillis
        val doubleTapTimeout = viewConfiguration.doubleTapTimeoutMillis
        val touchSlop = viewConfiguration.touchSlop
        var panSlop: Offset

        var pathGestureState: PathGestureState

        var event: PointerEvent
        var referencePointer: PointerInputChange? = null
        do {
            event = awaitPointerEventWithTimeout()
        } while (
            !(event.type == PointerEventType.Press && (onTap != null || onDoubleTap != null || onLongPress != null) &&
                    tester.checkHit(event.changes[0].position)) &&
            !(event.type == PointerEventType.Move && onHover != null)
        )

        when (event.type) {
            PointerEventType.Press -> {
                pathGestureState = PathGestureState.WAITING_UP
                referencePointer = event.changes[0]
            }

            PointerEventType.Move -> {
                event.changes.forEach {
                    if (!it.isOutOfBounds(size, extendedTouchPadding)) {
                        onHover?.invoke(convertScreenOffsetToProjectedCoordinates(it.position.asScreenOffset()))
                    }
                }
                pathGestureState = PathGestureState.HOVER
            }

            else -> return@awaitEachGesture
        }

        do {
            when (pathGestureState) {
                PathGestureState.WAITING_UP -> {
                    var timePassed = 0L
                    panSlop = Offset.Zero
                    do {
                        try {
                            event = awaitPointerEventWithTimeout(longPressTimeout - timePassed)
                            timePassed += event.changes.minOf { it.uptimeMillis - it.previousUptimeMillis }

                            when (event.type) {
                                PointerEventType.Release -> {
                                    if (event.changes.size == 1) {
                                        if (onDoubleTap != null) {
                                            pathGestureState = PathGestureState.WAITING_DOWN
                                            break
                                        }
                                        onTap?.invoke(convertScreenOffsetToProjectedCoordinates(referencePointer!!.position.asScreenOffset()))
                                        return@awaitEachGesture
                                    }
                                }

                                PointerEventType.Move -> {
                                    if (!tester.checkHit(event.changes[0].position))
                                        return@awaitEachGesture

                                    panSlop += event.calculatePan()
                                    if (panSlop.getDistance() > touchSlop)
                                        return@awaitEachGesture
                                }
                            }
                        } catch (_: PointerEventTimeoutCancellationException) {
                            onLongPress?.invoke(convertScreenOffsetToProjectedCoordinates(referencePointer!!.position.asScreenOffset()))
                            return@awaitEachGesture
                        }
                    } while (!event.changes.all { it.isOutOfBounds(size, extendedTouchPadding) })
                }

                PathGestureState.WAITING_DOWN -> {
                    var timePassed = 0L
                    panSlop = Offset.Zero
                    var previousEvent = event
                    do {
                        try {
                            event = awaitPointerEventWithTimeout(doubleTapTimeout - timePassed)
                            timePassed += event.changes.minOf { it.uptimeMillis } - previousEvent.changes.minOf { it.uptimeMillis }
                            if (doubleTapTimeout - timePassed < 0)
                                throw PointerEventTimeoutCancellationException(0)

                            when (event.type) {
                                PointerEventType.Press -> {
                                    if (tester.checkHit(event.changes[0].position)) {
                                        referencePointer = event.changes[0]
                                        if (onDoubleTap != null) {
                                            pathGestureState = PathGestureState.WAITING_UP_AFTER_TAP
                                            break
                                        } else {
                                            onTap?.invoke(convertScreenOffsetToProjectedCoordinates(referencePointer.position.asScreenOffset()))
                                            pathGestureState = PathGestureState.WAITING_UP
                                            break
                                        }
                                    } else {
                                        onTap?.invoke(convertScreenOffsetToProjectedCoordinates(referencePointer!!.position.asScreenOffset()))
                                        return@awaitEachGesture
                                    }
                                }

                                PointerEventType.Move -> {
                                    val currentCentroid = event.changes[0].position
                                    val previousCentroid = previousEvent.changes[0].position
                                    panSlop += currentCentroid - previousCentroid
                                    if (panSlop.getDistance() > touchSlop) {
                                        onTap?.invoke(convertScreenOffsetToProjectedCoordinates(referencePointer!!.position.asScreenOffset()))
                                        return@awaitEachGesture
                                    }
                                }
                            }
                            previousEvent = event
                        } catch (_: PointerEventTimeoutCancellationException) {
                            onTap?.invoke(convertScreenOffsetToProjectedCoordinates(referencePointer!!.position.asScreenOffset()))
                            return@awaitEachGesture
                        }
                    } while (!event.changes.all { it.isOutOfBounds(size, extendedTouchPadding) })
                }

                PathGestureState.WAITING_UP_AFTER_TAP -> {
                    var timePassed = 0L
                    panSlop = Offset.Zero
                    do {
                        try {
                            event = awaitPointerEventWithTimeout(longPressTimeout - timePassed)
                            timePassed += event.changes.minOf { it.uptimeMillis - it.previousUptimeMillis }

                            when (event.type) {
                                PointerEventType.Release -> {
                                    if (event.changes.size == 1) {
                                        onDoubleTap?.invoke(convertScreenOffsetToProjectedCoordinates(referencePointer!!.position.asScreenOffset()))
                                        return@awaitEachGesture
                                    }
                                }

                                PointerEventType.Move -> {
                                    if (!tester.checkHit(event.changes[0].position))
                                        return@awaitEachGesture

                                    panSlop += event.calculatePan()
                                    if (panSlop.getDistance() > touchSlop)
                                        return@awaitEachGesture
                                }
                            }
                        } catch (_: PointerEventTimeoutCancellationException) {
                            onLongPress?.invoke(convertScreenOffsetToProjectedCoordinates(referencePointer!!.position.asScreenOffset()))
                            return@awaitEachGesture
                        }
                    } while (!event.changes.all { it.isOutOfBounds(size, extendedTouchPadding) })
                }

                PathGestureState.HOVER -> {
                    do {
                        event = awaitPointerEventWithTimeout()

                        when (event.type) {
                            PointerEventType.Press -> {
                                if ((onTap != null || onDoubleTap != null || onLongPress != null) && tester.checkHit(event.changes[0].position)) {
                                    pathGestureState = PathGestureState.WAITING_UP
                                    break
                                }
                            }

                            PointerEventType.Move -> {
                                event.changes.forEach {
                                    if (!it.isOutOfBounds(size, extendedTouchPadding)) {
                                        onHover?.invoke(convertScreenOffsetToProjectedCoordinates(it.position.asScreenOffset()))
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

