package com.rafambn.kmap.components.marker

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.layout.LazyLayoutMeasureScope
import androidx.compose.ui.unit.Constraints

@OptIn(ExperimentalFoundationApi::class)
internal class MeasuredMarkerProvider(
    private val componentProvider: MarkerProvider,
    private val measureScope: LazyLayoutMeasureScope,
) {
    private val childConstraints = Constraints(
        maxWidth = Constraints.Companion.Infinity,
        maxHeight = Constraints.Companion.Infinity
    )

    fun getAndMeasure(index: Int): MeasuredMarker =
        MeasuredMarker(
            index = index,
            placeables = measureScope.measure(index, childConstraints),
            parameters = componentProvider.getParameters(index)
        )
}
