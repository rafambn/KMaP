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
import com.rafambn.kmap.components.PathComponent
import com.rafambn.kmap.core.CameraState
import com.rafambn.kmap.core.MapState
import com.rafambn.kmap.utils.ScreenOffset
import kotlin.math.pow

@Composable
internal fun PathCanvas(
    cameraState: CameraState,
    modifier: Modifier,
    pathComponent: PathComponent,
    mapState: MapState
) {
    val rotationDegrees = cameraState.angleDegrees.toFloat()
    val offset: ScreenOffset
    with(mapState) {
        offset = pathComponent.origin.toTilePoint().toScreenOffset()
    }
    val densityScale = LocalDensity.current.density
    Layout(
        modifier = modifier
//            .then(pathComponent.gestureDetector?.let { Modifier.pointerInput(PointerEventPass.Main) { it(this) } } ?: Modifier)//TODO add path gesture
            .zIndex(pathComponent.zIndex)
            .graphicsLayer {
                alpha = pathComponent.alpha
                clip = true
            }
            .drawBehind {
                withTransform({
                    translate(offset.x.toFloat(), offset.y.toFloat())
                    rotate(rotationDegrees, Offset.Zero)
                    scale(2F.pow(cameraState.zoom) * densityScale, Offset.Zero)
                }) {
                    drawIntoCanvas { canvas ->
                        drawPath(
                            path = pathComponent.path,
                            color = pathComponent.color,
                            colorFilter = pathComponent.colorFilter,
                            blendMode = pathComponent.blendMode,
                            style = pathComponent.style
                        )
                    }
                }
            }
    ) { _, constraints ->
        layout(constraints.maxWidth, constraints.maxHeight) {}
    }
}