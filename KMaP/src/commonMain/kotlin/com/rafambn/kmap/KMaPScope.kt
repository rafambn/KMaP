package com.rafambn.kmap

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.pointer.PointerInputScope
import com.rafambn.kmap.core.Canvas
import com.rafambn.kmap.core.CanvasParameters
import com.rafambn.kmap.core.ClusterComponent
import com.rafambn.kmap.core.ClusterParameters
import com.rafambn.kmap.core.MarkerComponent
import com.rafambn.kmap.core.MarkerParameters
import com.rafambn.kmap.utils.TileRenderResult

interface KMaPScope //TODO brainStorm a path api

fun KMaPScope.cluster(
    clusterParameters: ClusterParameters,
    clusterContent: @Composable (cluster: ClusterParameters, size: Int) -> Unit
) {
    if (this is KMaPContent) {
        clusters.add(ClusterComponent(clusterParameters, clusterContent))
    }
}

fun KMaPScope.canvas(
    canvasParameters: CanvasParameters = CanvasParameters(),
    tileSource: suspend (zoom: Int, row: Int, column: Int) -> TileRenderResult,
    gestureDetection: (suspend PointerInputScope.() -> Unit)? = null
) {
    if (this is KMaPContent) {
        visibleCanvas.add(Canvas(canvasParameters, tileSource, gestureDetection))
    }
}

fun KMaPScope.marker(
    markerParameters: MarkerParameters,
    markerContent: @Composable (marker: MarkerParameters) -> Unit
) {
    if (this is KMaPContent) {
        markers.add(MarkerComponent(markerParameters, markerContent))
    }
}

fun KMaPScope.markers(
    markerParameters: MutableList<MarkerParameters>,
    markerContent: @Composable (marker: MarkerParameters) -> Unit
) {
    if (this is KMaPContent) {
        markerParameters.forEach {
            markers.add(MarkerComponent(it, markerContent))
        }
    }
}