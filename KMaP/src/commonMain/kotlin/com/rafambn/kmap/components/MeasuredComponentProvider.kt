package com.rafambn.kmap.components

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

    private fun getAndMeasure(index: Int): MeasuredComponent =
        MeasuredComponent(
            index = index,
            placeables = measureScope.measure(index, childConstraints),
            parameters = componentProvider.getParameters(index)
        )

    // Specific functions for each component type with automatic index shifting
    fun getAndMeasureMarker(markerIndex: Int): MeasuredComponent {
        val actualIndex = markerIndex // Markers are at [0 ... markersCount-1]
        return getAndMeasure(actualIndex)
    }

    fun getAndMeasureCluster(markerIndex: Int): MeasuredComponent {
        val actualIndex = componentProvider.markersCount + markerIndex // Clusters are at [markersCount ... markersCount*2-1]
        return getAndMeasure(actualIndex)
    }

    fun getAndMeasurePath(pathIndex: Int): MeasuredComponent {
        val actualIndex = componentProvider.markersCount * 2 + pathIndex // Paths are at [markersCount*2 ... markersCount*2+pathsCount-1]
        return getAndMeasure(actualIndex)
    }

    fun getAndMeasureCanvas(canvasIndex: Int): MeasuredComponent {
        val actualIndex =
            componentProvider.markersCount * 2 + componentProvider.pathsCount + canvasIndex // Canvas are at [markersCount*2+pathsCount ... end]
        return getAndMeasure(actualIndex)
    }
}
