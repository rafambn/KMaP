package com.rafambn.kmap.gestures

import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputEventHandler
import androidx.compose.ui.input.pointer.PointerInputFilter
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastMapNotNull
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.Continuation
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.createCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.max

fun Modifier.sharedPointerInput(key1: Any?, block: PointerInputEventHandler, viewConfiguration: ViewConfiguration): Modifier =
    this then SharedSuspendPointerInputElement(key1 = key1, pointerInputEventHandler = block, viewConfiguration = viewConfiguration)

class SharedSuspendPointerInputElement(
    val key1: Any? = null,
    val key2: Any? = null,
    val keys: Array<out Any?>? = null,
    val pointerInputEventHandler: PointerInputEventHandler,
    val viewConfiguration: ViewConfiguration,
) : ModifierNodeElement<SharedSuspendingPointerInputModifierNodeImpl>() {
    override fun InspectorInfo.inspectableProperties() {
        name = "pointerInput"
        properties["key1"] = key1
        properties["key2"] = key2
        properties["keys"] = keys
        properties["pointerInputEventHandler"] = pointerInputEventHandler
    }

    override fun create(): SharedSuspendingPointerInputModifierNodeImpl {
        return SharedSuspendingPointerInputModifierNodeImpl(key1, key2, keys, pointerInputEventHandler, viewConfiguration)
    }

    override fun update(node: SharedSuspendingPointerInputModifierNodeImpl) {
        node.update(key1, key2, keys, pointerInputEventHandler)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SharedSuspendPointerInputElement) return false

        if (key1 != other.key1) return false
        if (key2 != other.key2) return false
        if (keys != null) {
            if (other.keys == null) return false
            if (!keys.contentEquals(other.keys)) return false
        } else if (other.keys != null) return false

        return pointerInputEventHandler === other.pointerInputEventHandler
    }

    override fun hashCode(): Int {
        var result = key1?.hashCode() ?: 0
        result = 31 * result + (key2?.hashCode() ?: 0)
        result = 31 * result + (keys?.contentHashCode() ?: 0)
        result = 31 * result + pointerInputEventHandler.hashCode()
        return result
    }
}

sealed interface SharedSuspendingPointerInputModifierNode : PointerInputModifierNode {
    /**
     * Handler for pointer input events. When changed, any previously executing pointerInputHandler
     * will be canceled.
     */
    @Deprecated(
        message = "This property is deprecated. Use 'pointerInputEventHandler' instead.",
        level = DeprecationLevel.ERROR,
        replaceWith =
            ReplaceWith(
                "pointerInputEventHandler",
                "androidx.compose.ui.input.pointer." +
                        "SuspendingPointerInputModifierNode.pointerInputEventHandler"
            )
    )
    var pointerInputHandler: suspend PointerInputScope.() -> Unit

    /**
     * Handler for pointer input events. When changed, any previously executing
     * pointerInputEventHandler will be canceled.
     */
    // Supports more dynamic use cases than previous functional type version.
    // NOTE: If you implement this interface, replace the default implementation. For more
    // technical details, see aosp/3070509
    var pointerInputEventHandler: PointerInputEventHandler
        get() = TODO("pointerInputEventHandler must be implemented (get()).")
        set(value) = TODO("pointerInputEventHandler must be implemented (set($value)).")

    /**
     * Resets the underlying coroutine used to run the handler for input pointer events. This should
     * be called whenever a large change has been made that forces the gesture detection to be
     * completely invalid.
     *
     * For example, if [pointerInputEventHandler] has different modes for detecting a gesture (long
     * press, double click, etc.), and by switching the modes, any currently-running gestures are no
     * longer valid.
     */
    fun resetPointerInputHandler()
}

