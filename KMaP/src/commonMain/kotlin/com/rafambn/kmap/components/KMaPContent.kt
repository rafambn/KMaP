package com.rafambn.kmap.components

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.copy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.Layout
import com.rafambn.kmap.core.MapState
import com.rafambn.kmap.gestures.MapGestureWrapper
import com.rafambn.kmap.gestures.PathGestureWrapper
import com.rafambn.kmap.gestures.detectPathGestures
import com.rafambn.kmap.gestures.sharedPointerInput
import com.rafambn.kmap.tiles.TileCanvas
import com.rafambn.kmap.utils.ProjectedCoordinates
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
        mapState.canvasKernel.refreshCanvas(canvas.map { it.parameters })
    }

    fun canvas(
        parameters: CanvasParameters,
        gestureWrapper: MapGestureWrapper? = null
    ) {
        canvas.add(
            Canvas(
                parameters
            ) {
                TileCanvas(
                    canvasSize = mapState.cameraState.canvasSize,
                    magnifierScale = mapState.cameraState.zoom - mapState.cameraState.zoom.toIntFloor(),
                    positionOffset = mapState.drawReference,
                    tileSize = mapState.mapProperties.tileSize,
                    rotationDegrees = mapState.cameraState.angleDegrees.toFloat(),
                    translation = mapState.cameraState.canvasSize.asOffset() / 2F,
                    gestureWrapper = gestureWrapper,
                    tileLayers = mapState.canvasKernel.getTileLayers(parameters.id)
                )
            }
        )
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
        gestureWrapper: PathGestureWrapper? = null
    ) {
        val originalPath = parameters.path.copy()
        var padding = if (parameters.style is Stroke) parameters.style.width / 2F else 0F
        padding = maxOf(padding, parameters.clickPadding)
        parameters.totalPadding = padding
        val unmodBounds = originalPath.getBounds()
        val pointY = if (mapState.mapProperties.coordinatesRange.latitude.orientation == 1)
            unmodBounds.top else unmodBounds.bottom
        val pointX = if (mapState.mapProperties.coordinatesRange.longitude.orientation == 1)
            unmodBounds.left else unmodBounds.right
        val topLeft = ProjectedCoordinates(pointX, pointY)
        parameters.drawPoint = topLeft
        val orientationMatrix = Matrix()
        orientationMatrix.scale(
            (mapState.mapProperties.coordinatesRange.longitude.orientation).toFloat(),
            (mapState.mapProperties.coordinatesRange.latitude.orientation).toFloat()
        )
        val scaleMatrix = Matrix()
        scaleMatrix.scale(
            (mapState.mapProperties.tileSize.width / mapState.mapProperties.coordinatesRange.longitude.span).toFloat(),
            (mapState.mapProperties.tileSize.height / mapState.mapProperties.coordinatesRange.latitude.span).toFloat()
        )
        originalPath.transform(orientationMatrix)
        originalPath.transform(scaleMatrix)
        val bounds = originalPath.getBounds()
        paths.add(
            Path(parameters) {
                Layout(
                    modifier = Modifier
                        .then(gestureWrapper?.let {
                            Modifier.sharedPointerInput {
                                detectPathGestures(
                                    onTap = gestureWrapper.onTap,
                                    onDoubleTap = gestureWrapper.onDoubleTap,
                                    onLongPress = gestureWrapper.onLongPress,
                                    mapState = mapState,
                                    path = originalPath,
                                    threshold = parameters.clickPadding
                                )
                            } } ?: Modifier)
                        .alpha(0.5F)//TODO remove later
                        .background(color = Color.Black)
                        .drawBehind {
                            withTransform({ translate(-bounds.left + padding, -bounds.top + padding) }) {
                                drawIntoCanvas { canvas ->
                                    drawPath(
                                        path = originalPath,
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
