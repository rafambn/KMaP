package com.rafambn.kmap.lazy

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.layout.LazyLayoutMeasureScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.util.fastForEach
import com.rafambn.kmap.core.MapState
import com.rafambn.kmap.core.ViewPort
import com.rafambn.kmap.core.isViewPortIntersecting
import com.rafambn.kmap.utils.ScreenOffset

@ExperimentalFoundationApi
@Composable
fun rememberComponentMeasurePolicy(
    componentProviderLambda: () -> ComponentProvider,
    mapState: MapState,
) = remember<LazyLayoutMeasureScope.(Constraints) -> MeasureResult>(
    mapState,
) {
    { containerConstraints ->
        val componentProvider = componentProviderLambda()

        val itemsCount = componentProvider.itemCount

        val measuredItemProvider = MeasuredComponentProvider(componentProvider,this)

        measureComponent(
            itemsCount = itemsCount,
            measuredItemProvider = measuredItemProvider,
            mapState = mapState,
            constraints = containerConstraints,
            layout = { width, height, placement ->
                layout(
                    containerConstraints.constrainWidth(width),
                    containerConstraints.constrainHeight(height),
                    emptyMap(),
                    placement
                )
            }
        )
    }
}

internal fun measureComponent(
    itemsCount: Int,
    measuredItemProvider: MeasuredComponentProvider,
    mapState: MapState,
    constraints: Constraints,
    layout: (Int, Int, Placeable.PlacementScope.() -> Unit) -> MeasureResult
): MeasureResult {
    val layoutWidth = constraints.maxWidth
    val layoutHeight = constraints.maxHeight

    if (itemsCount <= 0) {
        return layout(layoutWidth, layoutHeight) {}
    } else {

        val visibleItems = ArrayDeque<MeasuredComponent>()

        val measuredItem = mutableListOf<MeasuredComponent>()

        for (index in 0 until itemsCount) {
            measuredItem.add(measuredItemProvider.getAndMeasure(index))
        }

        val mapViewPort = mapState.viewPort
        measuredItem.forEach {
            it.offset = with(mapState) {
                it.parameters.coordinates.toCanvasPosition().toScreenOffset()
            }
            val itemDrawPosition = ScreenOffset(it.maxWidth * it.parameters.drawPosition.x, it.maxHeight * it.parameters.drawPosition.y)
            val itemViewPort = ViewPort(
                it.offset - itemDrawPosition,
                Size(it.maxWidth.toFloat(), it.maxHeight.toFloat())
            )
            if (mapViewPort.isViewPortIntersecting(itemViewPort))
                visibleItems.add(it)
        }


        return layout(layoutWidth, layoutHeight) {
            visibleItems.fastForEach {
                it.place(this, it.offset, it.parameters, mapState.cameraState.angleDegrees, mapState.cameraState.zoom)
            }
        }
    }
}


//    fun updateCluster(mapState: MapState) {
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