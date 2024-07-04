package io.github.rafambn.kmap

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import io.github.rafambn.kmap.core.ComponentType
import io.github.rafambn.kmap.core.MapComponentData
import io.github.rafambn.kmap.core.Placer
import io.github.rafambn.kmap.core.TileCanvas
import io.github.rafambn.kmap.core.componentData
import io.github.rafambn.kmap.model.ResultTile

interface KMaPScope {
    @Composable
    fun placers(items: List<Placer>, markerContent: @Composable (Placer) -> Unit) = items.forEach { item ->
        Layout(
            content = { markerContent(item) },
            modifier = Modifier
                .componentData(MapComponentData(item, ComponentType.PLACER)),
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
    fun canvas(item: Placer, getTile: suspend (zoom: Int, row: Int, column: Int) -> ResultTile) =
        Layout(
            content = { TileCanvas(getTile) },
            modifier = Modifier
                .fillMaxSize()
                .componentData(MapComponentData(item, ComponentType.CANVAS)),
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