package com.rafambn.kmap.lazyMarker

import androidx.compose.ui.layout.Placeable
import com.rafambn.kmap.components.MarkerParameters
import com.rafambn.kmap.utils.Degrees
import com.rafambn.kmap.utils.ScreenOffset
import kotlin.math.pow

class MeasuredComponent(
    val placeables: List<Placeable>,
    val parameters: MarkerParameters
) {
    val maxWidth: Int = placeables.maxOf { placeable -> placeable.width }
    val maxHeight: Int = placeables.maxOf { placeable -> placeable.height }
    var offset = ScreenOffset.Zero

    private val placeablesCount: Int get() = placeables.size

    fun place(
        scope: Placeable.PlacementScope,
        placeOffset: ScreenOffset,
        parameters: MarkerParameters,
        cameraAngle: Degrees,
        cameraZoom: Float
    ) = with(scope) {
        repeat(placeablesCount) { index ->
            placeables[index].placeWithLayer(
                x = 0,
                y = 0,
                zIndex = parameters.zIndex
            ) {
                alpha = parameters.alpha

                translationX = placeOffset.x - parameters.drawPosition.x * placeables[index].width
                translationY = placeOffset.y - parameters.drawPosition.y * placeables[index].height
                transformOrigin = parameters.drawPosition.asTransformOrigin()
                parameters.zoomToFix?.let { zoom ->
                    scaleX = 2F.pow(cameraZoom - zoom)
                    scaleY = 2F.pow(cameraZoom - zoom)
                }
                rotationZ =
                    if (parameters.rotateWithMap)
                        (cameraAngle + parameters.rotation).toFloat()
                    else
                        parameters.rotation.toFloat()
            }
        }
    }
}