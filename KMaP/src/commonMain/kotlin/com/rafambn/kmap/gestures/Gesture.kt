package com.rafambn.kmap.gestures

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.unit.IntSize
import kotlin.math.PI
import kotlin.math.atan2


suspend fun AwaitPointerEventScope.awaitPointerEventWithTimeout(
    timeOut: Long? = null,
    pass: PointerEventPass = PointerEventPass.Main
): PointerEvent {
    return if (timeOut == null)
        awaitPointerEvent(pass)
    else {
        withTimeout(timeOut) {
            awaitPointerEvent(pass)
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
