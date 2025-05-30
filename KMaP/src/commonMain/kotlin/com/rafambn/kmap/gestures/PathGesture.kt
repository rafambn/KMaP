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
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.isOutOfBounds
import com.rafambn.kmap.core.MapState
import com.rafambn.kmap.components.canvas.tiled.TileDimension
import com.rafambn.kmap.utils.ScreenOffset
import com.rafambn.kmap.utils.asScreenOffset
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive

suspend fun PointerInputScope.detectPathGestures(
    onTap: ((ScreenOffset) -> Unit)? = null,
    onDoubleTap: ((ScreenOffset) -> Unit)? = null,
    onLongPress: ((ScreenOffset) -> Unit)? = null,
    mapState: MapState,
    path: Path,
    threshold: Float = 10f,
) = coroutineScope {
    val pathMeasure = PathMeasure()
    pathMeasure.setPath(path, false)
    val pathHitTester = PathHitTester(path, threshold)

    val tester = PathTester(pathHitTester, pathMeasure, threshold, mapState.mapProperties.tileSize)

    awaitEachGesture {
        val longPressTimeout = viewConfiguration.longPressTimeoutMillis
        val doubleTapTimeout = viewConfiguration.doubleTapTimeoutMillis
        val touchSlop = viewConfiguration.touchSlop
        var panSlop: Offset

        var pathGestureState: PathGestureState

        var event: PointerEvent

        do {
            event = awaitPointerEventWithTimeout()
        } while (
            !(event.type == PointerEventType.Press && (onTap != null || onDoubleTap != null || onLongPress != null)
                    && event.changes.any { !it.isConsumed && it.pressed && tester.checkHit(it.position) })
        )

        pathGestureState = PathGestureState.WAITING_UP
        //TODO check for consumed on pathGesture and MapGesture
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
                                    if (onDoubleTap != null) {
                                        pathGestureState = PathGestureState.WAITING_UP_AFTER_TAP
                                        break
                                    } else {
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
    }
}

class PathTester(
    private val path: PathHitTester,
    private val pathMeasure: PathMeasure,
    private val threshold: Float,
    private val tileDimension: TileDimension
) {

    fun checkHit(point: Offset): Boolean {
        val pointTranslated = point.copy((point.x - tileDimension.width.toFloat()) / 2, (point.y - tileDimension.height.toFloat()) / 2)
        if (isPointInsidePath(path, pointTranslated))
            return true
        if (isPointNearPath(pathMeasure, pointTranslated, threshold))
            return true

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
        val initialPointOnPath = pathMeasure.getPosition(0f)

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
