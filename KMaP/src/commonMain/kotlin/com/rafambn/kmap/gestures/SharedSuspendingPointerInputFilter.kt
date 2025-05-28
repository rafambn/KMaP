package com.rafambn.kmap.gestures

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.IntSize

fun Modifier.sharedPointerInput(block: suspend PointerInputScope.() -> Unit): Modifier =
    this then SharedPointerInputElement(pointerInputEventHandler = block)

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

    private var pointerInputNode: SuspendingPointerInputModifierNode =
        delegate(SuspendingPointerInputModifierNode(pointerInputEventHandler))

    override fun sharePointerInputWithSiblings(): Boolean = true

    fun update(pointerInputEventHandler: suspend PointerInputScope.() -> Unit) {
        pointerInputNode.resetPointerInputHandler()
        pointerInputNode = delegate(SuspendingPointerInputModifierNode(pointerInputEventHandler))
    }

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize
    ) {
        pointerInputNode.onPointerEvent(pointerEvent, pass, bounds)
    }

    override fun onCancelPointerInput() {
        pointerInputNode.onCancelPointerInput()
    }
}
