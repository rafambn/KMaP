package com.rafambn.kmap.lazyMarker

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.layout.LazyLayoutMeasureScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import com.rafambn.kmap.core.MapState

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
