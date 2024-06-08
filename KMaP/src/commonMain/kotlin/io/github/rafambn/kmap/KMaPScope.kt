package io.github.rafambn.kmap

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import io.github.rafambn.kmap.core.MapComponentData
import io.github.rafambn.kmap.core.Placer
import io.github.rafambn.kmap.core.componentData
import io.github.rafambn.kmap.utils.offsets.CanvasPosition
import io.github.rafambn.kmap.utils.rotateCentered
import io.github.rafambn.kmap.utils.toRadians
import kotlin.math.pow

interface KMaPScope {
    @Composable
    fun placers(items: List<Placer>, markerContent: @Composable (Placer) -> Unit) = items.forEach { item ->
        Layout(
            content = { markerContent(item) },
            modifier = Modifier
                .componentData(MapComponentData(item.coordinates, item.zIndex, item.drawPosition, item.angle)),
            measurePolicy = { measurables, constraints ->
                val placeable = measurables.first().measure(constraints)
                layout(constraints.minWidth, constraints.minHeight) {
                    placeable.placeWithLayer(
                        x = (-item.drawPosition.x * placeable.width + if (item.scaleWithMap) (1 - 1 / 2F.pow(item.zoom - item.zoomToFix)) * placeable.width / 2 else 0F).toInt(),
                        y = (-item.drawPosition.y * placeable.height + if (item.scaleWithMap) (1 - 1 / 2F.pow(item.zoom - item.zoomToFix)) * placeable.height / 2 else 0F).toInt(),
                        zIndex = item.zIndex
                    ) {
                        if (item.scaleWithMap) {
                            scaleX = 2F.pow(item.zoom - item.zoomToFix)
                            scaleY = 2F.pow(item.zoom - item.zoomToFix)
                        }
                        if (item.rotateWithMap) {
                            val center = CanvasPosition(
                                -(placeable.width) / 2.0,
                                -(placeable.height) / 2.0
                            )
                            val place = CanvasPosition.Zero.rotateCentered(center, item.angle.toRadians())
                            translationX = place.horizontal.toFloat()
                            translationY = place.vertical.toFloat()
                            rotationZ = item.angle.toFloat()
                        }
                    }
                }
            }
        )
    }

    companion object : KMaPScope
}