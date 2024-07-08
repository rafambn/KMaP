package io.github.rafambn.kmap

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import io.github.rafambn.kmap.core.CanvasData
import io.github.rafambn.kmap.core.ComponentType
import io.github.rafambn.kmap.core.GroupData
import io.github.rafambn.kmap.core.MapComponentData
import io.github.rafambn.kmap.core.PlacerData
import io.github.rafambn.kmap.core.TileCanvas
import io.github.rafambn.kmap.core.componentData
import io.github.rafambn.kmap.model.ResultTile

interface KMaPScope {
    @Composable
    fun placers(placerData: List<PlacerData>, markerContent: @Composable (PlacerData) -> Unit) = placerData.forEach { item ->
        Layout(
            content = { markerContent(item) },
            modifier = Modifier
                .componentData(MapComponentData(ComponentType.PLACER, item)),
            measurePolicy = { measurables, constraints ->
                val placeable = measurables.first().measure(constraints)
                layout(placeable.width, placeable.height) {
                    placeable.place(
                        x = 0,
                        y = 0
                    )
                }
            }
        )
    }

    @Composable
    fun group(items: List<GroupData>, markerContent: @Composable (GroupData) -> Unit) = items.forEach { item ->
        Layout(
            content = { markerContent(item) },
            modifier = Modifier
                .componentData(MapComponentData(ComponentType.GROUP, item)),
            measurePolicy = { measurables, constraints ->
                val placeable = measurables.first().measure(constraints)
                layout(placeable.width, placeable.height) {
                    placeable.place(
                        x = 0,
                        y = 0
                    )
                }
            }
        )
    }

    @Composable
    fun canvas(canvasData: CanvasData, getTile: suspend (zoom: Int, row: Int, column: Int) -> ResultTile) =
        Layout(
            content = { TileCanvas(getTile) },
            modifier = Modifier
                .fillMaxSize()
                .componentData(MapComponentData(ComponentType.CANVAS, canvasData)),
            measurePolicy = { measurables, constraints ->
                val placeable = measurables.first().measure(constraints)
                layout(placeable.width, placeable.height) {
                    placeable.place(
                        x = 0,
                        y = 0
                    )
                }
            }
        )

    companion object : KMaPScope
}