package com.rafambn.kmap.lazyMarker

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
import com.rafambn.kmap.components.Canvas
import com.rafambn.kmap.components.CanvasParameters
import com.rafambn.kmap.components.Cluster
import com.rafambn.kmap.components.ClusterParameters
import com.rafambn.kmap.components.Marker
import com.rafambn.kmap.components.MarkerParameters
import com.rafambn.kmap.components.Path
import com.rafambn.kmap.components.PathParameters
import com.rafambn.kmap.gestures.sharedPointerInput
import com.rafambn.kmap.tiles.TileRenderResult

class KMaPContent(
    content: KMaPContent.() -> Unit,
) {

    val markers = mutableListOf<Marker>()
    val cluster = mutableListOf<Cluster>()
    val canvas = mutableListOf<Canvas>()
    val paths = mutableListOf<Path>()

    init {
        apply(content)
    }

    fun canvas(
        parameters: CanvasParameters = CanvasParameters(1F, 0F),
        maxCacheTiles: Int = 20,
        tileSource: suspend (zoom: Int, row: Int, column: Int) -> TileRenderResult,
        gestureDetection: (suspend PointerInputScope.() -> Unit)? = null
    ) {
        canvas.add(Canvas(parameters, maxCacheTiles, tileSource, gestureDetection))
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
            Path(parameters, gestureDetection){
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
