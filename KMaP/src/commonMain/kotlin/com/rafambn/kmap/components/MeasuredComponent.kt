package com.rafambn.kmap.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.Placeable
import com.rafambn.kmap.core.DrawPosition
import com.rafambn.kmap.utils.Degrees
import com.rafambn.kmap.utils.ScreenOffset
import com.rafambn.kmap.utils.rotate
import com.rafambn.kmap.utils.toRadians
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
            when (parameters) {
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
                    val paddingWithZoom = parameters.totalPadding * 2F.pow(cameraZoom)
                    val paddingOffset = ScreenOffset(paddingWithZoom, paddingWithZoom).rotate(cameraAngle.toRadians())
                    placeables[index].placeWithLayer(
                        x = 0,
                        y = 0,
                        zIndex = parameters.zIndex
                    ) {
                        transformOrigin = DrawPosition.TOP_LEFT.asTransformOrigin()
                        translationX = offset.xFloat - paddingOffset.xFloat
                        translationY = offset.yFloat - paddingOffset.yFloat
                        alpha = parameters.alpha
                        rotationZ = cameraAngle.toFloat()
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
