package io.github.rafambn.kmap

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
            Modifier.layoutId(MapComponentType.CANVAS),
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
    ) = renderItems(items, markerContent, MapComponentType.MARKER)

    @Composable
    override fun paths(
        items: List<PathPlacer>,
        pathContent: @Composable (PathPlacer) -> Unit
    ) = renderItems(items, pathContent, MapComponentType.PATH)

    @Composable
    private fun <T> renderItems(
        items: List<T>,
        content: @Composable (T) -> Unit,
        mapComponentType: MapComponentType
    ) = items.forEach { item ->
        Layout(
            content = { content(item) },
            modifier = Modifier.layoutId(mapComponentType),
            measurePolicy = { measurables, constraints ->
                val placeableList = measurables.map { it.measure(constraints) }
                layout(constraints.maxWidth, constraints.maxHeight) {
                    placeableList.forEach { it.placeRelativeWithLayer(0, 0) }
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