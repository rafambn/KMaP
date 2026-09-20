package com.rafambn.kmap.components.internal

import androidx.compose.ui.layout.Placeable
import com.rafambn.kmap.components.CanvasParameters
import com.rafambn.kmap.components.ClusterParameters
import com.rafambn.kmap.components.DrawPosition
import com.rafambn.kmap.components.MarkerParameters
import com.rafambn.kmap.components.Parameters
import com.rafambn.kmap.components.PathParameters
import com.rafambn.kmap.components.ViewPort
import com.rafambn.kmap.geometry.angle.Degrees
import com.rafambn.kmap.geometry.angle.rotate
import com.rafambn.kmap.geometry.angle.toRadians
import com.rafambn.kmap.utils.ScreenOffset
import kotlin.math.pow

internal class MeasuredComponent(
    val index: Int,
    val placeables: List<Placeable>,
    val parameters: Parameters
) {
    val maxWidth: Int = placeables.maxOf { placeable -> placeable.width }
    val maxHeight: Int = placeables.maxOf { placeable -> placeable.height }
    var offset = ScreenOffset.Zero
    var viewPort = ViewPort.Zero

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
