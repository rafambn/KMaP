package com.rafambn.kmap.components

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.layout.Layout
import com.rafambn.kmap.core.MapState
import com.rafambn.kmap.gestures.sharedPointerInput
import com.rafambn.kmap.tiles.TileCanvas
import com.rafambn.kmap.utils.asOffset
import com.rafambn.kmap.utils.toIntFloor

class KMaPContent(
    content: KMaPContent.() -> Unit,
    val mapState: MapState,
) {

    val markers = mutableListOf<Marker>()
    val cluster = mutableListOf<Cluster>()
    val canvas = mutableListOf<Canvas>()
    val paths = mutableListOf<Path>()

    init {
        apply(content)
        mapState.canvasKernel.clearOldCanvas(canvas.map { it.parameters.id })
    }

    fun canvas(
        parameters: CanvasParameters,
        gestureDetection: (suspend PointerInputScope.() -> Unit)? = null
    ) {
        canvas.add(
            Canvas(
                parameters,
                gestureDetection
            ) {
                TileCanvas(
                    canvasSize = mapState.cameraState.canvasSize,
                    magnifierScale = mapState.cameraState.zoom - mapState.cameraState.zoom.toIntFloor(),
                    positionOffset = mapState.drawReference,
                    tileSize = mapState.mapProperties.tileSize,
                    rotationDegrees = mapState.cameraState.angleDegrees.toFloat(),
                    translation = mapState.cameraState.canvasSize.asOffset() / 2F,
                    gestureDetection = gestureDetection,
                    tileLayers = mapState.canvasKernel.getTileLayers(parameters.id)
                )
            }
        )
        mapState.canvasKernel.addCanvas(parameters)
    }

    inline fun <T : MarkerParameters> marker(
        marker: T,
        crossinline itemContent: @Composable (item: T) -> Unit
    ) {
        markers.add(Marker(marker, { itemContent(marker) }))
    }

    inline fun <T : MarkerParameters> markers(
        markers: List<T>,
        crossinline itemContent: @Composable (item: T, index: Int) -> Unit
    ) {
        markers.forEachIndexed { index, it ->
            this.markers.add(Marker(it, { itemContent(it, index) }))
        }
    }

    inline fun <T : ClusterParameters> cluster(
        cluster: T,
        crossinline itemContent: @Composable (item: T) -> Unit
    ) {
        this.cluster.add(Cluster(cluster, { itemContent(cluster) }))
    }

    fun path(
        parameters: PathParameters,
        gestureDetection: (suspend PointerInputScope.(path: androidx.compose.ui.graphics.Path) -> Unit)? = null
    ) {
        var padding = if (parameters.style is Stroke) parameters.style.width / 2F else 0F
        padding = maxOf(padding, parameters.clickPadding)
        paths.add(
            Path(parameters, gestureDetection) {
                val bounds = parameters.path.getBounds()
                Layout(
                    modifier = Modifier
                        .then(gestureDetection?.let { Modifier.sharedPointerInput { it(parameters.path) } } ?: Modifier)
                        .alpha(0.5F)//TODO remove later
                        .background(color = Color.Black)
                        .drawBehind {
                            withTransform({ translate(-bounds.left + padding, -bounds.top + padding) }) {
                                drawIntoCanvas { canvas ->
                                    drawPath(
                                        path = parameters.path,
                                        color = parameters.color,
                                        colorFilter = parameters.colorFilter,
                                        blendMode = parameters.blendMode,
                                        style = parameters.style
                                    )
                                }
                            }
                        }) { _, constraints ->
                    layout((bounds.width + padding * 2).toInt(), (bounds.height + padding * 2).toInt()) {}
                }
            }
        )
    }
}
