package com.rafambn.kmap.core

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.layout.LazyLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import com.rafambn.kmap.components.canvas.tiled.TileCanvas
import com.rafambn.kmap.components.KMaPScope
import com.rafambn.kmap.components.marker.rememberMarkerMeasurePolicy
import com.rafambn.kmap.components.marker.MarkerProvider
import com.rafambn.kmap.components.path.PathProvider
import com.rafambn.kmap.components.path.rememberPathMeasurePolicy
import com.rafambn.kmap.components.rememberComponentProviderLambda

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KMaP(
    modifier: Modifier = Modifier,
    mapState: MapState,
    content: KMaPScope.() -> Unit
) {
    val itemProvider = rememberComponentProviderLambda(content, mapState)

    itemProvider.invoke().canvas.forEach {
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

    val markerMeasurePolicy = rememberMarkerMeasurePolicy(
        markerProviderLambda = { MarkerProvider(itemProvider.invoke()) },
        mapState = mapState,
    )

    val pathMeasurePolicy = rememberPathMeasurePolicy(
        pathProviderLambda = { PathProvider(itemProvider.invoke()) },
        mapState = mapState,
    )

    LazyLayout(
        itemProvider = { MarkerProvider(itemProvider.invoke()) },
        modifier = modifier.clipToBounds(),
        prefetchState = null,
        measurePolicy = markerMeasurePolicy
    )

    LazyLayout(
        itemProvider = { PathProvider(itemProvider.invoke()) },
        modifier = modifier.clipToBounds(),
        prefetchState = null,
        measurePolicy = pathMeasurePolicy
    )
}
