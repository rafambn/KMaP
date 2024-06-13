package io.github.rafambn.kmap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.toSize
import io.github.rafambn.kmap.core.ComponentType
import io.github.rafambn.kmap.core.DrawPosition
import io.github.rafambn.kmap.core.MapComponentData
import io.github.rafambn.kmap.core.TileCanvas
import io.github.rafambn.kmap.core.componentData
import io.github.rafambn.kmap.gestures.GestureInterface
import io.github.rafambn.kmap.model.TileCanvasStateModel

@Composable
fun KMaP(
    modifier: Modifier = Modifier,
    tileCanvasStateModel: State<TileCanvasStateModel>,
    canvasGestureListener: GestureInterface,
    onCanvasChangeSize: (Offset) -> Unit,
    content: @Composable KMaPScope.() -> Unit = {}
) {
    Layout(
        content = {
            TileCanvas( //TODO make it a placer and receive a tileProvider
                Modifier
                    .componentData(MapComponentData(Offset.Zero, 0F, DrawPosition.TOP_LEFT, 0.0, ComponentType.CANVAS)),
                tileCanvasStateModel.value,
                canvasGestureListener
            )
            KMaPScope.content()
        },
        modifier
            .background(Color.Gray)
            .clipToBounds()
            .wrapContentSize()
            .onGloballyPositioned { coordinates ->
                onCanvasChangeSize(coordinates.size.toSize().toRect().bottomRight)
            }
    ) { measurables, constraints ->
        val canvasData: MapComponentData
        val canvasPlaceable = measurables
            .first { it.componentData.componentType == ComponentType.CANVAS }
            .also { canvasData = it.componentData }
            .measure(constraints)

        val placersData: List<MapComponentData>
        val placersPlaceable = measurables
            .filter { it.componentData.componentType == ComponentType.PLACER }
            .also { measurableMarkers -> placersData = measurableMarkers.map { it.componentData } }
            .map { it.measure(constraints) }

        layout(constraints.maxWidth, constraints.maxHeight) {
            canvasPlaceable.place(
                x = 0,
                y = 0,
                zIndex = canvasData.zIndex
            )
            placersPlaceable.forEachIndexed { index, placeable ->
                placeable.place(
                    x = placersData[index].position.x.toInt(),
                    y = placersData[index].position.y.toInt(),
                    zIndex = placersData[index].zIndex
                )
            }
        }
    }
}
//placeable.placeWithLayer(//TODO see if all this math can be avoid just by changing where things are calculated
//x = (-item.drawPosition.x * placeable.width + if (item.scaleWithMap) (1 - 1 / 2F.pow(item.zoom - item.zoomToFix)) * placeable.width / 2 else 0F).toInt(),
//y = (-item.drawPosition.y * placeable.height + if (item.scaleWithMap) (1 - 1 / 2F.pow(item.zoom - item.zoomToFix)) * placeable.height / 2 else 0F).toInt(),
//zIndex = item.zIndex
//) {
//    if (item.scaleWithMap) {
//        scaleX = 2F.pow(item.zoom - item.zoomToFix)
//        scaleY = 2F.pow(item.zoom - item.zoomToFix)
//    }
//    if (item.rotateWithMap) {
//        val center = CanvasPosition(
//            -(placeable.width) / 2.0,
//            -(placeable.height) / 2.0
//        )
//        val place = CanvasPosition.Zero.rotateCentered(center, item.angle.toRadians())
//        translationX = place.horizontal.toFloat()
//        translationY = place.vertical.toFloat()
//        rotationZ = item.angle.toFloat()
//    }
//}