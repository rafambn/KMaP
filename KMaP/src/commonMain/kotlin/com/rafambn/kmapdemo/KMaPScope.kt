package com.rafambn.kmapdemo

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import com.rafambn.kmapdemo.core.CanvasParameters
import com.rafambn.kmapdemo.core.ClusterData
import com.rafambn.kmapdemo.core.ClusterParameters
import com.rafambn.kmapdemo.core.MarkerData
import com.rafambn.kmapdemo.core.MarkerParameters
import com.rafambn.kmapdemo.core.state.MapState
import com.rafambn.kmapdemo.model.ResultTile
import kotlin.math.pow
import kotlin.math.sqrt

typealias Marker = Pair<MarkerParameters, @Composable (MarkerData) -> Unit>
typealias MarkerOutput = Pair<MarkerData, @Composable (MarkerData) -> Unit>
typealias Cluster = Pair<ClusterParameters, @Composable (ClusterData) -> Unit>
typealias ClusterOutput = Pair<ClusterData, @Composable (ClusterData) -> Unit>
typealias Canvas = Pair<CanvasParameters, suspend (zoom: Int, row: Int, column: Int) -> ResultTile>

class KMaPContent : KMaPConfig {
    private var mapState: MapState? = null
    private val markers = mutableListOf<Marker>()
    private val clusters = mutableListOf<Cluster>()
    val visibleCanvas = mutableListOf<Canvas>()
    val visibleMarkers = mutableListOf<MarkerOutput>()
    val visibleClusters = mutableListOf<ClusterOutput>()

    override fun markers(items: List<MarkerParameters>, markerContent: @Composable (MarkerData) -> Unit) {
        markers += items.map { it to markerContent }
    }

    override fun canvas(item: CanvasParameters, tileSource: suspend (zoom: Int, row: Int, column: Int) -> ResultTile) {
        visibleCanvas += item to tileSource
    }

    override fun cluster(item: ClusterParameters, clusterContent: @Composable (ClusterData) -> Unit) {
        clusters += item to clusterContent
    }

    fun updateCluster() {
        if (clusters.isEmpty() || mapState == null || markers.size < 2)
            return

        //clear visible markers
        visibleMarkers.clear()
        visibleClusters.clear()
        //show markers that do not map to clusters
        val clusterTags = clusters.map { it.first.tag }
        val markersPositions = markers.mapNotNull {
            val markerData = it.first.toMarkerData(
                with(mapState!!) {
                    it.first.coordinates.toCanvasPosition().toScreenOffset()
                }
            )
            if (clusterTags.contains(it.first.tag))
                Pair(markerData, it.second)
            else {
                visibleMarkers.add(Pair(markerData, it.second))
                null
            }
        }

        //for each cluster tag calculate the visible markers and clusters
        clusterTags.forEach { tag ->
            val markerWithTag = markersPositions.filter { it.first.tag == tag }
            val currentCluster = clusters.find { it.first.tag == tag }!!
            createClusters(markerWithTag, currentCluster.first.clusterThreshold, currentCluster).also { result ->
                visibleClusters.addAll(result.first)
                visibleMarkers.addAll(result.second)
            }
        }
        println(visibleMarkers)
    }

    fun setMap(mapState: MapState) {
        this.mapState = mapState
        if (clusters.isEmpty() || markers.size < 2)
            visibleMarkers.addAll(markers.map {
                Pair(it.first.toMarkerData(
                    with(mapState) {
                        it.first.coordinates.toCanvasPosition().toScreenOffset()
                    }
                ), it.second)
            })
    }
}

interface KMaPConfig {
    fun markers(items: List<MarkerParameters>, markerContent: @Composable (MarkerData) -> Unit)

    fun canvas(item: CanvasParameters = CanvasParameters(), tileSource: suspend (zoom: Int, row: Int, column: Int) -> ResultTile)

    fun cluster(item: ClusterParameters, clusterContent: @Composable (ClusterData) -> Unit)
}

fun distance(point1: Offset, point2: Offset): Float {
    val xDifference = point2.x - point1.x
    val yDifference = point2.y - point1.y
    return sqrt(xDifference.pow(2) + yDifference.pow(2))
}

fun createClusters(
    coordinates: List<MarkerOutput>,
    threshold: Dp,
    originalCluster: Cluster
): Pair<List<ClusterOutput>, List<MarkerOutput>> {
    val clusters = mutableListOf<ClusterOutput>()
    val visited = mutableSetOf<MarkerOutput>()
    val noise = mutableListOf<MarkerOutput>()

    for (coord in coordinates) {
        if (coord !in visited) {
            visited.add(coord)
            val neighbors = coordinates.filter {
                distance(
                    it.first.placementOffset,
                    coord.first.placementOffset
                ) <= threshold.value && !visited.contains(it)
            }
            if (neighbors.isNotEmpty())
                expandCluster(coord, neighbors, clusters, visited, originalCluster)
            else
                noise.add(coord)
        }
    }
    return Pair(clusters, noise)
}

private fun expandCluster(
    coord: MarkerOutput,
    neighbors: List<MarkerOutput>,
    cluster: MutableList<ClusterOutput>,
    visited: MutableSet<MarkerOutput>,
    originalCluster: Cluster
) {
    var size = 1
    var avgOffset = coord.first.placementOffset
    val queue = neighbors.toMutableList()
    while (queue.isNotEmpty()) {
        val current = queue.removeAt(0)
        visited.add(current)
        size++
        avgOffset += current.first.placementOffset
    }
    cluster.add(
        Pair(
            originalCluster.first.toClusterData(avgOffset / size.toFloat(), size),
            originalCluster.second
        )
    )
}