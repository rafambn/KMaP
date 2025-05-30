package com.rafambn.kmap.components.path

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.layout.LazyLayoutMeasureScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.util.fastForEach
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

    val visibleItems = mutableListOf<MeasuredPath>()
    val measuredPaths = mutableListOf<MeasuredPath>()

    for (index in 0 until pathCount) {
        measuredPaths.add(measuredItemProvider.getAndMeasure(index))
    }

    return layout {
        measuredPaths.fastForEach {
            it.place(this, it.offset, it.parameters, mapState.cameraState.angleDegrees, mapState.cameraState.zoom)
        }
    }
}
