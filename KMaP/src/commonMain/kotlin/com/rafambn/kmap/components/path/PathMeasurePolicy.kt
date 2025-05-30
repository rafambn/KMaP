package com.rafambn.kmap.components.path

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.layout.LazyLayoutMeasureScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import com.rafambn.kmap.core.MapState

@ExperimentalFoundationApi
@Composable
fun rememberPathMeasurePolicy(
    pathProviderLambda: () -> PathProvider,
    mapState: MapState,
) = remember<LazyLayoutMeasureScope.(Constraints) -> MeasureResult>(
    mapState,
) {
    { containerConstraints ->
        val componentProvider = pathProviderLambda()

        val markersCount = componentProvider.itemCount

        val measuredItemProvider = MeasuredPathProvider(componentProvider, this)

        measurePath(
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

internal fun measurePath(
    markersCount: Int,
    measuredItemProvider: MeasuredPathProvider,
    mapState: MapState,
    layout: (Placeable.PlacementScope.() -> Unit) -> MeasureResult
): MeasureResult {
//
//    if (markersCount <= 0)
//        return layout {}
//
//    val visibleItems = mutableListOf<MeasuredMarker>()
//    val itemsThatCanClusterMap = mutableMapOf<Int, List<MeasuredMarker>>()
//    val measuredMarkers = mutableListOf<MeasuredMarker>()
//
//    for (index in 0 until markersCount) {
//        measuredMarkers.add(measuredItemProvider.getAndMeasure(index))
//    }
//
//    val mapViewPort = Rect(
//        Offset.Zero,
//        Size(mapState.cameraState.canvasSize.x, mapState.cameraState.canvasSize.y)
//    )
//    measuredMarkers.forEach { measuredComponent ->
//        require(measuredComponent.parameters is MarkerParameters)
//        measuredComponent.offset = with(mapState) {
//            measuredComponent.parameters.coordinates.toTilePoint().toScreenOffset()
//        }
//        measuredComponent.viewPort = getViewPort(
//            measuredComponent.parameters.drawPosition,
//            measuredComponent.maxWidth.toFloat(),
//            measuredComponent.maxHeight.toFloat(),
//            measuredComponent.offset.asOffset()
//        )
//        //TODO expand test for rotating markers
//        if (measuredComponent.parameters.zoomParameters.zoomVisibilityRange.contains(mapState.cameraState.zoom) &&
//            mapViewPort.overlaps(measuredComponent.viewPort)) {
//            if (measuredComponent.parameters.clusterId != null) {
//                val map = itemsThatCanClusterMap[measuredComponent.parameters.clusterId]
//                map?.let {
//                    itemsThatCanClusterMap[measuredComponent.parameters.clusterId] = it + measuredComponent
//                } ?: run {
//                    itemsThatCanClusterMap.put(measuredComponent.parameters.clusterId, listOf(measuredComponent))
//                }
//            } else
//                visibleItems.add(measuredComponent)
//        }
//        measuredComponent.parameters
//    }
//    itemsThatCanClusterMap.forEach {
//        clusterComponents(it.value, measuredItemProvider, markersCount) { nonClusteredComponent, clusters ->
//            visibleItems.addAll(nonClusteredComponent)
//            visibleItems.addAll(clusters)
//        }
//    }
//
//    return layout {
//        visibleItems.fastForEach {
//            it.place(this, it.offset, it.parameters, mapState.cameraState.angleDegrees, mapState.cameraState.zoom)
//        }
//    }

    TODO("Not yet implemented")
}
