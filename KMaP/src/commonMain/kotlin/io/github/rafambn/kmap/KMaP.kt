package io.github.rafambn.kmap

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layoutId

@Composable
fun KMaP(
    modifier: Modifier = Modifier,
    mapState: MapState,
) {
    MotionManager(modifier, mapState) {
        TileCanvas(
            Modifier.layoutId(MapComponentType.CANVAS),
            mapState.state,
            mapState.boundingBox,
            mapState.zoomLevel,
            mapState.mapProperties.mapCoordinatesRange,
            mapState.mapProperties.outsideTiles,
            mapState.matrix,
            mapState.positionOffset,
            mapState.updateState()
        )
    }
}