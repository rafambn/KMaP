package com.rafambn.kmap.path

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.zIndex
import com.rafambn.kmap.core.CameraState
import com.rafambn.kmap.components.PathParameters
import com.rafambn.kmap.core.MapState
import com.rafambn.kmap.utils.ScreenOffset
import kotlin.math.pow

@Composable
internal fun PathCanvas(
    cameraState: CameraState,
    modifier: Modifier,
    pathParameters: PathParameters,
    mapState: MapState
//    gestureDetector: (suspend PointerInputScope.() -> Unit)? //TODO add input scope
) {
    val rotationDegrees = cameraState.angleDegrees.toFloat()
    val offset: ScreenOffset
    with(mapState) {
        offset = pathParameters.origin.toTilePoint().toScreenOffset()
    }
    Layout(
        modifier = modifier
//            .then(gestureDetector?.let { Modifier.pointerInput(PointerEventPass.Main) { it(this) } } ?: Modifier)
            .zIndex(pathParameters.zIndex)
            .graphicsLayer {
                alpha = pathParameters.alpha
                clip = true
            }
            .drawBehind {
                withTransform({
                    translate(offset.x.toFloat(), offset.y.toFloat())
                    rotate(rotationDegrees, Offset.Zero)
                    scale(2F.pow(cameraState.zoom), Offset.Zero)
                }) {
                    drawIntoCanvas { canvas ->
                        drawPath(
                            path = pathParameters.path,
                            color = pathParameters.color,
                            colorFilter = pathParameters.colorFilter,
                            blendMode = pathParameters.blendMode,
                            style = pathParameters.style
                        )
                    }
                }
            }
    ) { _, constraints ->
        layout(constraints.maxWidth, constraints.maxHeight) {}
    }
}