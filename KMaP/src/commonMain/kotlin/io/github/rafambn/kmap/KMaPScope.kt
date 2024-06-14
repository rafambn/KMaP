package io.github.rafambn.kmap

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.Layout
import io.github.rafambn.kmap.core.ComponentType
import io.github.rafambn.kmap.core.MapComponentData
import io.github.rafambn.kmap.core.Placer
import io.github.rafambn.kmap.core.componentData
import io.github.rafambn.kmap.utils.offsets.CanvasPosition
import io.github.rafambn.kmap.utils.rotateCentered
import io.github.rafambn.kmap.utils.toIntFloor
import io.github.rafambn.kmap.utils.toRadians
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
                        x = (-item.drawPosition.x * placeable.width).toIntFloor(),
                        y = (-item.drawPosition.y * placeable.height).toIntFloor(),
                        zIndex = item.zIndex
                    ) {
                        if (item.scaleWithMap) {
                            transformOrigin = TransformOrigin(0F,0F)
                            scaleX = 2F.pow(item.zoom - item.zoomToFix)
                            scaleY = 2F.pow(item.zoom - item.zoomToFix)
                        }
                        if (item.rotateWithMap) {
                            transformOrigin = TransformOrigin(0F,0F)
                            rotationZ = item.angle.toFloat()
                        }
                    }
                }
            }
        )
    }

    companion object : KMaPScope
}