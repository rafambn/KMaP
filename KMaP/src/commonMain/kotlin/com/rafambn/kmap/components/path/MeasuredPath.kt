package com.rafambn.kmap.components.path

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.Placeable
import com.rafambn.kmap.core.CameraState
import com.rafambn.kmap.mapProperties.CoordinatesRange
import com.rafambn.kmap.utils.CanvasDrawReference
import com.rafambn.kmap.utils.Degrees
import com.rafambn.kmap.utils.ScreenOffset
import com.rafambn.kmap.utils.asOffset
import kotlin.math.pow

class MeasuredPath(
    val index: Int,
    val placeables: List<Placeable>,
    val parameters: PathParameter
) {//TODO maybe merge with MeasuredMarker

    var offset = ScreenOffset.Zero

    private val placeablesCount: Int get() = placeables.size

    fun place(
        scope: Placeable.PlacementScope,
        parameters: PathParameter,
        cameraAngle: Degrees,
        cameraZoom: Float,
        coordinatesRange: CoordinatesRange,
    ) = with(scope) {
        repeat(placeablesCount) { index ->
            placeables[index].placeWithLayer(
                x = 0,
                y = 0,
                zIndex = parameters.zIndex
            ) {
                alpha = parameters.alpha
                translationX = offset.x
                translationY = offset.y
                rotationZ = cameraAngle.toFloat()
                //TODO add base zoom
                scaleX = 2F.pow(cameraZoom) * coordinatesRange.longitude.orientation
                scaleY = 2F.pow(cameraZoom) * coordinatesRange.latitude.orientation
            }
        }
    }
}
