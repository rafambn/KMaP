package com.rafambn.kmap.core

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.pointer.PointerInputScope
import com.rafambn.kmap.components.Canvas
import com.rafambn.kmap.components.CanvasComponent
import com.rafambn.kmap.components.CanvasParameters
import com.rafambn.kmap.components.ClusterComponent
import com.rafambn.kmap.components.ClusterParameters
import com.rafambn.kmap.components.MarkerComponent
import com.rafambn.kmap.components.MarkerParameters
import com.rafambn.kmap.tiles.TileRenderResult

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
        canvas.add(CanvasComponent(canvasParameters, tileSource, gestureDetection))
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
    markerParameters: List<MarkerParameters>,
    markerContent: @Composable (marker: MarkerParameters) -> Unit
) {
    if (this is KMaPContent) {
        markerParameters.forEach {
            markers.add(MarkerComponent(it, markerContent))
        }
    }
}