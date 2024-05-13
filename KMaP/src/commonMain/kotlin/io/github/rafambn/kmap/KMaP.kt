package io.github.rafambn.kmap

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layoutId
import io.github.rafambn.kmap.gestures.GestureInterface

@Composable
fun KMaP(
    modifier: Modifier = Modifier,
    mapState: MapState,
    canvasGestureListener: GestureInterface = DefaultCanvasGestureListener(mapState)
) {
    Placer(modifier, { mapState.onCanvasSizeChanged(it) }) {
        TileCanvas(
            Modifier.layoutId(MapComponentType.CANVAS),
            mapState.canvasSize / 2F,
            mapState.angleDegrees,
            mapState.magnifierScale,
            mapState.tileCanvasState.visibleTilesList.toList(),
            mapState.positionOffset,
            mapState.state,
            canvasGestureListener
        )
    }
}