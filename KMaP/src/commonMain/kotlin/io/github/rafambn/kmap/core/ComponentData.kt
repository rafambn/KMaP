package io.github.rafambn.kmap.core

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ParentDataModifierNode
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Density
import io.github.rafambn.kmap.utils.Degrees
import io.github.rafambn.kmap.utils.offsets.ScreenOffset

fun Modifier.componentData(componentData: MapComponentData) = this.then(ComponentDataElement(componentData = componentData))

private data class ComponentDataElement(
    private val componentData: MapComponentData
) : ModifierNodeElement<ComponentDataModifier>() {
    override fun create() = ComponentDataModifier(componentData)

    override fun update(node: ComponentDataModifier) {
        node.componentData = componentData
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "componentData"
        value = componentData
    }
}

internal class ComponentDataModifier(
    componentData: MapComponentData,
) : ParentDataModifierNode, ComponentDataParentData, Modifier.Node() {

    override var componentData: MapComponentData = componentData
        internal set

    override fun Density.modifyParentData(parentData: Any?): Any {
        return this@ComponentDataModifier
    }
}

interface ComponentDataParentData {
    val componentData: MapComponentData
}

val Measurable.componentData: MapComponentData
    get() = (parentData as ComponentDataParentData).componentData


data class MapComponentData(
    val position: ScreenOffset,
    val zIndex: Float,
    val drawPosition: DrawPosition,
    private val angle: Degrees,
    val componentType: ComponentType,
)

enum class ComponentType {
    CANVAS,
    PLACER
}
