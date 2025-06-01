package com.rafambn.kmap.lazyMarker

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.Placeable
import com.rafambn.kmap.components.CanvasParameters
import com.rafambn.kmap.components.ClusterParameters
import com.rafambn.kmap.components.MarkerParameters
import com.rafambn.kmap.components.Parameters
import com.rafambn.kmap.components.PathParameters
import com.rafambn.kmap.utils.Degrees
import com.rafambn.kmap.utils.ScreenOffset
import kotlin.math.pow

class MeasuredComponent(
    val index: Int,
    val placeables: List<Placeable>,
    val parameters: Parameters
) {
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
            when(parameters){
                is ClusterParameters -> {
                    placeables[index].placeWithLayer(
                        x = 0,
                        y = 0,
                        zIndex = parameters.zIndex
                    ) {
                        alpha = parameters.alpha

                        translationX = placeOffset.xFloat
                        translationY = placeOffset.yFloat
                        rotationZ =
                            if (parameters.rotateWithMap)
                                (cameraAngle + parameters.rotation).toFloat()
                            else
                                parameters.rotation.toFloat()
                    }
                }
                is MarkerParameters -> {
                    placeables[index].placeWithLayer(
                        x = 0,
                        y = 0,
                        zIndex = parameters.zIndex
                    ) {
                        alpha = parameters.alpha

                        translationX = placeOffset.xFloat - parameters.drawPosition.x * placeables[index].width
                        translationY = placeOffset.yFloat - parameters.drawPosition.y * placeables[index].height
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

                is PathParameters -> {
                    placeables[index].placeWithLayer(
                        x = 0,
                        y = 0,
                        zIndex = parameters.zIndex
                    ) {
                        alpha = parameters.alpha
                        translationX = offset.xFloat
                        translationY = offset.yFloat
                        rotationZ = cameraAngle.toFloat()
                        //TODO add base zoom
                        scaleX = 2F.pow(cameraZoom)
                        scaleY = 2F.pow(cameraZoom)
                    }
                }
                is CanvasParameters -> {
                    placeables[index].placeWithLayer(
                        x = 0,
                        y = 0,
                        zIndex = parameters.zIndex
                    ) {
                        alpha = parameters.alpha
                        clip = true
                    }
                }
            }
        }
    }
}
