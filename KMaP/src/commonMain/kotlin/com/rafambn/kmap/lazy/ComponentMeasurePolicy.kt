package com.rafambn.kmap.lazy

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.layout.LazyLayoutMeasureScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.util.fastForEach
import com.rafambn.kmap.components.MarkerParameters
import com.rafambn.kmap.core.MapState
import com.rafambn.kmap.core.getViewPort
import com.rafambn.kmap.core.isViewPortIntersecting

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

        val markersCount = componentProvider.markersCount

        val measuredItemProvider = MeasuredComponentProvider(componentProvider, this)

        measureComponent(
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

internal fun measureComponent(
    markersCount: Int,
    measuredItemProvider: MeasuredComponentProvider,
    mapState: MapState,
    layout: (Placeable.PlacementScope.() -> Unit) -> MeasureResult
): MeasureResult {

    if (markersCount <= 0) {
        return layout {}
    } else {

        val visibleItems = mutableListOf<MeasuredComponent>()
        val itemsThatCanClusterMap = mutableMapOf<Int, List<MeasuredComponent>>()
        val measuredComponents = mutableListOf<MeasuredComponent>()

        for (index in 0 until markersCount) {
            measuredComponents.add(measuredItemProvider.getAndMeasure(index))
        }

        val mapViewPort = mapState.viewPort
        measuredComponents.forEach { measuredComponent ->
            require(measuredComponent.parameters is MarkerParameters)
            measuredComponent.offset = with(mapState) {
                measuredComponent.parameters.coordinates.toCanvasPosition().toScreenOffset()
            }
            measuredComponent.viewPort = getViewPort(
                measuredComponent.parameters.drawPosition,
                measuredComponent.maxWidth.toFloat(),
                measuredComponent.maxHeight.toFloat(),
                measuredComponent.offset
            )
            if (mapViewPort.isViewPortIntersecting(measuredComponent.viewPort)) {
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
}

private fun clusterComponents(
    measuredComponents: List<MeasuredComponent>,
    measuredItemProvider: MeasuredComponentProvider,
    markersCount: Int,
    result: (nonClusteredComponent: List<MeasuredComponent>, clusters: List<MeasuredComponent>) -> Unit
) {
    val visited = mutableSetOf<MeasuredComponent>()
    val clusters = mutableListOf<MeasuredComponent>()
    val nonClustered = mutableListOf<MeasuredComponent>()

    for (measuredComponent in measuredComponents) {
        if (measuredComponent !in visited) {
            visited.add(measuredComponent)
            val intersecting = measuredComponents.filter { it.viewPort.isViewPortIntersecting(measuredComponent.viewPort) && !visited.contains(it) }
            if (intersecting.isNotEmpty())
                expandCluster(measuredComponent, intersecting, clusters, visited, measuredItemProvider,markersCount)
            else
                nonClustered.add(measuredComponent)
        }
    }
    result(nonClustered, clusters)
}

private fun expandCluster(
    measuredComponent: MeasuredComponent,
    intersecting: List<MeasuredComponent>,
    clusters: MutableList<MeasuredComponent>,
    visited: MutableSet<MeasuredComponent>,
    measuredItemProvider: MeasuredComponentProvider,
    markersCount: Int,
) {
    var size = 1
    var avgOffset = measuredComponent.viewPort.origin
    val queue = intersecting.toMutableList()
    while (queue.isNotEmpty()) {
        val current = queue.removeAt(0)
        visited.add(current)
        size++
        avgOffset += current.viewPort.origin
    }
    val cluster = measuredItemProvider.getAndMeasure(measuredComponent.index + markersCount)
    cluster.offset = avgOffset / size.toFloat()
    clusters.add(cluster)
}