package com.rafambn.kmap.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.PointerInputScope
import com.rafambn.kmap.components.canvas.tiled.TileRenderResult
import com.rafambn.kmap.components.marker.ClusterParameters
import com.rafambn.kmap.components.marker.MarkerParameters
import com.rafambn.kmap.components.path.PathParameter

interface KMaPScope {

    fun canvas(
        alpha: Float = 1F,
        zIndex: Float = 0F,
        maxCacheTiles: Int = 20,
        tileSource: suspend (zoom: Int, row: Int, column: Int) -> TileRenderResult,
        gestureDetection: (suspend PointerInputScope.() -> Unit)? = null
    )

    fun marker(
        markerParameters: MarkerParameters,
        markerContent: @Composable () -> Unit
    )

    fun markers(
        markerParameters: List<MarkerParameters>,
        markerContent: @Composable (index: Int) -> Unit
    )

    fun cluster(
        clusterParameters: ClusterParameters,
        clusterContent: @Composable () -> Unit
    )

    fun path(
        pathParameter: PathParameter,
        gestureDetection: (suspend PointerInputScope.(path: Path) -> Unit)? = null
    )

    fun paths(
        pathParameter: List<PathParameter>,
        gestureDetection: (suspend PointerInputScope.(path: Path) -> Unit)? = null
    )
}

inline fun <T : MarkerParameters> KMaPScope.markers(
    markers: List<T>,
    crossinline itemContent: @Composable (item: T) -> Unit
) = markers(
    markerParameters = markers
) {
    itemContent(markers[it])
}

inline fun <T : MarkerParameters> KMaPScope.marker(
    marker: T,
    crossinline itemContent: @Composable (item: T) -> Unit
) = marker(
    markerParameters = marker
) {
    itemContent(marker)
}

inline fun <T : ClusterParameters> KMaPScope.cluster(
    cluster: T,
    crossinline itemContent: @Composable (item: T) -> Unit
) = cluster(
    clusterParameters = cluster
) {
    itemContent(cluster)
}
