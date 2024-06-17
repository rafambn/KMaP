package io.github.rafambn.kmap

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.Layout
import io.github.rafambn.kmap.core.ComponentType
import io.github.rafambn.kmap.core.MapComponentData
import io.github.rafambn.kmap.core.Placer
import io.github.rafambn.kmap.core.componentData
import io.github.rafambn.kmap.utils.toIntFloor
import kotlin.math.pow

interface KMaPScope {
    @Composable
    fun placers(items: List<Placer>, markerContent: @Composable (Placer) -> Unit) = items.forEach { item ->
        Layout(
            content = { markerContent(item) },
            modifier = Modifier
                .componentData(MapComponentData(item.coordinates, item.zIndex, item.drawPosition, item.angle, ComponentType.PLACER)),
            measurePolicy = { measurables, constraints ->
                val placeable = measurables.first().measure(constraints)
                layout(placeable.width, placeable.height) {
                    placeable.placeWithLayer(
                        x = 0,
                        y = 0,
                        zIndex = item.zIndex
                    ) {
                        alpha = item.alpha
                        translationX = -item.drawPosition.x * placeable.width
                        translationY = -item.drawPosition.y * placeable.height
                        transformOrigin = TransformOrigin(item.drawPosition.x, item.drawPosition.y)
                        if (item.scaleWithMap) {
                            scaleX = 2F.pow(item.zoom - item.zoomToFix)
                            scaleY = 2F.pow(item.zoom - item.zoomToFix)
                        }
                        rotationZ = if (item.rotateWithMap) (item.angle + item.rotation).toFloat() else item.rotation.toFloat()
                    }
                }
            }
        )
    }

    companion object : KMaPScope
}