package com.rafambn.kmap.components.path

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.Placeable
import com.rafambn.kmap.components.Parameters
import com.rafambn.kmap.components.marker.MarkerParameters
import com.rafambn.kmap.utils.Degrees
import com.rafambn.kmap.utils.ScreenOffset

class MeasuredPath(
    val index: Int,
    val placeables: List<Placeable>,
    val parameters: Parameters
) {//TODO maybe merge with MeasuredMarker

    val maxWidth: Int = placeables.maxOf { placeable -> placeable.width }
    val maxHeight: Int = placeables.maxOf { placeable -> placeable.height }
    var offset = ScreenOffset.Zero
    var viewPort = Rect(Offset.Zero, Size.Zero)

    private val placeablesCount: Int get() = placeables.size

    fun place(
        scope: Placeable.PlacementScope,
        placeOffset: ScreenOffset,
        parameters: Parameters,
        cameraAngle: Degrees,
        cameraZoom: Float
    ) = with(scope) {
        repeat(placeablesCount) { index ->
            require(parameters is PathParameter)
            placeables[index].placeWithLayer(
                x = 0,
                y = 0,
                zIndex = parameters.zIndex
            ) {
                alpha = parameters.alpha

                translationX = placeOffset.x
                translationY = placeOffset.y
                rotationZ = cameraAngle.toFloat()
            }
        }
    }
}
