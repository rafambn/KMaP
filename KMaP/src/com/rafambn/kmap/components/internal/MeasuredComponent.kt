package com.rafambn.kmap.components.internal

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.layout.Placeable
import com.rafambn.kmap.components.DrawPosition
import com.rafambn.kmap.components.ViewPort
import com.rafambn.kmap.components.getViewPort
import com.rafambn.kmap.components.parameters.*
import com.rafambn.kmap.geometry.angle.Degrees
import com.rafambn.kmap.geometry.angle.rotate
import com.rafambn.kmap.geometry.angle.toRadians
import com.rafambn.kmap.geometry.plane.ScreenOffset
import kotlin.math.pow

internal class MeasuredComponent(
    val index: Int,
    val placeables: List<Placeable>,
    val parameters: Parameters
) {
    var offset = ScreenOffset.Zero
    var viewPort = ViewPort.Zero

    private val placeablesCount: Int get() = placeables.size

    fun markerViewPort(cameraAngle: Degrees, cameraZoom: Float): ViewPort {
        require(parameters is MarkerParameters)
        if (placeables.isEmpty()) return ViewPort.Zero

        val rotation = if (parameters.rotateWithMap) cameraAngle + parameters.rotation else parameters.rotation
        val scale = parameters.zoomToFix?.let { 2F.pow(cameraZoom - it) } ?: 1F
        val transform = Matrix()
        transform.translate(offset.x.toFloat(), offset.y.toFloat())
        transform.rotateZ(rotation.toFloat())
        transform.scale(scale, scale)

        // Each root rotates and scales around its own draw anchor, just as in place().
        val bounds = placeables.map { placeable ->
            transform.map(getViewPort(
                parameters.drawPosition,
                placeable.width.toFloat(),
                placeable.height.toFloat(),
                Offset.Zero,
            ).value)
        }
        return ViewPort(Rect(
            bounds.minOf { it.left },
            bounds.minOf { it.top },
            bounds.maxOf { it.right },
            bounds.maxOf { it.bottom },
        ))
    }

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

                        translationX = placeOffset.x.toFloat()
                        translationY = placeOffset.y.toFloat()
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

                        translationX = placeOffset.x.toFloat() - parameters.drawPosition.x * placeables[index].width
                        translationY = placeOffset.y.toFloat() - parameters.drawPosition.y * placeables[index].height
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
                    val paddingOffset = ScreenOffset(
                        paddingWithZoom.toDouble(),
                        paddingWithZoom.toDouble()
                    ).rotate(cameraAngle.toRadians())
                    placeables[index].placeWithLayer(
                        x = 0,
                        y = 0,
                        zIndex = parameters.zIndex
                    ) {
                        transformOrigin = DrawPosition.TOP_LEFT.asTransformOrigin()
                        translationX = offset.x.toFloat() - paddingOffset.x.toFloat()
                        translationY = offset.y.toFloat() - paddingOffset.y.toFloat()
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
