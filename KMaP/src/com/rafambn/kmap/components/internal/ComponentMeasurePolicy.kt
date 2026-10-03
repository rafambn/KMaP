package com.rafambn.kmap.components.internal

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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastForEach
import com.rafambn.kmap.MapState
import com.rafambn.kmap.components.ViewPort
import com.rafambn.kmap.components.parameters.MarkerParameters
import com.rafambn.kmap.components.parameters.PathParameters
import com.rafambn.kmap.geometry.plane.*

@ExperimentalFoundationApi
@Composable
internal fun rememberComponentMeasurePolicy(
    componentProviderLambda: () -> ComponentProvider,
    mapState: MapState,
) = remember<LazyLayoutMeasureScope.(Constraints) -> MeasureResult>(
    mapState,
) {
    { containerConstraints ->
        require(containerConstraints.hasBoundedWidth && containerConstraints.hasBoundedHeight) {
            "KMaP requires bounded width and height constraints"
        }

        val layoutSize = IntSize(containerConstraints.maxWidth, containerConstraints.maxHeight)
        val componentProvider = componentProviderLambda()
        mapState.setViewportSize(layoutSize)

        val measuredItemProvider = MeasuredComponentProvider(componentProvider, this)

        measureComponent(
            markersCount = componentProvider.markersCount,
            canvasCount = componentProvider.canvasCount,
            pathsCount = componentProvider.pathsCount,
            measuredItemProvider = measuredItemProvider,
            mapState = mapState,
            canvasConstraints = Constraints.fixed(layoutSize.width, layoutSize.height),
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
    canvasCount: Int,
    pathsCount: Int,
    measuredItemProvider: MeasuredComponentProvider,
    mapState: MapState,
    canvasConstraints: Constraints,
    layout: (Placeable.PlacementScope.() -> Unit) -> MeasureResult
): MeasureResult {
    val visibleItems = mutableListOf<MeasuredComponent>()

    if (markersCount > 0) {
        val itemsThatCanClusterMap = mutableMapOf<Int, MutableList<MeasuredComponent>>()
        val mapViewPort = ViewPort(
            Rect(
                Offset.Zero,
                Size(
                    mapState.viewportSize.width.toFloat(),
                    mapState.viewportSize.height.toFloat()
                )
            )
        )
        repeat(markersCount) { index ->
            val measuredComponent = measuredItemProvider.getAndMeasureMarker(index)
            require(measuredComponent.parameters is MarkerParameters)
            measuredComponent.offset = context(mapState) {
                measuredComponent.parameters.coordinates.toTilePoint().toNearestScreenOffset()
            }
            measuredComponent.viewPort = measuredComponent.markerViewPort(
                mapState.cameraState.angleDegrees,
                mapState.cameraState.zoom,
            )
            if (measuredComponent.parameters.zoomVisibilityRange.contains(mapState.cameraState.zoom) &&
                !measuredComponent.viewPort.value.isEmpty &&
                mapViewPort.overlaps(measuredComponent.viewPort)
            ) {
                val clusterId = measuredComponent.parameters.clusterId
                if (clusterId != null) {
                    itemsThatCanClusterMap.getOrPut(clusterId) { mutableListOf() }.add(measuredComponent)
                } else {
                    visibleItems.add(measuredComponent)
                }
            }
        }
        itemsThatCanClusterMap.values.forEach {
            visibleItems.addAll(clusterComponents(it, measuredItemProvider))
        }
    }
    if (pathsCount > 0) {
        val measuredPats = mutableListOf<MeasuredComponent>()
        repeat(pathsCount) { index ->
            val measuredPath = measuredItemProvider.getAndMeasurePath(index)
            require(measuredPath.parameters is PathParameters)
            if (measuredPath.parameters.zoomVisibilityRange.contains(mapState.cameraState.zoom)) {
                val bounds = measuredPath.parameters.path.getBounds()
                val coordinatesRange = mapState.mapProperties.coordinatesRange
                val drawPoint = ProjectedCoordinates(
                    (if (coordinatesRange.longitude.orientation == 1) bounds.left else bounds.right).toDouble(),
                    (if (coordinatesRange.latitude.orientation == 1) bounds.top else bounds.bottom).toDouble()
                )
                measuredPath.offset = context(mapState) {
                    drawPoint.toTilePoint().toScreenOffset()
                }
                measuredPats.add(measuredPath)
            }
        }
        visibleItems.addAll(measuredPats)
    }

    if (canvasCount > 0) {
        repeat(canvasCount) { index ->
            visibleItems.add(measuredItemProvider.getAndMeasureCanvas(index, canvasConstraints))
        }
    }

    return layout {
        visibleItems.fastForEach {
            it.place(
                this,
                it.offset,
                it.parameters,
                mapState.cameraState.angleDegrees,
                mapState.cameraState.zoom,
            )
        }
    }
}

private fun clusterComponents(
    measuredComponents: List<MeasuredComponent>,
    measuredItemProvider: MeasuredComponentProvider,
): List<MeasuredComponent> {
    val remaining = measuredComponents.toMutableSet()
    val clusters = mutableListOf<MeasuredComponent>()
    val nonClustered = mutableListOf<MeasuredComponent>()

    for (measuredComponent in measuredComponents) {
        if (!remaining.remove(measuredComponent)) continue

        val queue = ArrayDeque<MeasuredComponent>()
        queue.addLast(measuredComponent)
        var size = 0
        var offsetSum = Offset.Zero
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            size++
            offsetSum += current.viewPort.topLeft
            val candidates = remaining.iterator()
            while (candidates.hasNext()) {
                val candidate = candidates.next()
                if (current.viewPort.overlaps(candidate.viewPort)) {
                    candidates.remove()
                    queue.addLast(candidate)
                }
            }
        }

        if (size == 1) {
            nonClustered.add(measuredComponent)
        } else {
            val cluster = measuredItemProvider.getAndMeasureCluster(measuredComponent.index)
            cluster.offset = (offsetSum / size.toFloat()).asScreenOffset()
            clusters.add(cluster)
        }
    }
    return nonClustered + clusters
}
