package com.rafambn.kmap.lazy

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope.Companion.DefaultBlendMode
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.input.pointer.PointerInputScope
import com.rafambn.kmap.components.ClusterParameters
import com.rafambn.kmap.components.MarkerParameters
import com.rafambn.kmap.tiles.TileRenderResult
import com.rafambn.kmap.utils.Coordinates

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
        origin: Coordinates,
        path: Path,
        color: Color,
        zIndex: Float = 1F,
        alpha: Float = 1F,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode,
        gestureDetection: (suspend PointerInputScope.() -> Unit)? = null
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