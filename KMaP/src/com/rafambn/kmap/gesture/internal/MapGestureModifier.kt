package com.rafambn.kmap.gesture.internal

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import com.rafambn.kmap.geometry.plane.asScreenOffset
import com.rafambn.kmap.gesture.MapGestureCallbacks

internal fun Modifier.mapGestures(gestureCallbacks: MapGestureCallbacks?): Modifier = this.then(
    gestureCallbacks?.let {
        Modifier.sharedPointerInput {
            detectMapGestures(
                onTap = gestureCallbacks.onTap,
                onDoubleTap = gestureCallbacks.onDoubleTap,
                onLongPress = gestureCallbacks.onLongPress,
                onTapLongPress = gestureCallbacks.onTapLongPress,
                onTapSwipe = gestureCallbacks.onTapSwipe,
                onTransform = gestureCallbacks.onTransform,
                onTwoFingerTap = gestureCallbacks.onTwoFingerTap,
                onHover = gestureCallbacks.onHover,
            )
        }
    } ?: Modifier
).then(
    gestureCallbacks?.onScroll?.let {
        Modifier.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val pointerEvent = awaitPointerEvent()
                    if (pointerEvent.type == PointerEventType.Scroll) {
                        pointerEvent.changes.forEach {
                            if (it.scrollDelta.y != 0F)
                                gestureCallbacks.onScroll.invoke(it.position.asScreenOffset(), it.scrollDelta.y)
                        }
                    }
                }
            }
        }
    } ?: Modifier
)
