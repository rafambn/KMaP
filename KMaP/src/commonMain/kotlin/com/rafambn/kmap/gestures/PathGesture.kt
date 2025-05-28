package com.rafambn.kmap.gestures

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import com.rafambn.kmap.core.MapState
import com.rafambn.kmap.utils.Coordinates
import com.rafambn.kmap.utils.asScreenOffset
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.cancellation.CancellationException

/**
 * Detects tap, double tap, and long press gestures on a path.
 *
 * @param onTap Called when a tap is detected on the path
 * @param onDoubleTap Called when a double tap is detected on the path
 * @param onLongPress Called when a long press is detected on the path
 * @param mapState The map state
 * @param path The path to detect gestures on
 * @param threshold The maximum distance from the path to consider a hit
 */
suspend fun PointerInputScope.detectPathGestures(
    onTap: (() -> Unit)? = null,
    onDoubleTap: ((Coordinates) -> Unit)? = null,
    onLongPress: ((Coordinates) -> Unit)? = null,
    mapState: MapState,
    path: Path,
    threshold: Float = 10f
) = coroutineScope {
    // Create a PathMeasure for hit testing
    val pathMeasure = PathMeasure()
    pathMeasure.setPath(path, false)

    val currentContext = currentCoroutineContext()
    awaitEachGesture{
        onTap?.invoke()
    }

//    awaitEachGesture {
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

/**
 * Checks if a point is inside a closed path.
 * This uses the PathMeasure to check if the point is inside the path.
 *
 * @param path The path to check
 * @param point The point to check
 * @return True if the point is inside the path, false otherwise
 */
private fun isPointInsidePath(path: Path, point: Offset): Boolean {
    // Use the built-in contains method if available
    // This is a placeholder - in a real implementation, you would use the appropriate method
    // to check if the point is inside the path
    return false
}

/**
 * Checks if a point is near a path.
 * This uses the PathMeasure to find the closest point on the path and check if it's within the threshold.
 *
 * @param pathMeasure The PathMeasure for the path
 * @param point The point to check
 * @param threshold The maximum distance to consider the point near the path
 * @return True if the point is near the path, false otherwise
 */
private fun isPointNearPath(pathMeasure: PathMeasure, point: Offset, threshold: Float): Boolean {
    // Find the closest point on the path to the given point
    val closestPoint = findClosestPointOnPath(pathMeasure, point)

    // Calculate the distance between the point and the closest point on the path
    val distance = calculateDistance(point, closestPoint)

    // Return true if the distance is less than or equal to the threshold
    return distance <= threshold
}

/**
 * Finds the closest point on a path to a given point.
 *
 * @param pathMeasure The PathMeasure for the path
 * @param point The point to find the closest point to
 * @return The closest point on the path
 */
private fun findClosestPointOnPath(pathMeasure: PathMeasure, point: Offset): Offset {
    // This is a simplified implementation
    // In a real implementation, you would use the PathMeasure to find the closest point

    // For now, we'll return a placeholder
    return Offset.Zero
}

/**
 * Calculates the distance between two points.
 *
 * @param a The first point
 * @param b The second point
 * @return The distance between the points
 */
private fun calculateDistance(a: Offset, b: Offset): Float {
    val dx = a.x - b.x
    val dy = a.y - b.y
    return kotlin.math.sqrt(dx * dx + dy * dy)
}

/**
 * Consumes all pointer events until an up event is received.
 */
private suspend fun AwaitPointerEventScope.consumePathEvents() {
    do {
        val event = awaitPointerEvent()
        event.changes.forEach { it.consume() }
    } while (event.changes.fastAny { it.pressed })
}
