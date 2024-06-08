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
                    .componentData(MapComponentData(Offset.Zero, 0F, DrawPosition.TOP_LEFT, 0.0)),
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
        val placersData: List<MapComponentData>
        val placersPlaceable = measurables
            .also { measurablePlacers -> placersData = measurablePlacers.map { it.componentData } }
            .map { it.measure(constraints) }

        layout(constraints.maxWidth, constraints.maxHeight) {
            placersPlaceable.forEachIndexed { index, placeable -> //TODO fix placers having 0 width and height
                placeable.place(
                    x = placersData[index].position.x.toInt(),
                    y = placersData[index].position.y.toInt(),
                    zIndex = placersData[index].zIndex
                )
            }
        }
    }
}