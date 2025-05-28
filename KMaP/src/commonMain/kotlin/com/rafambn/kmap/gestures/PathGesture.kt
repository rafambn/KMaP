package com.rafambn.kmap.gestures

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathHitTester
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.isOutOfBounds
import com.rafambn.kmap.core.MapState
import com.rafambn.kmap.utils.ScreenOffset
import com.rafambn.kmap.utils.asScreenOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

suspend fun PointerInputScope.detectPathGestures(
    onTap: ((ScreenOffset) -> Unit)? = null,
    onDoubleTap: ((ScreenOffset) -> Unit)? = null,
    onLongPress: ((ScreenOffset) -> Unit)? = null,
    mapState: MapState,
    path: Path,
    threshold: Float = 10f
) = coroutineScope {
    // Create a PathMeasure for hit testing
    val pathMeasure = PathMeasure()
    pathMeasure.setPath(path, false)
    val pathHitTester = PathHitTester(path, threshold)

    val tester = PathTester(pathHitTester, pathMeasure, threshold)

    awaitEachGesture {
        val longPressTimeout = viewConfiguration.longPressTimeoutMillis
        val doubleTapTimeout = viewConfiguration.doubleTapTimeoutMillis
        val touchSlop = viewConfiguration.touchSlop
        var panSlop: Offset

        var pathGestureState: PathGestureState

        var event: PointerEvent

        do {
            event = awaitPointerEventWithTimeout()
            if (event.type == PointerEventType.Press) {
                println(event)
                tester.checkHit(event.changes[0].position)
            }
        } while (
            !(event.type == PointerEventType.Press && (onTap != null || onDoubleTap != null || onLongPress != null)
                    && event.changes.any { !it.isConsumed && it.pressed && tester.checkHit(it.position) })
        )

        pathGestureState = PathGestureState.WAITING_UP
        //TODO check for consumeds on pathGEsture and MapGesture
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
                                            event.changes.forEach { it.consume() }
                                            pathGestureState = PathGestureState.WAITING_DOWN
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
                                    if (panSlop.getDistance() > touchSlop) {
                                        return@awaitEachGesture
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

                PathGestureState.WAITING_DOWN -> {
                    var timePassed = 0L
                    panSlop = Offset.Zero
                    do {
                        try {
                            event = awaitPointerEventWithTimeout(doubleTapTimeout - timePassed)
                            timePassed += event.changes.minOf { it.uptimeMillis - it.previousUptimeMillis }

                            when (event.type) {
                                PointerEventType.Press -> {
                                    if (onDoubleTap != null)
                                        pathGestureState = PathGestureState.WAITING_UP_AFTER_TAP
                                    else {
                                        event.changes.forEach { it.consume() }
                                        onTap?.invoke(event.changes[0].position.asScreenOffset())
                                    }
                                    return@awaitEachGesture
                                }

                                PointerEventType.Move -> {
                                    panSlop += event.calculatePan()
                                    event.changes.forEach { it.consume() }
                                    if (onTap != null && panSlop.getDistance() > touchSlop) {
                                        onTap.invoke(event.changes[0].position.asScreenOffset())
                                        return@awaitEachGesture
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

                PathGestureState.WAITING_UP_AFTER_TAP -> {
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
                                    if (panSlop.getDistance() > touchSlop) {
                                        onTap?.invoke(event.changes[0].position.asScreenOffset())
                                        return@awaitEachGesture
                                    }
                                }
                            }
                        } catch (_: PointerEventTimeoutCancellationException) {
                            onLongPress?.invoke(event.changes[0].position.asScreenOffset())
                            return@awaitEachGesture
                        }
                    } while (!event.changes.all { it.isOutOfBounds(size, extendedTouchPadding) })
                }
            }
        } while (this@coroutineScope.isActive && !event.changes.any { it.isOutOfBounds(size, extendedTouchPadding) })


//        val down = awaitFirstDown(pass = PointerEventPass.Initial)
//
//        // Check if the down event hit the path
//        // First check if the point is inside a closed path
//        val isInsidePath = isPointInsidePath(path, down.position)
//
//        // If not inside, check if it's near the path using PathMeasure
//        val isNearPath = if (!isInsidePath) {
//            isPointNearPath(pathMeasure, down.position, threshold)
//        } else {
//            false // Already inside, no need to check if it's near
//        }
//
//        // Only proceed if the point is inside or near the path
//        if (!isInsidePath && !isNearPath) {
//            return@awaitEachGesture
//        }
//
//        // Consume the down event to prevent other gesture detectors from processing it
//        down.consume()
//
//        // Calculate the long press timeout
//        val longPressTimeout = onLongPress?.let {
//            viewConfiguration.longPressTimeoutMillis
//        } ?: (Long.MAX_VALUE / 2)
//
//        var upOrCancel: PointerInputChange? = null
//        try {
//            // Wait for up or timeout (long press)
//            upOrCancel = withTimeout(longPressTimeout) {
//                waitForUpOrCancellation()
//            }
//
//            if (upOrCancel == null) {
//                // The pointer was canceled
//                return@awaitEachGesture
//            } else {
//                // The pointer was lifted
//                upOrCancel.consume()
//
//                if (onDoubleTap == null) {
//                    // If double tap is not supported, just invoke the tap callback
//                    onTap?.invoke(with(mapState) { upOrCancel.position.asScreenOffset().toTilePoint().toCoordinates() })
//                } else {
//                    // If double tap is supported, wait for a second tap
//                    try {
//                        val secondDown = withTimeout(viewConfiguration.doubleTapTimeoutMillis) {
//                            awaitFirstDown(pass = PointerEventPass.Initial)
//                        }
//
//                        // Check if the second down event hit the path
//                        val isSecondInsidePath = isPointInsidePath(path, secondDown.position)
//                        val isSecondNearPath = if (!isSecondInsidePath) {
//                            isPointNearPath(pathMeasure, secondDown.position, threshold)
//                        } else {
//                            false
//                        }
//
//                        if (!isSecondInsidePath && !isSecondNearPath) {
//                            // If the second tap didn't hit the path, treat it as a single tap
//                            onTap?.invoke(with(mapState) { upOrCancel.position.asScreenOffset().toTilePoint().toCoordinates() })
//                            return@awaitEachGesture
//                        }
//
//                        secondDown.consume()
//
//                        try {
//                            withTimeout(longPressTimeout) {
//                                val secondUp = waitForUpOrCancellation()
//                                if (secondUp != null) {
//                                    secondUp.consume()
//                                    onDoubleTap?.invoke(with(mapState) { secondUp.position.asScreenOffset().toTilePoint().toCoordinates() })
//                                } else {
//                                    // The second pointer was canceled
//                                    onTap?.invoke(with(mapState) { upOrCancel.position.asScreenOffset().toTilePoint().toCoordinates() })
//                                }
//                            }
//                        } catch (_: PointerEventTimeoutCancellationException) {
//                            // Long press during the second tap
//                            onTap?.invoke(with(mapState) { upOrCancel.position.asScreenOffset().toTilePoint().toCoordinates() })
//                            consumePathEvents()
//                        }
//                    } catch (_: PointerEventTimeoutCancellationException) {
//                        // Timeout waiting for the second tap, so it's a single tap
//                        onTap?.invoke(with(mapState) { upOrCancel.position.asScreenOffset().toTilePoint().toCoordinates() })
//                    }
//                }
//            }
//        } catch (_: PointerEventTimeoutCancellationException) {
//            // Long press
//            onLongPress?.invoke(with(mapState) { down.position.asScreenOffset().toTilePoint().toCoordinates() })
//            consumePathEvents()
//        }
//    }
    }
}

class PathTester(
    private val path: PathHitTester,
    private val pathMeasure: PathMeasure,
    private val threshold: Float
) {

    fun checkHit(point: Offset): Boolean {
        if (isPointInsidePath(path, point)) {
            return true
        }
       println("point is not on path")
        if (isPointNearPath(pathMeasure, point, threshold)) {
            return true
        }
        println("point is not near path")
        return false
    }

    private fun isPointInsidePath(path: PathHitTester, point: Offset): Boolean {
        return path.contains(point)
    }

    private fun isPointNearPath(
        pathMeasure: PathMeasure,
        point: Offset,
        threshold: Float
    ): Boolean {
        val closestPoint = findClosestPointOnPath(pathMeasure, point)
        val distance = calculateDistance(point, closestPoint)
        return distance <= threshold
    }

    private fun findClosestPointOnPath(pathMeasure: PathMeasure, point: Offset): Offset {
        val pathLength = pathMeasure.length
        val initialPointOnPath = pathMeasure.getPosition(0f) // Assumed safe for initial call

        println("----------------------")
        println(pathLength)
        if (pathLength == 0f) {
            return initialPointOnPath
        }

        val dxInitial = point.x - initialPointOnPath.x
        val dyInitial = point.y - initialPointOnPath.y
        var overallMinDistanceSquared = dxInitial * dxInitial + dyInitial * dyInitial
        var overallClosestPoint = initialPointOnPath

        val minSamples = 30
        val maxSamples = 1000
        val samplesPerUnitLength = 0.2f

        val calculatedSamples = (pathLength * samplesPerUnitLength).toInt()
        val numSamplesToProcess = calculatedSamples.coerceIn(minSamples, maxSamples)

        if (numSamplesToProcess > 0) {
            for (i in 1..numSamplesToProcess) {
                val fraction = i.toFloat() / numSamplesToProcess.toFloat()
                val currentDistanceOnPath = pathLength * fraction
                val pointOnPath = pathMeasure.getPosition(currentDistanceOnPath)

                val dx = point.x - pointOnPath.x
                val dy = point.y - pointOnPath.y
                val currentDistanceSquared = dx * dx + dy * dy

                if (currentDistanceSquared < overallMinDistanceSquared) {
                    overallMinDistanceSquared = currentDistanceSquared
                    overallClosestPoint = pointOnPath
                }
            }
        }

        return overallClosestPoint
    }

    private fun calculateDistance(a: Offset, b: Offset): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
}
