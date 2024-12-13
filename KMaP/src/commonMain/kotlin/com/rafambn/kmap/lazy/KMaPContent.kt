package com.rafambn.kmap.lazy

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.pointer.PointerInputScope
import com.rafambn.kmap.components.CanvasComponent
import com.rafambn.kmap.components.CanvasParameters
import com.rafambn.kmap.components.ClusterComponent
import com.rafambn.kmap.components.ClusterParameters
import com.rafambn.kmap.components.MarkerComponent
import com.rafambn.kmap.components.MarkerParameters
import com.rafambn.kmap.components.PathComponent
import com.rafambn.kmap.components.PathParameters
import com.rafambn.kmap.tiles.TileRenderResult

class KMaPContent(
    content: KMaPScope.() -> Unit,
) : KMaPScope {

    val markers = mutableListOf<MarkerComponent>()
    val cluster = mutableListOf<ClusterComponent>()
    val canvas = mutableListOf<CanvasComponent>()
    val paths = mutableListOf<PathComponent>()

    init {
        apply(content)
    }

    override fun canvas(
        canvasParameters: CanvasParameters,
        tileSource: suspend (zoom: Int, row: Int, column: Int) -> TileRenderResult,
        gestureDetection: (suspend PointerInputScope.() -> Unit)?
    ) {
        canvas.add(CanvasComponent(canvasParameters, tileSource, gestureDetection))
    }

    override fun marker(markerParameters: MarkerParameters, markerContent: @Composable (marker: MarkerParameters) -> Unit) {
        markers.add(MarkerComponent(markerParameters, markerContent))
    }

    override fun markers(markerParameters: List<MarkerParameters>, markerContent: @Composable (marker: MarkerParameters) -> Unit) {
        markerParameters.forEach {
            markers.add(MarkerComponent(it, markerContent))
        }
    }

    override fun cluster(clusterParameters: ClusterParameters, clusterContent: @Composable (cluster: ClusterParameters) -> Unit) {
        cluster.add(ClusterComponent(clusterParameters, clusterContent))
    }

    override fun path(pathParameters: PathParameters) {
        paths.add(PathComponent(pathParameters))
    }
}