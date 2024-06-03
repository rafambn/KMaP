package io.github.rafambn.kmap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import io.github.rafambn.kmap.gestures.GestureInterface
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ParentDataModifierNode
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.toSize
import io.github.rafambn.kmap.utils.CanvasPosition
import io.github.rafambn.kmap.utils.Degrees
import io.github.rafambn.kmap.utils.Position
import io.github.rafambn.kmap.utils.rotateCentered
import io.github.rafambn.kmap.utils.toPosition
import io.github.rafambn.kmap.utils.toRadians
import kotlin.math.pow

@Composable
fun KMaP(
    modifier: Modifier = Modifier,
    mapState: MapState,
    canvasGestureListener: GestureInterface = DefaultCanvasGestureListener(mapState),
    content: @Composable KMaPScope.() -> Unit = {}
) {
    Layout(
        content = {
            TileCanvas(
                Modifier
                    .componentData(MapComponentData(Position.Zero, 0F, DrawPosition.LEFT_TOP, 0.0)),
                TileCanvasStateModel(
                    mapState.canvasSize / 2F,
                    mapState.angleDegrees.toFloat(),
                    mapState.magnifierScale,
                    mapState.tileCanvasState.tileLayers,
                    mapState.positionOffset,
                    mapState.zoomLevel,
                    mapState.mapProperties.tileSize
                ),
                mapState.state,
                canvasGestureListener
            )
            KMaPScope.content()
        },
        modifier
            .background(Color.Gray)
            .clipToBounds()
            .wrapContentSize()
            .onGloballyPositioned { coordinates ->
                mapState.onCanvasSizeChanged(coordinates.size.toSize().toRect().bottomRight)
            }
    ) { measurables, constraints ->
        val placersData: List<MapComponentData>
        val placersPlaceable = measurables
            .also { measurablePlacers -> placersData = measurablePlacers.map { it.componentData } }
            .map { it.measure(constraints) }


        layout(constraints.maxWidth, constraints.maxHeight) {
            placersPlaceable.forEachIndexed { index, placeable ->
                placeable.place(
                    x = placersData[index].position.horizontal.toInt(),
                    y = placersData[index].position.vertical.toInt(),
                    zIndex = placersData[index].zIndex
                )
            }
        }
    }
}

@Stable
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
    val position: CanvasPosition,
    val zIndex: Float,
    val drawPosition: DrawPosition,
    private val angle: Degrees,
)

interface KMaPScope {
    @Composable
    fun placers(items: List<MarkerPlacer>, markerContent: @Composable (MarkerPlacer) -> Unit) = items.forEach { item -> //TODO add gesture input to this
        Layout(
            content = { markerContent(item) },
            modifier = Modifier
                .componentData(MapComponentData(item.coordinates.toPosition(), item.zIndex, item.drawPosition, item.angle)),
            measurePolicy = { measurables, constraints ->
                val placeable = measurables.first().measure(constraints)
                layout(constraints.minWidth, constraints.minHeight) {//TODO improve this math
                    placeable.placeWithLayer(
                        x = (-item.drawPosition.x * placeable.width + (1 - 1 / 2F.pow(item.zoom - item.zoomToFix)) * placeable.width / 2).toInt(),
                        y = (-item.drawPosition.y * placeable.height + (1 - 1 / 2F.pow(item.zoom - item.zoomToFix)) * placeable.height / 2).toInt(),
                        zIndex = item.zIndex
                    ) {
                        if (item.scaleWithMap) {
                            scaleX = 2F.pow(item.zoom - item.zoomToFix)
                            scaleY = 2F.pow(item.zoom - item.zoomToFix)
                        }
                        if (item.rotateWithMap) {
                            val center = Position(
                                -(placeable.width) / 2.0,
                                -(placeable.height) / 2.0
                            )
                            val place = Position.Zero.rotateCentered(center, item.angle.toRadians())
                            translationX = place.horizontal.toFloat()
                            translationY = place.vertical.toFloat()
                            rotationZ = item.angle.toFloat()
                        }
                    }
                }
            }
        )
    }
    companion object : KMaPScope
}