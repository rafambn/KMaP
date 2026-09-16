package com.rafambn.kmap.mapSource.tiled.canvas

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import com.rafambn.kmap.gestures.MapGestureWrapper
import com.rafambn.kmap.gestures.detectMapGestures
import com.rafambn.kmap.gestures.sharedPointerInput
import com.rafambn.kmap.utils.asScreenOffset

internal fun Modifier.mapGestures(gestureWrapper: MapGestureWrapper?): Modifier = this.then(
    gestureWrapper?.let {
        Modifier.sharedPointerInput {
            detectMapGestures(
                onTap = gestureWrapper.onTap,
                onDoubleTap = gestureWrapper.onDoubleTap,
                onLongPress = gestureWrapper.onLongPress,
                onTapLongPress = gestureWrapper.onTapLongPress,
                onTapSwipe = gestureWrapper.onTapSwipe,
                onGesture = gestureWrapper.onGesture,
                onTwoFingersTap = gestureWrapper.onTwoFingersTap,
                onHover = gestureWrapper.onHover,
            )
        }
    } ?: Modifier
).then(
    gestureWrapper?.onScroll?.let {
        Modifier.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val pointerEvent = awaitPointerEvent()
                    if (pointerEvent.type == PointerEventType.Scroll) {
                        pointerEvent.changes.forEach {
                            if (it.scrollDelta.y != 0F)
                                gestureWrapper.onScroll.invoke(it.position.asScreenOffset(), it.scrollDelta.y)
                        }
                    }
                }
            }
        }
    } ?: Modifier
)
