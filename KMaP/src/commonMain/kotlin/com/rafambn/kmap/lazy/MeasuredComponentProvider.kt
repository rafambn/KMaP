package com.rafambn.kmap.lazy

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.layout.LazyLayoutMeasureScope
import androidx.compose.ui.unit.Constraints

@OptIn(ExperimentalFoundationApi::class)
internal class MeasuredComponentProvider(
    private val componentProvider: ComponentProvider,
    private val measureScope: LazyLayoutMeasureScope,
) {
    private val childConstraints = Constraints(
        maxWidth = Constraints.Infinity,
        maxHeight = Constraints.Infinity
    )

    fun getAndMeasure(index: Int): MeasuredComponent =
        MeasuredComponent(
            index = index,
            placeables = measureScope.measure(index, childConstraints),
            parameters = componentProvider.getParameters(index)
        )
}