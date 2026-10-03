package com.rafambn.kmap.gesture

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.unit.IntSize
import com.rafambn.kmap.gesture.internal.detectMapGestures
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.coroutines.suspendCoroutine

internal class GestureTestScope(
    events: List<PointerEvent>,
    override val density: Float = 1f,
) : PointerInputScope {
    override val size = IntSize(1000, 1000)
    override val fontScale = 1f
    override val extendedTouchPadding = Size.Zero
    override val viewConfiguration = object : ViewConfiguration {
        override val longPressTimeoutMillis = 500L
        override val doubleTapTimeoutMillis = 300L
        override val doubleTapMinTimeMillis = 40L
        override val touchSlop = 18f * density
    }
    private val eventIterator = events.iterator()
    private val finished = IllegalStateException("End of test input")

    private val eventScope = object : AwaitPointerEventScope {
        override val size get() = this@GestureTestScope.size
        override val density get() = this@GestureTestScope.density
        override val fontScale get() = this@GestureTestScope.fontScale
        override val viewConfiguration get() = this@GestureTestScope.viewConfiguration
        override var currentEvent = PointerEvent(emptyList())
            private set

        override suspend fun awaitPointerEvent(pass: PointerEventPass): PointerEvent {
            if (!eventIterator.hasNext()) throw finished
            return eventIterator.next().also { currentEvent = it }
        }
    }

    override suspend fun <R> awaitPointerEventScope(block: suspend AwaitPointerEventScope.() -> R): R =
        suspendCoroutine { continuation ->
            block.startCoroutine(eventScope, object : Continuation<R> {
                override val context = EmptyCoroutineContext
                override fun resumeWith(result: Result<R>) = continuation.resumeWith(result)
            })
        }

    suspend fun detect(callbacks: MapGestureCallbacks) {
        try {
            detectMapGestures(
                onTap = callbacks.onTap,
                onDoubleTap = callbacks.onDoubleTap,
                onLongPress = callbacks.onLongPress,
                onTapLongPress = callbacks.onTapLongPress,
                onTapSwipe = callbacks.onTapSwipe,
                onTransform = callbacks.onTransform,
                onTwoFingerTap = callbacks.onTwoFingerTap,
                onHover = callbacks.onHover,
            )
        } catch (exception: IllegalStateException) {
            // Coroutine stack recovery can copy the exception and retain the original as its cause.
            if (exception !== finished && exception.cause !== finished) throw exception
        }
    }
}
