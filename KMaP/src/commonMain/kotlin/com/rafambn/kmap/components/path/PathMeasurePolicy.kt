package com.rafambn.kmap.components.path

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.layout.LazyLayoutMeasureScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.util.fastForEach
import com.rafambn.kmap.core.MapState
import com.rafambn.kmap.utils.Coordinates
import com.rafambn.kmap.utils.TilePoint

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
            pathCount = markersCount,
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
    pathCount: Int,
    measuredItemProvider: MeasuredPathProvider,
    mapState: MapState,
    layout: (Placeable.PlacementScope.() -> Unit) -> MeasureResult
): MeasureResult {

    if (pathCount <= 0)
        return layout {}

    val measuredPaths = mutableListOf<MeasuredPath>()

    repeat(pathCount) { index ->
        val measuredComponent = measuredItemProvider.getAndMeasure(index)


        var padding = if (measuredComponent.parameters.style is Stroke) measuredComponent.parameters.style.width / 2F else 0F
        padding = maxOf(padding, measuredComponent.parameters.detectionThreshold)
        measuredComponent.offset = with(mapState) {
            TilePoint(
                measuredComponent.parameters.path.getBounds().topLeft.x.toDouble() + 450 - padding,
                measuredComponent.parameters.path.getBounds().topLeft.y.toDouble() + 450 - padding
            ).toScreenOffset()
        }
        measuredPaths.add(measuredComponent)
    }

    return layout {
        measuredPaths.fastForEach {
            it.place(
                this,
                it.parameters,
                mapState.cameraState.angleDegrees,
                mapState.cameraState.zoom,
                mapState.mapProperties.coordinatesRange,
            )
        }
    }
}
