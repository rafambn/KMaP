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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import com.rafambn.kmap.components.Path
import com.rafambn.kmap.core.CameraState
import com.rafambn.kmap.core.MapState
import kotlin.math.pow

@Composable
internal fun PathCanvas(
    cameraState: CameraState,
    modifier: Modifier,
    path: Path,
    mapState: MapState
) {
    val rotationDegrees = cameraState.angleDegrees.toFloat()
//    val offset: ScreenOffset
//    with(mapState) {
//        offset = path.origin.toTilePoint().toScreenOffset()
//    }
    val densityScale = LocalDensity.current.density
    Layout(
        modifier = modifier
//            .then(pathComponent.gestureDetector?.let { Modifier.pointerInput(PointerEventPass.Main) { it(this) } } ?: Modifier)//TODO add path gesture
            .zIndex(path.parameters.zIndex)
            .graphicsLayer {
                alpha = path.parameters.alpha
                clip = true
            }
            .drawBehind {
                withTransform({
//                    translate(offset.x.toFloat(), offset.y.toFloat())
                    rotate(rotationDegrees, Offset.Zero)
                    scale(2F.pow(cameraState.zoom) * densityScale, Offset.Zero)
                }) {
                    drawIntoCanvas { canvas ->
                        drawPath(
                            path = path.parameters.path,
                            color = path.parameters.color,
                            colorFilter = path.parameters.colorFilter,
                            blendMode = path.parameters.blendMode,
                            style = path.parameters.style
                        )
                    }
                }
            }
    ) { _, constraints ->
        layout(constraints.maxWidth, constraints.maxHeight) {}
    }
}
