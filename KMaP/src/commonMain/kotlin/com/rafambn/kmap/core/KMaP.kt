package com.rafambn.kmap.core

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.layout.LazyLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import com.rafambn.kmap.tiles.TileCanvas
import com.rafambn.kmap.lazy.KMaPScope
import com.rafambn.kmap.lazy.rememberComponentProviderLambda
import com.rafambn.kmap.lazy.rememberComponentMeasurePolicy
import com.rafambn.kmap.path.PathCanvas

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KMaP(
    modifier: Modifier = Modifier,
    motionController: MotionController,
    mapState: MapState,
    content: KMaPScope.() -> Unit
) {
    val density = LocalDensity.current
    LaunchedEffect(Unit) {
        motionController.setMap(mapState)
        mapState.setDensity(density)
    }
    val itemProvider = rememberComponentProviderLambda(content, mapState)

    itemProvider.invoke().canvasList.forEach {
        TileCanvas(
            getTile = it.getTile,
            cameraState = mapState.cameraState,
            mapProperties = mapState.mapProperties,
            positionOffset = mapState.drawReference,
            boundingBox = mapState.boundingBox,
            canvasParameters = it.canvasParameters,
            gestureDetector = it.gestureDetection,
            modifier = modifier
                .onGloballyPositioned { coordinates ->
                    mapState.setCanvasSize(
                        Offset(
                            coordinates.size.width.toFloat(),
                            coordinates.size.height.toFloat()
                        )
                    )
                },
        )
    }

    val measurePolicy = rememberComponentMeasurePolicy(
        componentProviderLambda = itemProvider,
        mapState = mapState,
    )

    LazyLayout(
        itemProvider = itemProvider,
        modifier = modifier.clipToBounds(),
        prefetchState = null,
        measurePolicy = measurePolicy
    )

    itemProvider.invoke().pathList.forEach {
        PathCanvas(
            cameraState = mapState.cameraState,
            mapProperties = mapState.mapProperties,
            positionOffset = mapState.drawReference,
            pathParameters = it.pathParameters,
            modifier = modifier
        )
    }
}