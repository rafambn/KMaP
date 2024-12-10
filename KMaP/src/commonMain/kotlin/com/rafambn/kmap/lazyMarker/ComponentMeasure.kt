package com.rafambn.kmap.lazyMarker

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.util.fastForEach
import com.rafambn.kmap.core.MapState
import com.rafambn.kmap.core.ViewPort
import com.rafambn.kmap.core.isViewPortIntersecting
import com.rafambn.kmap.utils.ScreenOffset

internal fun measureComponent(
    itemsCount: Int,
    measuredItemProvider: MeasuredComponentProvider,
    mapState: MapState,
    constraints: Constraints,
    layout: (Int, Int, Placeable.PlacementScope.() -> Unit) -> MeasureResult
): MeasureResult {
    val layoutWidth = constraints.maxWidth
    val layoutHeight = constraints.maxHeight

    if (itemsCount <= 0) {
        return layout(layoutWidth, layoutHeight) {}
    } else {

        val visibleItems = ArrayDeque<MeasuredComponent>()

        val measuredItem = mutableListOf<MeasuredComponent>()

        for (index in 0 until itemsCount) {
            measuredItem.add(measuredItemProvider.getAndMeasure(index))
        }

        val mapViewPort = mapState.viewPort
        measuredItem.forEach {
            it.offset = with(mapState) {
                it.parameters.coordinates.toCanvasPosition().toScreenOffset()
            }
            val itemDrawPosition = ScreenOffset(it.maxWidth * it.parameters.drawPosition.x, it.maxHeight * it.parameters.drawPosition.y)
            val itemViewPort = ViewPort(
                it.offset - itemDrawPosition,
                Size(it.maxWidth.toFloat(), it.maxHeight.toFloat())
            )
            if (mapViewPort.isViewPortIntersecting(itemViewPort))
                visibleItems.add(it)
        }


        return layout(layoutWidth, layoutHeight) {
            visibleItems.fastForEach {
                it.place(this, it.offset, it.parameters, mapState.cameraState.angleDegrees, mapState.cameraState.zoom)
            }
        }
    }
}