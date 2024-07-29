package io.github.rafambn.kmap.core

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ParentDataModifierNode
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Density

fun Modifier.componentInfo(componentInfo: MapComponentInfo) = this.then(ComponentDataElement(componentInfo = componentInfo))

private data class ComponentDataElement(
    private val componentInfo: MapComponentInfo
) : ModifierNodeElement<ComponentInfoModifier>() {
    override fun create() = ComponentInfoModifier(componentInfo)

    override fun update(node: ComponentInfoModifier) {
        node.componentInfo = componentInfo
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "componentInfo"
        value = componentInfo
    }
}

internal class ComponentInfoModifier(
    componentData: MapComponentInfo,
) : ParentDataModifierNode, ComponentInfoParentData, Modifier.Node() {

    override var componentInfo: MapComponentInfo = componentData
        internal set

    override fun Density.modifyParentData(parentData: Any?): Any {
        return this@ComponentInfoModifier
    }
}

interface ComponentInfoParentData {
    val componentInfo: MapComponentInfo
}

val Measurable.componentInfo: MapComponentInfo
    get() = (parentData as ComponentInfoParentData).componentInfo


data class MapComponentInfo(
    val data: Any,
    val type: ComponentType,
)

enum class ComponentType {
    CANVAS,
    CLUSTER,
    MARKER
}
