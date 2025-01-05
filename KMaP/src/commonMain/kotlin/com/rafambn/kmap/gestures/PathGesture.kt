package com.rafambn.kmap.gestures

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import com.rafambn.kmap.core.MapState
import com.rafambn.kmap.utils.Coordinates
import com.rafambn.kmap.utils.asScreenOffset
import kotlinx.coroutines.coroutineScope

private suspend fun PointerInputScope.detectPathGestures( //TODO study on how to share pointer input
    onDoubleTap: ((Coordinates) -> Unit)? = null,
    onLongPress: ((Coordinates) -> Unit)? = null,
    onTap: ((Coordinates) -> Unit)? = null,
    mapState: MapState,
    path: Path,
) = coroutineScope {
    awaitEachGesture {
        val down = awaitFirstDown(pass = PointerEventPass.Final)
//        if (!mapState.isPathHit(down.position, path))
//            return@awaitEachGesture
        down.consume()
        val longPressTimeout = onLongPress?.let {
            viewConfiguration.longPressTimeoutMillis
        } ?: (Long.MAX_VALUE / 2)
        var upOrCancel: PointerInputChange? = null
        try {
            upOrCancel = withTimeout(longPressTimeout) {
                waitForUpOrCancellation()
            }
            if (upOrCancel == null)
                return@awaitEachGesture
            else {
                upOrCancel.consume()
                if (onDoubleTap == null) {
                    onTap?.invoke(with(mapState) { upOrCancel.position.asScreenOffset().toTilePoint().toCoordinates() })
                } else {
                    try {
                        val secondDown = withTimeout(viewConfiguration.doubleTapMinTimeMillis) {
                            awaitFirstDown()
                        }
//                        if (!mapState.isPathHit(secondDown.position, path)) {
//                            onTap?.invoke(with(mapState) { upOrCancel.position.asScreenOffset().toTilePoint().toCoordinates() })
//                            return@awaitEachGesture
//                        }
                        secondDown.consume()
                        try {
                            withTimeout(longPressTimeout) {
                                val secondUp = waitForUpOrCancellation()
                                if (secondUp != null) {
                                    secondUp.consume()
                                    onDoubleTap(with(mapState) { secondUp.position.asScreenOffset().toTilePoint().toCoordinates() })
                                } else
                                    onTap?.invoke(with(mapState) { upOrCancel.position.asScreenOffset().toTilePoint().toCoordinates() })
                            }
                        } catch (_: PointerEventTimeoutCancellationException) {
                            onTap?.invoke(with(mapState) { upOrCancel.position.asScreenOffset().toTilePoint().toCoordinates() })
                            consumeUntilUp()
                        }
                    } catch (_: PointerEventTimeoutCancellationException) {
                        onTap?.invoke(with(mapState) { upOrCancel.position.asScreenOffset().toTilePoint().toCoordinates() })
                    }
                }
            }
        } catch (_: PointerEventTimeoutCancellationException) {
            onLongPress?.invoke(with(mapState) { down.position.asScreenOffset().toTilePoint().toCoordinates() })
            consumeUntilUp()
        }
    }
}

suspend fun AwaitPointerEventScope.consumeUntilUp() {
    do {
        val event = awaitPointerEvent()
        event.changes.fastForEach { it.consume() }
    } while (event.changes.fastAny { it.pressed })
}