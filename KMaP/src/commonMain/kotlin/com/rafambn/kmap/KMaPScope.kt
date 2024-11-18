package com.rafambn.kmap

import androidx.compose.runtime.Composable
import com.rafambn.kmap.core.Canvas
import com.rafambn.kmap.core.CanvasParameters
import com.rafambn.kmap.core.ClusterComponent
import com.rafambn.kmap.core.ClusterParameters
import com.rafambn.kmap.core.MarkerComponent
import com.rafambn.kmap.core.MarkerParameters
import com.rafambn.kmap.model.ResultTile

interface KMaPScope //TODO brainStorm a path api

inline fun KMaPScope.cluster(
    clusterParameters: ClusterParameters,
    noinline clusterContent: @Composable (cluster: ClusterParameters, size: Int) -> Unit
) {
    if (this is KMaPContent) {
        clusters.add(ClusterComponent(clusterParameters, clusterContent))
    }
}

inline fun KMaPScope.canvas(
    canvasParameters: CanvasParameters = CanvasParameters(),
    noinline tileSource: suspend (zoom: Int, row: Int, column: Int) -> ResultTile
) {
    if (this is KMaPContent) {
        visibleCanvas.add(Canvas(canvasParameters, tileSource))
    }
}

inline fun KMaPScope.marker(
    markerParameters: MarkerParameters,
    noinline markerContent: @Composable (marker: MarkerParameters) -> Unit
) {
    if (this is KMaPContent) {
        markers.add(MarkerComponent(markerParameters, markerContent))
    }
}

inline fun KMaPScope.markers(
    markerParameters: MutableList<MarkerParameters>,
    noinline markerContent: @Composable (marker: MarkerParameters) -> Unit
) {
    if (this is KMaPContent) {
        markerParameters.forEach {
            markers.add(MarkerComponent(it, markerContent))
        }
    }
}