package com.rafambn.kmap.components.path

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.layout.LazyLayoutMeasureScope
import androidx.compose.ui.unit.Constraints

@OptIn(ExperimentalFoundationApi::class)
internal class MeasuredPathProvider(
    private val componentProvider: PathProvider,
    private val measureScope: LazyLayoutMeasureScope,
) {
    private val childConstraints = Constraints(
        maxWidth = Constraints.Infinity,
        maxHeight = Constraints.Infinity
    )

    fun getAndMeasure(index: Int): MeasuredPath =
        MeasuredPath(
            index = index,
            placeables = measureScope.measure(index, childConstraints),
            parameters = componentProvider.getParameters(index)
        )
}
