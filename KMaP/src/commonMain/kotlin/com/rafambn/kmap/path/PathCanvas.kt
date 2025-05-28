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
import com.rafambn.kmap.gestures.sharedPointerInput
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
    val bounds = pathComponent.path.getBounds()
    Layout(
        modifier = modifier
            .then(
                pathComponent.gestureDetector?.let {
                    Modifier.sharedPointerInput { it(pathComponent.path) }
                }
                ?: Modifier
            )
            .zIndex(pathComponent.zIndex)
            .graphicsLayer {
                alpha = pathComponent.alpha
                clip = true
            }
            .drawBehind {
                withTransform({
                    translate(offset.x, offset.y)
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

//        layout(bounds.width.toInt(), bounds.height.toInt()) {}
    }

//    val pathData = PathData {
//        moveTo(0F, 0F)
//        lineTo(100F, 100F)
//        lineTo(200F, 200F)
//        lineTo(100F, 200F)
//        lineTo(100F, 100F)
//    }
//    val path = pathData.toPath()
//    val bounds = path.getBounds()
//
//    val ds = rememberVectorPainter(
//        defaultWidth = bounds.width.dp,
//        defaultHeight = bounds.height.dp,
//        viewportWidth = bounds.width,
//        viewportHeight = bounds.height,
//        name = RootGroupName,
//        tintColor = Color.Green,
//        tintBlendMode = BlendMode.SrcIn,
//        autoMirror = false,
//        content = { x, y ->
//            Path(pathData, stroke = Brush.linearGradient(listOf(Color.Red, Color.Blue)))
//        })
//    Box{
//        Image(
//            painter = ds,
//            contentDescription = "",
//            modifier = Modifier.rotate(-45F).background(Color.Black),
//            contentScale = ContentScale.None,
//        )
//    }
}
