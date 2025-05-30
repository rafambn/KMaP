package com.rafambn.kmap.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.PointerInputScope
import com.rafambn.kmap.components.canvas.CanvasComponent
import com.rafambn.kmap.components.path.PathComponent
import com.rafambn.kmap.components.canvas.tiled.TileRenderResult
import com.rafambn.kmap.components.marker.ClusterComponent
import com.rafambn.kmap.components.marker.ClusterParameters
import com.rafambn.kmap.components.marker.MarkerComponent
import com.rafambn.kmap.components.marker.MarkerParameters
import com.rafambn.kmap.components.path.PathParameter

class KMaPContent(
    content: KMaPScope.() -> Unit,
) : KMaPScope {

    val markers = mutableListOf<MarkerComponent>()
    val cluster = mutableListOf<ClusterComponent>()
    val canvas = mutableListOf<CanvasComponent>()
    val paths = mutableListOf<PathComponent>()

    init {
        apply(content)
    }

    override fun canvas(
        alpha: Float,
        zIndex: Float,
        maxCacheTiles: Int,
        tileSource: suspend (zoom: Int, row: Int, column: Int) -> TileRenderResult,
        gestureDetection: (suspend PointerInputScope.() -> Unit)?
    ) {
        canvas.add(CanvasComponent(alpha, zIndex, maxCacheTiles, tileSource, gestureDetection))
    }

    override fun marker(markerParameters: MarkerParameters, markerContent: @Composable () -> Unit) {
        markers.add(MarkerComponent(markerParameters, { markerContent() }))
    }

    override fun markers(markerParameters: List<MarkerParameters>, markerContent: @Composable (index: Int) -> Unit) {
        markerParameters.forEachIndexed { index, it ->
            markers.add(MarkerComponent(it, { markerContent(index) }))
        }
    }

    override fun cluster(clusterParameters: ClusterParameters, clusterContent: @Composable () -> Unit) {
        cluster.add(ClusterComponent(clusterParameters, { clusterContent() }))
    }

    override fun path(pathParameter: PathParameter, gestureDetection: (suspend PointerInputScope.(Path) -> Unit)?) {
        paths.add(
            PathComponent(
                pathParameter,
                gestureDetection
            ){
//
//                val rotationDegrees = cameraState.angleDegrees.toFloat()
////    val offset: ScreenOffset
////    with(mapState) {
////        offset = pathComponent.origin.toTilePoint().toScreenOffset()
////    }
//                val densityScale = LocalDensity.current.density
//                val matrix = Matrix()
//                matrix.scale(2F.pow(cameraState.zoom) * densityScale, 2F.pow(cameraState.zoom) * densityScale)
//                val pathCopy = pathComponent.path.copy()
//                pathCopy.transform(matrix)
//                val bounds = pathCopy.getBounds()
//                val tileSize = mapState.mapProperties.tileSize
//                Layout(
//                    modifier = Modifier
//                        .then(pathComponent.gestureDetector?.let { Modifier.sharedPointerInput { it(pathCopy) } } ?: Modifier)
//                        .alpha(0.5F)
//                        .background(Color.Black)
//                        .zIndex(pathComponent.zIndex)
//                        .graphicsLayer {
//                            alpha = pathComponent.alpha
//                            clip = true
//                        }
//                        .drawBehind {
//                            withTransform({
//                                translate((tileSize.width).toFloat() / 2, (tileSize.height).toFloat() / 2)
//                                rotate(rotationDegrees, Offset.Zero)
//                                scale(2F.pow(cameraState.zoom) * densityScale, Offset.Zero)
//                            }) {
//                                drawIntoCanvas { canvas ->
//                                    drawPath(
//                                        path = pathComponent.path,
//                                        color = pathComponent.color,
//                                        colorFilter = pathComponent.colorFilter,
//                                        blendMode = pathComponent.blendMode,
//                                        style = pathComponent.style
//                                    )
//                                }
//                            }
//                        }
//                ) { _, constraints ->
////        layout(constraints.maxWidth, constraints.maxHeight) {}
////
////        layout(bounds.width.toInt(), bounds.height.toInt()) {}
//
//                    layout(
//                        (bounds.width).toInt(),
//                        (bounds.height).toInt()
//                    ) {}
//                }
            }
        )
    }

    override fun paths(
        pathParameter: List<PathParameter>,
        gestureDetection: (suspend PointerInputScope.(Path) -> Unit)?
    ) {
        pathParameter.forEach {
            path(it, gestureDetection)
        }
    }
}
