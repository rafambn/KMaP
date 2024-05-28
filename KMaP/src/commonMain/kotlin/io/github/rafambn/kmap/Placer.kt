package io.github.rafambn.kmap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.toSize

@Composable
internal fun Placer(
    modifier: Modifier = Modifier,
    onMapSizeChange: (Offset) -> Unit,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier
            .background(Color.Gray)
            .clipToBounds()
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                onMapSizeChange(coordinates.size.toSize().toRect().bottomRight)
            }
    ) { measurables, constraints ->

        val canvasPlaceable = measurables.first {
            it.layoutId == MapComponentType.CANVAS
        }.measure(constraints)
        val markersPlaceable = measurables.filter { it.layoutId == MapComponentType.MARKER }.map { it.measure(constraints) }

        val pathsPlaceable = measurables.filter { it.layoutId == MapComponentType.PATH }.map { it.measure(constraints) }

        layout(constraints.maxWidth, constraints.maxHeight) {
            canvasPlaceable.placeRelativeWithLayer(x = 0, y = 0, zIndex = 0F)

            markersPlaceable.forEach {
                it.placeRelativeWithLayer(0, 0)
            }
        }
    }
}