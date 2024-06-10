package io.github.rafambn.kmap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.toSize
import io.github.rafambn.kmap.core.MapComponentData
import io.github.rafambn.kmap.core.componentData

@Composable
fun KMaP(
    modifier: Modifier = Modifier,
    onCanvasChangeSize: (Offset) -> Unit,
    content: @Composable KMaPScope.() -> Unit = {}
) {
    Layout(
        content = {
            KMaPScope.content()
        },
        modifier
            .background(Color.Gray)
            .clipToBounds()
            .wrapContentSize()
            .onGloballyPositioned { coordinates ->
                onCanvasChangeSize(coordinates.size.toSize().toRect().bottomRight)
            }
    ) { measurables, constraints ->
        val placersData: List<MapComponentData>
        val placersPlaceable = measurables
            .also { measurablePlacers -> placersData = measurablePlacers.map { it.componentData } }
            .map { it.measure(constraints) }

        layout(constraints.maxWidth, constraints.maxHeight) {
            placersPlaceable.forEachIndexed { index, placeable ->
                placeable.place(
                    x = placersData[index].position.x.toInt(),
                    y = placersData[index].position.y.toInt(),
                    zIndex = placersData[index].zIndex
                )
            }
        }
    }
}