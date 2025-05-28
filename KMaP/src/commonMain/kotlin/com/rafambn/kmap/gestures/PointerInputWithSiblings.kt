package com.rafambn.kmap.gestures

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.unit.IntSize

class HoverReportingModifierNode(
    var onHover: (Offset) -> Unit
) : Modifier.Node(), PointerInputModifierNode {

    override fun sharePointerInputWithSiblings(): Boolean = true

    override fun onPointerEvent(event: PointerEvent, pass: PointerEventPass, bounds: IntSize) {
        if (event.type == PointerEventType.Enter || event.type == PointerEventType.Move) {
            val position = event.changes.firstOrNull()?.position
            position?.let { onHover(it) }
        }
    }

    override fun onCancelPointerInput() {
    }
}

class HoverReportingElement(
    private val onHover: (Offset) -> Unit
) : ModifierNodeElement<HoverReportingModifierNode>() {

    override fun create(): HoverReportingModifierNode {
        return HoverReportingModifierNode(onHover)
    }

    override fun update(node: HoverReportingModifierNode) {
        node.onHover = onHover
    }

    override fun hashCode(): Int = onHover.hashCode()

    override fun equals(other: Any?): Boolean {
        return other is HoverReportingElement && other.onHover == onHover
    }
}

fun Modifier.hoverReporting(onHover: (Offset) -> Unit): Modifier {
    return this.then(HoverReportingElement(onHover))
}

