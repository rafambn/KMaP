package io.github.rafambn.kmap

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import io.github.rafambn.kmap.core.CanvasData
import io.github.rafambn.kmap.core.ClusterCondition
import io.github.rafambn.kmap.core.ClusterData
import io.github.rafambn.kmap.core.MarkerData
import io.github.rafambn.kmap.core.state.MapState
import io.github.rafambn.kmap.model.ResultTile
import io.github.rafambn.kmap.utils.offsets.ScreenOffset
import kotlin.math.pow
import kotlin.math.sqrt

typealias Marker = Pair<MarkerData, @Composable (MarkerData) -> Unit>
typealias MarkerWithCoordinates = Pair<Marker, ScreenOffset>
typealias Cluster = Pair<ClusterCondition, @Composable (ClusterData) -> Unit>
typealias ClusterOutput = Pair<ClusterData, @Composable (ClusterData) -> Unit>
typealias Canvas = Pair<CanvasData, suspend (zoom: Int, row: Int, column: Int) -> ResultTile>

class KMaPContent : KMaPConfig {
    private var mapState: MapState? = null
    private val markers = mutableListOf<Marker>()
    private val clusters = mutableListOf<Cluster>()
    val visibleCanvas = mutableListOf<Canvas>()
    val visibleMarkers = mutableListOf<Marker>()
    val visibleClusters = mutableListOf<ClusterOutput>()

    override fun markers(items: List<MarkerData>, markerContent: @Composable (MarkerData) -> Unit) {
        markers += items.map { it to markerContent }
    }

    override fun canvas(item: CanvasData, getTile: suspend (zoom: Int, row: Int, column: Int) -> ResultTile) {
        visibleCanvas += item to getTile
    }

    override fun cluster(item: ClusterCondition, markerContent: @Composable (ClusterData) -> Unit) {
        clusters += item to markerContent
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
            if (clusterTags.contains(it.first.tag))
                it to with(mapState!!) {
                    (it.first).coordinates.toCanvasPosition().toScreenOffset()
                }
            else {
                visibleMarkers.add(it)
                null
            }
        }
        //for each cluster tag calculate the visible markers and clusters
        clusterTags.forEach { tag ->
            val markerWithTag = markersPositions.filter { it.first.first.tag == tag }.toMutableList()
            val currentCluster = clusters.find { it.first.tag == tag }!!
            createClusters(markerWithTag, currentCluster.first.clusterThreshold, currentCluster).also { result ->
                visibleClusters.addAll(result.first)
                visibleMarkers.addAll(result.second.map { it.first })
            }
        }
            println(visibleMarkers)
    }

    fun setMap(mapState: MapState) {
        this.mapState = mapState
        if (clusters.isEmpty() || markers.size < 2)
            visibleMarkers.addAll(markers)
    }
}

interface KMaPConfig {
    fun markers(items: List<MarkerData>, markerContent: @Composable (MarkerData) -> Unit)

    fun canvas(item: CanvasData, getTile: suspend (zoom: Int, row: Int, column: Int) -> ResultTile)

    fun cluster(item: ClusterCondition, markerContent: @Composable (ClusterData) -> Unit)
}

fun distance(point1: Offset, point2: Offset): Float {
    val xDifference = point2.x - point1.x
    val yDifference = point2.y - point1.y
    return sqrt(xDifference.pow(2) + yDifference.pow(2))
}

fun createClusters(
    coordinates: List<MarkerWithCoordinates>,
    threshold: Dp,
    originalCluster: Cluster
): Pair<List<ClusterOutput>, List<MarkerWithCoordinates>> {
    val clusters = mutableListOf<ClusterOutput>()
    val visited = mutableSetOf<MarkerWithCoordinates>()
    val noise = mutableListOf<MarkerWithCoordinates>()

    for (coord in coordinates) {
        if (coord !in visited) {
            visited.add(coord)
            val neighbors = coordinates.filter { distance(it.second, coord.second) <= threshold.value && !visited.contains(it) }
            if (neighbors.size >= 2)
                expandCluster(coord, neighbors, clusters, visited, originalCluster)
            else
                noise.add(coord)
        }
    }
    return Pair(clusters, noise)
}

private fun expandCluster(
    coord: MarkerWithCoordinates,
    neighbors: List<MarkerWithCoordinates>,
    cluster: MutableList<ClusterOutput>,
    visited: MutableSet<MarkerWithCoordinates>,
    originalCluster: Cluster
) {
    var size = 1
    var avgOffset = coord.second
    val queue = neighbors.toMutableList()
    while (queue.isNotEmpty()) {
        val current = queue.removeAt(0)
        visited.add(current)
        size++
        avgOffset += current.second
    }
    cluster.add(
        Pair(
            ClusterData(
                originalCluster.first.tag,
                originalCluster.first.clusterThreshold,
                originalCluster.first.alpha,
                originalCluster.first.zIndex,
                avgOffset / size.toFloat(),
                size
            ),
            originalCluster.second
        )
    )
}