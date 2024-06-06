package io.github.rafambn.kmap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
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
import io.github.rafambn.kmap.core.state.MapState
import io.github.rafambn.kmap.gestures.DefaultCanvasGestureListener
import io.github.rafambn.kmap.gestures.GestureInterface
import io.github.rafambn.kmap.model.TileCanvasStateModel

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
                    .componentData(MapComponentData(Offset.Zero, 0F, DrawPosition.TOP_LEFT, 0.0)),
                TileCanvasStateModel( //TODO make it a flow
                    mapState.canvasSize / 2F,
                    mapState.angleDegrees.toFloat(),
                    mapState.magnifierScale,
                    mapState.tileCanvasState.tileLayers,
                    mapState.positionOffset,
                    mapState.zoomLevel,
                    mapState.mapProperties.mapSource.tileSize
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
                    x = placersData[index].position.x.toInt(),
                    y = placersData[index].position.y.toInt(),
                    zIndex = placersData[index].zIndex
                )
            }
        }
    }
}