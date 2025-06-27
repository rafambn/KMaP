package com.rafambn.kmap.gestures

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
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

fun Offset.angle(): Double =
    if (x == 0f && y == 0f) 0.0 else atan2(x, y) * 180.0 / PI
