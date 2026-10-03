package com.rafambn.kmap.gesture.internal

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.IntSize

fun Modifier.sharedPointerInput(block: suspend PointerInputScope.() -> Unit): Modifier =
    this then SharedPointerInputElement(pointerInputEventHandler = block)

/**
 * Shares input with siblings and combines fragmented multi-touch events.
 *
 * Compose's HitPathTracker merges only common prefixes of hit paths. The same modifier
 * reached through different paths can therefore receive separate batches of pointer changes.
 * With a small marker over a larger one, one finger can hit both while another hits only
 * the exposed part of the larger marker. The shared sibling then receives the fingers
 * separately, which breaks map gesture recognition. This reproduces on Compose 1.12.0/1.12.1.
 *
 * The node collects Main changes until Final, then delivers one synthetic Main event with
 * all received pointers. This workaround does not preserve Initial/Final delivery,
 * currentEvent updates, or all native event metadata. A replacement must still handle
 * the overlapping-marker case with two fingers.
 */
class SharedPointerInputElement(
    val pointerInputEventHandler: suspend PointerInputScope.() -> Unit,
) : ModifierNodeElement<SharedPointerInputModifierNodeImpl>() {
    override fun InspectorInfo.inspectableProperties() {
        name = "pointerInput"
        properties["pointerInputEventHandler"] = pointerInputEventHandler
    }

    override fun create(): SharedPointerInputModifierNodeImpl {
        return SharedPointerInputModifierNodeImpl(pointerInputEventHandler)
    }

    override fun update(node: SharedPointerInputModifierNodeImpl) {
        node.update(pointerInputEventHandler)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SharedPointerInputElement) return false
        return pointerInputEventHandler === other.pointerInputEventHandler
    }

    override fun hashCode(): Int {
        return pointerInputEventHandler.hashCode()
    }
}

class SharedPointerInputModifierNodeImpl(
    pointerInputEventHandler: suspend PointerInputScope.() -> Unit,
) : DelegatingNode(), PointerInputModifierNode {

    private val pointerInputNode: SuspendingPointerInputModifierNode =
        delegate(SuspendingPointerInputModifierNode(pointerInputEventHandler))

    override fun sharePointerInputWithSiblings(): Boolean = true

    fun update(pointerInputEventHandler: suspend PointerInputScope.() -> Unit) {
        pointerInputNode.pointerInputEventHandler = PointerInputEventHandler { pointerInputEventHandler() }
    }

    var initialCount = 0
    var mainCount = 0
    var pointerList = mutableListOf<PointerInputChange>()
    var previousFinal = false

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize
    ) {
        if (previousFinal && pass == PointerEventPass.Initial) {
            pointerList.clear()
            previousFinal = false
            initialCount = 0
            mainCount = 0
        }
        if (pass == PointerEventPass.Initial) {
            initialCount++
        }
        if (pass == PointerEventPass.Main) {
            pointerList.addAll(pointerEvent.changes)
            mainCount++
        }
        if (pass == PointerEventPass.Final && !previousFinal && initialCount == mainCount) {
            previousFinal = true
            pointerInputNode.onPointerEvent(PointerEvent(pointerList.toList()), PointerEventPass.Main, bounds)
        }
    }

    override fun onCancelPointerInput() {
        pointerInputNode.resetPointerInputHandler()
    }
}
