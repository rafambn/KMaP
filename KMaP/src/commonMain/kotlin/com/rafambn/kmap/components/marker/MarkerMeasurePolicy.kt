package com.rafambn.kmap.components.marker

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.layout.LazyLayoutMeasureScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.util.fastForEach
import com.rafambn.kmap.core.MapState
import com.rafambn.kmap.core.getViewPort
import com.rafambn.kmap.utils.asOffset
import com.rafambn.kmap.utils.asScreenOffset

@ExperimentalFoundationApi
@Composable
fun rememberMarkerMeasurePolicy(
    markerProviderLambda: () -> MarkerProvider,
    mapState: MapState,
) = remember<LazyLayoutMeasureScope.(Constraints) -> MeasureResult>(
    mapState,
) {
    { containerConstraints ->
        val componentProvider = markerProviderLambda()

        val markersCount = componentProvider.markersCount

        val measuredItemProvider = MeasuredMarkerProvider(componentProvider, this)

        measureMarker(
            markersCount = markersCount,
            measuredItemProvider = measuredItemProvider,
            mapState = mapState,
            layout = { placement ->
                layout(
                    containerConstraints.maxWidth,
                    containerConstraints.maxHeight,
                    emptyMap(),
                    placement
                )
            }
        )
    }
}

internal fun measureMarker(
    markersCount: Int,
    measuredItemProvider: MeasuredMarkerProvider,
    mapState: MapState,
    layout: (Placeable.PlacementScope.() -> Unit) -> MeasureResult
): MeasureResult {

    if (markersCount <= 0)
        return layout {}

    val visibleItems = mutableListOf<MeasuredMarker>()
    val itemsThatCanClusterMap = mutableMapOf<Int, List<MeasuredMarker>>()
    val measuredMarkers = mutableListOf<MeasuredMarker>()

    for (index in 0 until markersCount) {
        measuredMarkers.add(measuredItemProvider.getAndMeasure(index))
    }

    val mapViewPort = Rect(
        Offset.Zero,
        Size(mapState.cameraState.canvasSize.x, mapState.cameraState.canvasSize.y)
    )
    measuredMarkers.forEach { measuredComponent ->
        require(measuredComponent.parameters is MarkerParameters)
        measuredComponent.offset = with(mapState) {
            measuredComponent.parameters.coordinates.toTilePoint().toScreenOffset()
        }
        measuredComponent.viewPort = getViewPort(
            measuredComponent.parameters.drawPosition,
            measuredComponent.maxWidth.toFloat(),
            measuredComponent.maxHeight.toFloat(),
            measuredComponent.offset.asOffset()
        )
        //TODO expand test for rotating markers
        if (measuredComponent.parameters.zoomParameters.zoomVisibilityRange.contains(mapState.cameraState.zoom) &&
            mapViewPort.overlaps(measuredComponent.viewPort)) {
            if (measuredComponent.parameters.clusterId != null) {
                val map = itemsThatCanClusterMap[measuredComponent.parameters.clusterId]
                map?.let {
                    itemsThatCanClusterMap[measuredComponent.parameters.clusterId] = it + measuredComponent
                } ?: run {
                    itemsThatCanClusterMap.put(measuredComponent.parameters.clusterId, listOf(measuredComponent))
                }
            } else
                visibleItems.add(measuredComponent)
        }
        measuredComponent.parameters
    }
    itemsThatCanClusterMap.forEach {
        clusterComponents(it.value, measuredItemProvider, markersCount) { nonClusteredComponent, clusters ->
            visibleItems.addAll(nonClusteredComponent)
            visibleItems.addAll(clusters)
        }
    }

    return layout {
        visibleItems.fastForEach {
            it.place(this, it.offset, it.parameters, mapState.cameraState.angleDegrees, mapState.cameraState.zoom)
        }
    }
}

private fun clusterComponents(
    measuredMarkers: List<MeasuredMarker>,
    measuredItemProvider: MeasuredMarkerProvider,
    markersCount: Int,
    result: (nonClusteredComponent: List<MeasuredMarker>, clusters: List<MeasuredMarker>) -> Unit
) {
    val visited = mutableSetOf<MeasuredMarker>()
    val clusters = mutableListOf<MeasuredMarker>()
    val nonClustered = mutableListOf<MeasuredMarker>()

    for (measuredComponent in measuredMarkers) {
        if (measuredComponent !in visited) {
            visited.add(measuredComponent)
            val intersecting = measuredMarkers.filter { it.viewPort.overlaps(measuredComponent.viewPort) && !visited.contains(it) }
            if (intersecting.isNotEmpty())
                expandCluster(measuredComponent, intersecting, clusters, visited, measuredItemProvider, markersCount)
            else
                nonClustered.add(measuredComponent)
        }
    }
    result(nonClustered, clusters)
}

private fun expandCluster(
    measuredMarker: MeasuredMarker,
    intersecting: List<MeasuredMarker>,
    clusters: MutableList<MeasuredMarker>,
    visited: MutableSet<MeasuredMarker>,
    measuredItemProvider: MeasuredMarkerProvider,
    markersCount: Int,
) {
    var size = 1
    var avgOffset = measuredMarker.viewPort.topLeft
    val queue = intersecting.toMutableList()
    while (queue.isNotEmpty()) {
        val current = queue.removeAt(0)
        visited.add(current)
        size++
        avgOffset += current.viewPort.topLeft
    }
    val cluster = measuredItemProvider.getAndMeasure(measuredMarker.index + markersCount)
    cluster.offset = (avgOffset / size.toFloat()).asScreenOffset()
    clusters.add(cluster)
}
