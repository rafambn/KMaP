package io.github.rafambn.kmap

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layoutId

@Composable
fun KMaP(
    modifier: Modifier = Modifier,
    cameraState: CameraState,
    mapProperties: MapProperties
//    content: @Composable () -> Unit = {}
) {

    MotionManager(modifier, cameraState, mapProperties) {

        TileCanvas(Modifier.layoutId(MapComponentType.CANVAS), cameraState._tileSize.value)

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