package com.rafambn.kmap.lazyMarker

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.pointer.PointerInputScope
import com.rafambn.kmap.components.Canvas
import com.rafambn.kmap.components.CanvasParameters
import com.rafambn.kmap.components.Cluster
import com.rafambn.kmap.components.ClusterParameters
import com.rafambn.kmap.components.Marker
import com.rafambn.kmap.components.MarkerParameters
import com.rafambn.kmap.components.Path
import com.rafambn.kmap.components.PathParameters
import com.rafambn.kmap.tiles.TileRenderResult

class KMaPContent(
    content: KMaPContent.() -> Unit,
) {

    val markers = mutableListOf<Marker>()
    val cluster = mutableListOf<Cluster>()
    val canvas = mutableListOf<Canvas>()
    val paths = mutableListOf<Path>()

    init {
        apply(content)
    }

    fun canvas(
        parameters: CanvasParameters = CanvasParameters(1F, 0F),
        maxCacheTiles: Int = 20,
        tileSource: suspend (zoom: Int, row: Int, column: Int) -> TileRenderResult,
        gestureDetection: (suspend PointerInputScope.() -> Unit)? = null
    ) {
        canvas.add(Canvas(parameters, maxCacheTiles, tileSource, gestureDetection))
    }

    inline fun <T : MarkerParameters> marker(
        marker: T,
        crossinline itemContent: @Composable (item: T) -> Unit
    ) {
        markers.add(Marker(marker, { itemContent(marker) }))
    }

    inline fun <T : MarkerParameters> markers(
        markers: List<T>,
        crossinline itemContent: @Composable (item: T, index: Int) -> Unit
    ) {
        markers.forEachIndexed { index, it ->
            this.markers.add(Marker(it, { itemContent(it, index) }))
        }
    }

    inline fun <T : ClusterParameters> cluster(
        cluster: T,
        crossinline itemContent: @Composable (item: T) -> Unit
    ) {
        this.cluster.add(Cluster(cluster, { itemContent(cluster) }))
    }

    fun path(
        parameters: PathParameters,
        gestureDetection: (suspend PointerInputScope.() -> Unit)? = null
    ) {
        paths.add(Path(parameters, gestureDetection))
    }
}
