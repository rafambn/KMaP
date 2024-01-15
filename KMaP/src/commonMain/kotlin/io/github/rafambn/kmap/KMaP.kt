package io.github.rafambn.kmap

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun KMaP(
    modifier: Modifier = Modifier,
//    cameraState: CameraState,
//    mapProperties: MapProperties,
//    gestures: MapGestures,
    content: @Composable () -> Unit = {}
) {

    MapPlacements(modifier) {
        TileCanvas(Modifier, 200F)



    }
//    ZoomPanRotate(
//        modifier = modifier
//            .clipToBounds()
//            .background(state.mapBackground),
//        gestureListener = zoomPRState,
//        layoutSizeChangeListener = zoomPRState,
//    ) {
//        TileCanvas(
//            modifier = Modifier,
//            zoomPRState = zoomPRState,
//            visibleTilesResolver = state.visibleTilesResolver,
//            tileSize = state.tileSize,
//            alphaTick = state.tileCanvasState.alphaTick,
//            colorFilterProvider = state.tileCanvasState.colorFilterProvider,
//            tilesToRender = state.tileCanvasState.tilesToRender,
//            isFilteringBitmap = state.isFilteringBitmap,
//        )
//
//        MarkerComposer(
//            modifier = Modifier.zIndex(1f),
//            zoomPRState = zoomPRState,
//            markerRenderState = markerState,
//            mapState = state
//        )
//
//        PathComposer(
//            modifier = Modifier,
//            zoomPRState = zoomPRState,
//            pathState = pathState
//        )
//
//        content()
//    }
}