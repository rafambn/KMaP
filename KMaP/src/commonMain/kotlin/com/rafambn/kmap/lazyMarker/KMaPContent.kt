package com.rafambn.kmap.lazyMarker

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.pointer.PointerInputScope
import com.rafambn.kmap.components.CanvasComponent
import com.rafambn.kmap.components.CanvasParameters
import com.rafambn.kmap.components.ClusterComponent
import com.rafambn.kmap.components.ClusterParameters
import com.rafambn.kmap.components.MarkerComponent
import com.rafambn.kmap.components.MarkerParameters
import com.rafambn.kmap.tiles.TileRenderResult

class KMaPContent(
    content: KMaPScope.() -> Unit,
) : KMaPScope {

    val markers = mutableListOf<MarkerComponent>()
    val cluster = mutableListOf<ClusterComponent>()
    val canvas = mutableListOf<CanvasComponent>()

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

    override fun cluster(clusterParameters: ClusterParameters, clusterContent: @Composable (cluster: ClusterParameters, size: Int) -> Unit) {
        cluster.add(ClusterComponent(clusterParameters, clusterContent))
    }
}

//class KMaPContent(
//    content: KMaPScope.() -> Unit,
//) : KMaPScope {
//    val markers = mutableListOf<MarkerComponent>()
//    val clusters = mutableListOf<ClusterComponent>()
//    val canvas = mutableListOf<CanvasComponent>()
//    val visibleCanvas = mutableListOf<Canvas>()
//    val visibleMarkers = mutableListOf<Marker>()
//    val visibleClusters = mutableListOf<Cluster>()
//
//    init {
//        apply(content)
//        visibleCanvas.addAll(canvas.map { Canvas(it.canvasParameters, it.getTile, it.gestureDetection) })
//    }
//}
//
//    fun updateCluster(mapState: MapState) {//TODO improve this code to account for draw position
//        //clear visible markers
//        visibleMarkers.clear()
//        visibleClusters.clear()
//        //show markers that do not map to clusters
//        val clusterTags = clusters.map { it.clusterParameters.tag }
//        val markersPositions = markers.mapNotNull {
//            val markerData = it
//            val markerPlacemente = with(mapState) {
//                it.markerParameters.coordinates.toCanvasPosition().toScreenOffset()
//            }
//            if (clusterTags.contains(it.markerParameters.tag))
//                Marker(markerData.markerParameters, markerPlacemente, markerData.markerContent)
//            else {
//                visibleMarkers.add(Marker(markerData.markerParameters, markerPlacemente, markerData.markerContent))
//                null
//            }
//        }
//
//        //for each cluster tag calculate the visible markers and clusters
//        clusterTags.forEach { tag ->
//            val markerWithTag = markersPositions.filter { it.markerParameters.tag == tag }
//            val currentCluster = clusters.find { it.clusterParameters.tag == tag }!!
//            createClusters(markerWithTag, currentCluster).also { result ->
//                visibleClusters.addAll(result.first)
//                visibleMarkers.addAll(result.second)
//            }
//        }
//    }
//
//    private fun distance(point1: ScreenOffset, point2: ScreenOffset): Float {
//        val xDifference = point2.x - point1.x
//        val yDifference = point2.y - point1.y
//        return sqrt(xDifference.pow(2) + yDifference.pow(2))
//    }
//
//    private fun createClusters(
//        coordinates: List<Marker>,
//        originalCluster: ClusterComponent
//    ): Pair<List<Cluster>, List<Marker>> {
//        val threshold = originalCluster.clusterParameters.clusterThreshold.value
//        val clusters = mutableListOf<Cluster>()
//        val visited = mutableSetOf<Marker>()
//        val noise = mutableListOf<Marker>()
//
//        for (coord in coordinates) {
//            if (coord !in visited) {
//                visited.add(coord)
//                val neighbors = coordinates.filter {
//                    distance(
//                        it.placementOffset,
//                        coord.placementOffset
//                    ) <= threshold && !visited.contains(it)
//                }
//                if (neighbors.isNotEmpty())
//                    expandCluster(coord, neighbors, clusters, visited, originalCluster)
//                else
//                    noise.add(coord)
//            }
//        }
//        return Pair(clusters, noise)
//    }
//
//    private fun expandCluster(
//        coord: Marker,
//        neighbors: List<Marker>,
//        cluster: MutableList<Cluster>,
//        visited: MutableSet<Marker>,
//        originalCluster: ClusterComponent
//    ) {
//        var size = 1
//        var avgOffset = coord.placementOffset
//        val queue = neighbors.toMutableList()
//        while (queue.isNotEmpty()) {
//            val current = queue.removeAt(0)
//            visited.add(current)
//            size++
//            avgOffset += current.placementOffset
//        }
//        cluster.add(
//            Cluster(
//                originalCluster.clusterParameters,
//                avgOffset / size.toFloat(),
//                size,
//                originalCluster.clusterContent
//            )
//        )
//    }
//}