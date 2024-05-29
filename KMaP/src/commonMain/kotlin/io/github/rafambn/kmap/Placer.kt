package io.github.rafambn.kmap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Color
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
        val canvasData: MapComponent
        val canvasPlaceable = measurables
            .first { (it.layoutId as MapComponent).mapComponentType == MapComponentType.CANVAS }
            .also { canvasData = it.layoutId as MapComponent }
            .measure(constraints)

        val markersData: List<MapComponent>
        val markersPlaceable = measurables
            .filter { (it.layoutId as MapComponent).mapComponentType == MapComponentType.MARKER }
            .also { measurableMarkers -> markersData = measurableMarkers.map { it.layoutId as MapComponent } }
            .map { it.measure(constraints) }

        val pathsData: List<MapComponent>
        val pathsPlaceable = measurables
            .filter { (it.layoutId as MapComponent).mapComponentType == MapComponentType.PATH }
            .also { measurableMarkers -> pathsData = measurableMarkers.map { it.layoutId as MapComponent } }
            .map { it.measure(constraints) }

        layout(constraints.maxWidth, constraints.maxHeight) {
            canvasPlaceable.placeRelativeWithLayer(
                x = canvasData.position.horizontal.toInt(),
                y = canvasData.position.vertical.toInt(),
                zIndex = canvasData.zIndex
            )

            markersPlaceable.forEachIndexed { index, placeable ->
                placeable.placeRelative(
                    x = 0,
                    y = 0,
                    zIndex = markersData[index].zIndex
                )
            }

            pathsPlaceable.forEachIndexed { index, placeable ->
                placeable.placeRelativeWithLayer(
                    x = 0,
                    y = 0,
                    zIndex = pathsData[index].zIndex
                )
            }
        }
    }
}