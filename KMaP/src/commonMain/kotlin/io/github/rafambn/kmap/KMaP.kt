package io.github.rafambn.kmap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import io.github.rafambn.kmap.gestures.GestureInterface
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ParentDataModifierNode
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.toSize
import io.github.rafambn.kmap.utils.CanvasPosition
import io.github.rafambn.kmap.utils.Degrees
import io.github.rafambn.kmap.utils.rotate
import io.github.rafambn.kmap.utils.rotateCentered
import io.github.rafambn.kmap.utils.toRadians
import kotlin.math.PI
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
                    .componentData(MapComponentData(Offset.Zero, 5F, DrawPosition.LEFT_TOP, 0.0, MapComponentType.CANVAS)),
                TileCanvasStateModel(
                    mapState.canvasSize / 2F,
                    mapState.angleDegrees.toFloat(),
                    mapState.magnifierScale,
                    mapState.tileCanvasState.tileLayers,
                    mapState.positionOffset,
                    mapState.zoomLevel,
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
        val canvasData: MapComponentData
        val canvasPlaceable = measurables
            .first { it.componentData.mapComponentType == MapComponentType.CANVAS }
            .also { canvasData = it.componentData }
            .measure(constraints)

        val markersData: List<MapComponentData>
        val markersPlaceable = measurables
            .filter { it.componentData.mapComponentType == MapComponentType.MARKER }
            .also { measurableMarkers -> markersData = measurableMarkers.map { it.componentData } }
            .map { it.measure(constraints) }

        val pathsData: List<MapComponentData>
        val pathsPlaceable = measurables
            .filter { it.componentData.mapComponentType == MapComponentType.PATH }
            .also { measurableMarkers -> pathsData = measurableMarkers.map { it.componentData } }
            .map { it.measure(constraints) }

        layout(constraints.maxWidth, constraints.maxHeight) {
            canvasPlaceable.place(
                x = 0,
                y = 0,
                zIndex = 0F
            )

            markersPlaceable.forEachIndexed { index, placeable ->
                placeable.place(
                    x = markersData[index].position.x.toInt(),
                    y = markersData[index].position.y.toInt(),
                    zIndex = markersData[index].zIndex
                )
            }

            pathsPlaceable.forEachIndexed { index, placeable ->
                placeable.placeRelativeWithLayer(
                    x = markersData[index].position.x.toInt(),
                    y = markersData[index].position.y.toInt(),
                    zIndex = pathsData[index].zIndex
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
    val mapComponentType: MapComponentType
)

interface KMaPScope {
    @Composable
    fun markers(items: List<MarkerPlacer>, markerContent: @Composable (MarkerPlacer) -> Unit) = items.forEach { item ->
        Layout(
            content = { markerContent(item) },
            modifier = Modifier
                .componentData(MapComponentData(item.coordinates, item.zIndex, item.drawPosition, item.angle, MapComponentType.MARKER)),
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
                            val center = Offset(
                                -(placeable.width * 1F) / 2,
                                -(placeable.height * 1F) / 2
                            )
                            val place = Offset.Zero.rotateCentered(center, item.angle.toRadians())
                            translationX = place.x
                            translationY = place.y
                            rotationZ = item.angle.toFloat()
                        }
                    }
                }
            }
        )
    }

    @Composable
    fun paths(items: List<PathPlacer>, pathContent: @Composable (PathPlacer) -> Unit) = items.forEach { item ->
        Layout(
            content = { pathContent(item) },
            modifier = Modifier
                .wrapContentSize()
                .componentData(MapComponentData(item.coordinates, item.zIndex, item.drawPosition, item.angle, MapComponentType.MARKER)),
            measurePolicy = { measurables, constraints ->
                val placeable = measurables.first().measure(constraints)
                layout(constraints.minWidth, constraints.minHeight) {
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
                            val center = Offset(
                                -(placeable.width * 1F) / 2,
                                -(placeable.height * 1F) / 2
                            )
                            val place = Offset.Zero.rotateCentered(center, item.angle.toRadians())
                            translationX = place.x
                            translationY = place.y
                            rotationZ = item.angle.toFloat()
                        }
                    }
                }
            }
        )
    }

    companion object : KMaPScope
}