package com.rafambn.kmap.path

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.rafambn.kmap.core.CameraState
import com.rafambn.kmap.mapProperties.MapProperties
import com.rafambn.kmap.components.PathParameters
import com.rafambn.kmap.utils.CanvasDrawReference
import com.rafambn.kmap.utils.asOffset
import com.rafambn.kmap.utils.toIntFloor
import kotlin.math.pow

@Composable
internal fun PathCanvas(
    cameraState: CameraState,
    mapProperties: MapProperties,
    positionOffset: CanvasDrawReference,
    modifier: Modifier,
    pathParameters: PathParameters,
//    gestureDetector: (suspend PointerInputScope.() -> Unit)? //TODO add input scope
) {
    val zoomLevel = cameraState.zoom.toIntFloor()
    val magnifierScale = cameraState.zoom - zoomLevel + 1F
    val tileSize = mapProperties.tileSize
    val rotationDegrees = cameraState.angleDegrees.toFloat()
    val translation = cameraState.canvasSize.asOffset() / 2F

    Layout(
        modifier = modifier
//            .then(gestureDetector?.let { Modifier.pointerInput(PointerEventPass.Main) { it(this) } } ?: Modifier)
            .graphicsLayer {
                alpha = pathParameters.alpha
                clip = true
            }
            .zIndex(pathParameters.zIndex)
            .drawBehind {
                withTransform({
                    translate(translation.x, translation.y)
                    rotate(rotationDegrees, Offset.Zero)
                    scale(2F.pow(cameraState.zoom), Offset.Zero)
                }) {
                    drawIntoCanvas { canvas ->
                        drawPath(
                            path = pathParameters.path,
                            color = Color.Red,
                            style = Stroke(
                                width = 4.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                            )
                        )
                    }
                }
            }
    ) { _, constraints ->
        layout(constraints.maxWidth, constraints.maxHeight) {}
    }
}