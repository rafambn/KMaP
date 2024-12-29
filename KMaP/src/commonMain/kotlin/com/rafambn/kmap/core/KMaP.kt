package com.rafambn.kmap.core

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.layout.LazyLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import com.rafambn.kmap.tiles.TileCanvas
import com.rafambn.kmap.lazy.KMaPScope
import com.rafambn.kmap.lazy.rememberComponentProviderLambda
import com.rafambn.kmap.lazy.rememberComponentMeasurePolicy
import com.rafambn.kmap.path.PathCanvas

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KMaP(
    modifier: Modifier = Modifier,
    mapState: MapState,
    content: KMaPScope.() -> Unit
) {
    val itemProvider = rememberComponentProviderLambda(content, mapState)

    itemProvider.invoke().canvasList.forEach {
        TileCanvas(
            cameraState = mapState.cameraState,
            mapProperties = mapState.mapProperties,
            positionOffset = mapState.drawReference,
            viewPort = mapState.viewPort,
            canvasComponent = it,
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
            pathComponent = it,
            modifier = modifier,
            mapState = mapState
        )
    }
}