class SharedSuspendingPointerInputModifierNodeImpl(
    private var key1: Any? = null,
    private var key2: Any? = null,
    private var keys: Array<out Any?>? = null,
    pointerInputEventHandler: PointerInputEventHandler,
    viewConfiguration: ViewConfiguration,
) : Modifier.Node(), SharedSuspendingPointerInputModifierNode, PointerInputScope {
    @Deprecated("Exists to maintain compatibility with previous API shape")
    constructor(
        key1: Any?,
        key2: Any?,
        keys: Array<out Any?>?,
        pointerInputEvent: suspend PointerInputScope.() -> Unit,
        viewConfiguration: ViewConfiguration,
    ) : this(
        key1 = key1,
        key2 = key2,
        keys = keys,
        pointerInputEventHandler = PointerInputEventHandler {}, // Empty Lambda, not used.
        viewConfiguration = viewConfiguration,
    ) {
        // If the _deprecatedPointerInputHandler is set, we will use that instead of the
        // pointerInputEventHandler (why empty lambda above doesn't matter).
        _deprecatedPointerInputHandler = pointerInputEvent
    }

    override fun sharePointerInputWithSiblings(): Boolean = true

    // Previously used to execute pointer input handlers (now pointerInputEventHandler covers that).
    // This exists purely for backwards compatibility.
    private var _deprecatedPointerInputHandler: (suspend PointerInputScope.() -> Unit)? = null

    // Main handler for pointer input events
    private var _pointerInputEventHandler = pointerInputEventHandler

    @Deprecated("Super property deprecated")
    override var pointerInputHandler: suspend PointerInputScope.() -> Unit
        get() = _deprecatedPointerInputHandler ?: {}
        set(value) {
            resetPointerInputHandler()
            _deprecatedPointerInputHandler = value
        }

    override var pointerInputEventHandler
        set(value) {
            resetPointerInputHandler()
            _deprecatedPointerInputHandler = null
            _pointerInputEventHandler = value
        }
        get() = _pointerInputEventHandler

    override val size: IntSize
        get() = boundsSize

    // The handler for pointer input events is now executed lazily when the first event fires.
    // This job indicates that pointer input handler job is running.
    private var pointerInputJob: Job? = null

    private var currentEvent: PointerEvent = PointerEvent(emptyList())

    /**
     * Actively registered input handlers from currently ongoing calls to [awaitPointerEventScope].
     * Must use `synchronized(pointerHandlersLock)` to access.
     */
    private val pointerHandlers =
        mutableVectorOf<PointerEventHandlerCoroutine<*>>()

    /**
     * Scratch list for dispatching to handlers for a particular phase. Used to hold a copy of the
     * contents of [pointerHandlers] during dispatch so that resumed continuations may add/remove
     * handlers without affecting the current dispatch pass. Must only access on the UI thread.
     */
    private val dispatchingPointerHandlers =
        mutableVectorOf<PointerEventHandlerCoroutine<*>>()

    /**
     * The last pointer event we saw where at least one pointer was currently down; null otherwise.
     * Used to synthesize a fake "all pointers changed to up/all changes to down-state consumed"
     * event for propagating cancellation. This synthetic event corresponds to Android's
     * `MotionEvent.ACTION_CANCEL`.
     */
    private var lastPointerEvent: PointerEvent? = null

    /**
     * The size of the bounds of this input filter. Normally [PointerInputFilter.size] can be used,
     * but for tests, it is better to not rely on something set to an `internal` method.
     */
    private var boundsSize: IntSize = IntSize.Zero

    override val extendedTouchPadding: Size
        get() {
            val minimumTouchTargetSize = viewConfiguration.minimumTouchTargetSize.toSize()
            val size = size
            val horizontal = max(0f, minimumTouchTargetSize.width - size.width) / 2f
            val vertical = max(0f, minimumTouchTargetSize.height - size.height) / 2f
            return Size(horizontal, vertical)
        }
    override val viewConfiguration: ViewConfiguration = viewConfiguration

    override var interceptOutOfBoundsChildEvents: Boolean = false

    override fun onDetach() {
        resetPointerInputHandler()
        super.onDetach()
    }

    // The handler for incoming pointer input events needs to be reset if the density changes.
    override fun onDensityChange() {
        resetPointerInputHandler()
    }

    // The handler for incoming pointer input events needs to be reset if the view configuration
    // changes.
    override fun onViewConfigurationChange() {
        resetPointerInputHandler()
    }

    /**
     * This cancels the existing coroutine and essentially resets pointerInputEventHandler's
     * execution. Note, the pointerInputEventHandler still executes lazily, meaning nothing will be
     * done again until a new event comes in. More details: This is triggered from a LayoutNode if
     * the Density or ViewConfiguration change (in an older implementation using composed, these
     * values were used as keys so it would reset everything when either change, we do that manually
     * now through this function). It is also used for testing.
     */
    override fun resetPointerInputHandler() {
        val localJob = pointerInputJob
        if (localJob != null) {
            localJob.cancel(CancellationException("Pointer input was reset"))
            pointerInputJob = null
        }
    }

    internal fun update(
        key1: Any?,
        key2: Any?,
        keys: Array<out Any?>?,
        pointerInputEventHandler: PointerInputEventHandler,
    ) {
        var needsReset = false

        // key1
        if (this.key1 != key1) {
            needsReset = true
        }
        this.key1 = key1

        // key2
        if (this.key2 != key2) {
            needsReset = true
        }
        this.key2 = key2

        // keys
        if (this.keys != null && keys == null) {
            needsReset = true
        }
        if (this.keys == null && keys != null) {
            needsReset = true
        }
        if (this.keys != null && keys != null && !keys.contentEquals(this.keys)) {
            needsReset = true
        }
        this.keys = keys

        // Lambda literals will have a new instance every time they are executed (even if it is from
        // the same code location), so we can not use them as a comparison mechanism to avoid
        // restarting pointer input handlers when they are functionally the same. However, we can
        // get around this by using a SAM interface and a class comparison instead. (Even though the
        // instances are different, they will have the same class type.)
        if (this.pointerInputEventHandler::class != pointerInputEventHandler::class) {
            needsReset = true
        }

        // Only reset when keys have changed or pointerInputEventHandler is called from a different
        // call site (determined by class comparison).
        if (needsReset) {
            resetPointerInputHandler()
        }
        _pointerInputEventHandler = pointerInputEventHandler
    }

    /**
     * Snapshot the current [pointerHandlers] and run [block] on each one. May not be called
     * reentrant or concurrent with itself.
     *
     * Dispatches from first to last registered for [PointerEventPass.Initial] and
     * [PointerEventPass.Final]; dispatches from last to first for [PointerEventPass.Main]. This
     * corresponds to the down/up/down dispatch behavior of each of these passes along the hit test
     * path through the Compose UI layout hierarchy.
     */
    @OptIn(InternalCoroutinesApi::class)
    private inline fun forEachCurrentPointerHandler(
        pass: PointerEventPass,
        block: (PointerEventHandlerCoroutine<*>) -> Unit
    ) {
        // Copy handlers to avoid mutating the collection during dispatch
        dispatchingPointerHandlers.addAll(pointerHandlers)
        try {
            when (pass) {
                PointerEventPass.Initial,
                PointerEventPass.Final -> dispatchingPointerHandlers.forEach(block)
                PointerEventPass.Main -> dispatchingPointerHandlers.forEachReversed(block)
            }
        } finally {
            dispatchingPointerHandlers.clear()
        }
    }

    /**
     * Dispatch [pointerEvent] for [pass] to all [pointerHandlers] currently registered when the
     * call begins.
     */
    private fun dispatchPointerEvent(pointerEvent: PointerEvent, pass: PointerEventPass) {
        forEachCurrentPointerHandler(pass) { it.offerPointerEvent(pointerEvent, pass) }
    }

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize
    ) {
        boundsSize = bounds
        if (pass == PointerEventPass.Initial) {
            currentEvent = pointerEvent
        }

        // Coroutine lazily launches when first event comes in.
        if (pointerInputJob == null) {
            // 'start = CoroutineStart.UNDISPATCHED' required so handler doesn't miss first event.
            pointerInputJob =
                coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
                    if (_deprecatedPointerInputHandler != null) {
                        _deprecatedPointerInputHandler!!()
                    } else {
                        with(pointerInputEventHandler) { invoke() }
                    }
                }
        }

        dispatchPointerEvent(pointerEvent, pass)

        lastPointerEvent =
            pointerEvent.takeIf { event ->
                !event.changes.fastAll { it.changedToUpIgnoreConsumed() }
            }
    }

    override fun onCancelPointerInput() {
        // Synthesize a cancel event for whatever state we previously saw, if one is applicable.
        // A cancel event is one where all previously down pointers are now up, the change in
        // down-ness is consumed. Any pointers that were previously hovering are left unchanged.
        val lastEvent = lastPointerEvent ?: return

        if (lastEvent.changes.fastAll { !it.pressed }) {
            return // There aren't any pressed pointers, so we don't need to send any events.
        }
        val newChanges =
            lastEvent.changes.fastMapNotNull { old ->
                PointerInputChange(
                    id = old.id,
                    position = old.position,
                    uptimeMillis = old.uptimeMillis,
                    pressed = false,
                    pressure = old.pressure,
                    previousPosition = old.position,
                    previousUptimeMillis = old.uptimeMillis,
                    previousPressed = old.pressed,
                    isInitiallyConsumed = old.pressed,
                    type = old.type
                )
            }

        val cancelEvent = PointerEvent(newChanges)

        currentEvent = cancelEvent
        // Dispatch the synthetic cancel for all three passes
        dispatchPointerEvent(cancelEvent, PointerEventPass.Initial)
        dispatchPointerEvent(cancelEvent, PointerEventPass.Main)
        dispatchPointerEvent(cancelEvent, PointerEventPass.Final)

        lastPointerEvent = null
    }

    override suspend fun <R> awaitPointerEventScope(
        block: suspend AwaitPointerEventScope.() -> R
    ): R = suspendCancellableCoroutine { continuation ->
        val handlerCoroutine = PointerEventHandlerCoroutine(continuation)
            pointerHandlers += handlerCoroutine

            // NOTE: We resume the new continuation while holding this lock.
            // We do this since it runs in a RestrictsSuspension scope and therefore
            // will only suspend when awaiting a new event. We don't release this
            // synchronized lock until we know it has an awaiter and any future dispatch
            // would succeed.

            // We also create the coroutine with both a receiver and a completion continuation
            // of the handlerCoroutine itself; we don't use our currently available suspended
            // continuation as the resume point because handlerCoroutine needs to remove the
            // ContinuationInterceptor from the supplied CoroutineContext to have un-dispatched
            // behavior in our restricted suspension scope. This is required so that we can
            // process event-awaits synchronously and affect the next stage in the pipeline
            // without running too late due to dispatch.
            block.createCoroutine(handlerCoroutine, handlerCoroutine).resume(Unit)

        // Restricted suspension handler coroutines can't propagate structured job cancellation
        // automatically as the context must be EmptyCoroutineContext; do it manually instead.
        continuation.invokeOnCancellation { handlerCoroutine.cancel(it) }
    }

    override val density: Float
        get() = requireDensity().density
    override val fontScale: Float
        get() = requireDensity().fontScale

    /**
     * Implementation of the inner coroutine created to run a single call to
     * [awaitPointerEventScope].
     *
     * [PointerEventHandlerCoroutine] implements [AwaitPointerEventScope] to provide the input
     * handler DSL, and [Continuation] so that it can wrap [completion] and remove the
     * [ContinuationInterceptor] from the calling context and run un-dispatched.
     */
    private inner class PointerEventHandlerCoroutine<R>(
        private val completion: Continuation<R>,
    ) :
        AwaitPointerEventScope,
        Density by this@SharedSuspendingPointerInputModifierNodeImpl,
        Continuation<R> {

        private var pointerAwaiter: CancellableContinuation<PointerEvent>? = null
        private var awaitPass: PointerEventPass = PointerEventPass.Main

        override val currentEvent: PointerEvent
            get() = this@SharedSuspendingPointerInputModifierNodeImpl.currentEvent

        override val size: IntSize
            get() = this@SharedSuspendingPointerInputModifierNodeImpl.boundsSize

        override val viewConfiguration: ViewConfiguration
            get() = this@SharedSuspendingPointerInputModifierNodeImpl.viewConfiguration

        override val extendedTouchPadding: Size
            get() = this@SharedSuspendingPointerInputModifierNodeImpl.extendedTouchPadding

        fun offerPointerEvent(event: PointerEvent, pass: PointerEventPass) {
            if (pass == awaitPass) {
                pointerAwaiter?.run {
                    pointerAwaiter = null
                    resume(event)
                }
            }
        }

        // Called to run any finally blocks in the awaitPointerEventScope block
        fun cancel(cause: Throwable?) {
            pointerAwaiter?.cancel(cause)
            pointerAwaiter = null
        }

        // context must be EmptyCoroutineContext for restricted suspension coroutines
        override val context: CoroutineContext = EmptyCoroutineContext

        // Implementation of Continuation; clean up and resume our wrapped continuation.
        override fun resumeWith(result: Result<R>) {
             pointerHandlers -= this
            completion.resumeWith(result)
        }

        override suspend fun awaitPointerEvent(pass: PointerEventPass): PointerEvent =
            suspendCancellableCoroutine { continuation ->
                awaitPass = pass
                pointerAwaiter = continuation
            }

        override suspend fun <T> withTimeoutOrNull(
            timeMillis: Long,
            block: suspend AwaitPointerEventScope.() -> T
        ): T? {
            return try {
                withTimeout(timeMillis, block)
            } catch (_: PointerEventTimeoutCancellationException) {
                null
            }
        }

        override suspend fun <T> withTimeout(
            timeMillis: Long,
            block: suspend AwaitPointerEventScope.() -> T
        ): T {
            if (timeMillis <= 0L) {
                pointerAwaiter?.resumeWithException(
                    PointerEventTimeoutCancellationException(timeMillis)
                )
            }

            val job =
                coroutineScope.launch {
                    // Delay twice because the timeout continuation needs to be lower-priority than
                    // input events, not treated fairly in FIFO order. The second
                    // micro-delay reposts it to the back of the queue, after any input events
                    // that were posted but not processed during the first delay.
                    delay(timeMillis - 8L)
                    delay(8L)

                    pointerAwaiter?.resumeWithException(
                        PointerEventTimeoutCancellationException(timeMillis)
                    )
                }
            try {
                return block()
            } finally {
                job.cancel(CancellationException("Pointer input was reset"))
            }
        }
    }
}
