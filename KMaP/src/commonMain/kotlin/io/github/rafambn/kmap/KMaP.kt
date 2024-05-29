package io.github.rafambn.kmap

import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import io.github.rafambn.kmap.gestures.GestureInterface
import androidx.compose.ui.layout.layoutId

@Composable
fun KMaP(
    modifier: Modifier = Modifier,
    mapState: MapState,
    canvasGestureListener: GestureInterface = DefaultCanvasGestureListener(mapState),
    content: @Composable KMaPScope.() -> Unit = {}
) {
    Placer(modifier, { mapState.onCanvasSizeChanged(it) }) {
        TileCanvas(
            Modifier.layoutId(MapComponent(Position.Zero, 0F, DrawPosition.LEFT_TOP, MapComponentType.CANVAS)),
            TileCanvasStateModel(
                mapState.canvasSize / 2F,
                mapState.angleDegrees,
                mapState.magnifierScale,
                mapState.tileCanvasState.tileLayers,
                mapState.positionOffset,
                mapState.zoomLevel,
            ),
            mapState.state,
            canvasGestureListener
        )
        KMaPScopeImpl().apply { content() }
    }
}

class KMaPScopeImpl : KMaPScope {
    @Composable
    override fun markers(
        items: List<MarkerPlacer>,
        markerContent: @Composable (MarkerPlacer) -> Unit
    ) = items.forEach { item ->
        Layout(
            content = { markerContent(item) },
            modifier = Modifier.layoutId(MapComponent(item.coordinates, item.zIndex, item.drawPosition, MapComponentType.MARKER)),
            measurePolicy = { measurables, constraints ->
                val placeable = measurables.first()
                val measuredSize = placeable.measure(constraints.copy(minHeight = 0, minWidth = 0))

                layout(constraints.maxWidth, constraints.maxHeight) {
                    measuredSize.placeRelativeWithLayer(
                        x = (item.coordinates.horizontal - item.drawPosition.x * measuredSize.width).toInt(),
                        y = (item.coordinates.vertical - item.drawPosition.y * measuredSize.height).toInt(),
                        zIndex = item.zIndex
                    )
                }
            }
        )
    }

    @Composable
    override fun paths(
        items: List<PathPlacer>,
        pathContent: @Composable (PathPlacer) -> Unit
    ) = items.forEach { item ->
        Layout(
            content = { pathContent(item) },
            modifier = Modifier
                .layoutId(MapComponent(item.coordinates, item.zIndex, item.drawPosition, MapComponentType.PATH))
                .wrapContentSize(),
            measurePolicy = { measurables, constraints ->
                val placeable = measurables.first()
                val measuredSize = placeable.measure(constraints.copy(minHeight = 0, minWidth = 0))

                layout(constraints.maxWidth, constraints.maxHeight) {
                    measuredSize.placeRelativeWithLayer(
                        x = (item.coordinates.horizontal - item.drawPosition.x * measuredSize.width).toInt(),
                        y = (item.coordinates.vertical - item.drawPosition.y * measuredSize.height).toInt(),
                        zIndex = item.zIndex
                    )
                }
            }
        )
    }
}

interface KMaPScope {
    @Composable
    fun markers(items: List<MarkerPlacer>, markerContent: @Composable (MarkerPlacer) -> Unit)

    @Composable
    fun paths(items: List<PathPlacer>, pathContent: @Composable (PathPlacer) -> Unit)
}