package com.rafambn.kmap.components

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.zIndex
import com.rafambn.kmap.components.canvas.CanvasComponent
import com.rafambn.kmap.components.path.PathComponent
import com.rafambn.kmap.components.canvas.tiled.TileRenderResult
import com.rafambn.kmap.components.marker.ClusterComponent
import com.rafambn.kmap.components.marker.ClusterParameters
import com.rafambn.kmap.components.marker.MarkerComponent
import com.rafambn.kmap.components.marker.MarkerParameters
import com.rafambn.kmap.components.path.PathParameter
import com.rafambn.kmap.gestures.sharedPointerInput

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
        var padding = if (pathParameter.style is Stroke) pathParameter.style.width / 2F else 0F
        padding = maxOf(padding, pathParameter.detectionThreshold)
        paths.add(
            PathComponent(
                parameters = pathParameter,
                padding = padding,
                gestureDetector = gestureDetection,
            ) {
                val bounds = pathParameter.path.getBounds()
                Layout(
                    modifier = Modifier
                        .then(gestureDetection?.let { Modifier.sharedPointerInput { it(pathParameter.path) } } ?: Modifier)
                        .alpha(0.5F)//TODO remove later
                        .background(color = Color.Black)
                        .drawBehind {
                            withTransform({ translate(-bounds.left + padding, -bounds.top + padding) }) {
                                drawIntoCanvas { canvas ->
                                    drawPath(
                                        path = pathParameter.path,
                                        color = pathParameter.color,
                                        colorFilter = pathParameter.colorFilter,
                                        blendMode = pathParameter.blendMode,
                                        style = pathParameter.style
                                    )
                                }
                            }
                        }) { _, constraints ->
                    layout((bounds.width + padding * 2).toInt(), (bounds.height + padding * 2).toInt()) {}
                }
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
