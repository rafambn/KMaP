package com.rafambn.kmap.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
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
import com.rafambn.kmap.mapSource.tiled.raster.RasterTileCanvas
import com.rafambn.kmap.mapSource.tiled.vector.VectorTileCanvas
import com.rafambn.kmap.utils.ProjectedCoordinates
import com.rafambn.kmap.utils.ScreenOffset
import com.rafambn.kmap.utils.plus

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
        val canvasIds = canvas.map { it.parameters.id }
        require(canvasIds.size == canvasIds.toSet().size) {
            "Canvas must have different ids"
        }
        mapState.canvasKernel.refreshCanvas(canvas.map { it.parameters })
    }

    fun rasterCanvas(
        parameters: RasterCanvasParameters,
        gestureWrapper: MapGestureWrapper? = null
    ) {
        canvas.add(
            Canvas(parameters) {
                RasterTileCanvas(
                    id = parameters.id,
                    canvasSize = mapState.cameraState.canvasSize,
                    gestureWrapper = gestureWrapper,
                    tileLayers = mapState.drawTileLayers,
                    magnifierScale = mapState.drawMagScale,
                    positionOffset = mapState.drawReference,
                    tileSize = mapState.drawTileSize,
                    rotationDegrees = mapState.drawRotationDegrees,
                    translation = mapState.drawTranslation,
                )
            }
        )
    }

    fun vectorCanvas(
        parameters: VectorCanvasParameters,
        gestureWrapper: MapGestureWrapper? = null
    ) {
        canvas.add(
            Canvas(parameters) {
                VectorTileCanvas(
                    id = parameters.id,
                    canvasSize = mapState.cameraState.canvasSize,
                    gestureWrapper = gestureWrapper,
                    tileLayers = mapState.drawTileLayers,
                    style = parameters.style,
                    magnifierScale = mapState.drawMagScale,
                    positionOffset = mapState.drawReference,
                    tileSize = mapState.drawTileSize,
                    rotationDegrees = mapState.drawRotationDegrees,
                    translation = mapState.drawTranslation,
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
        val orientationX = (mapState.mapProperties.coordinatesRange.longitude.orientation).toFloat()
        val orientationY = (mapState.mapProperties.coordinatesRange.latitude.orientation).toFloat()
        orientationMatrix.scale(orientationX, orientationY)
        val scaleMatrix = Matrix()
        val scaleX = (mapState.mapProperties.tileSize.width / mapState.mapProperties.coordinatesRange.longitude.span).toFloat()
        val scaleY = (mapState.mapProperties.tileSize.height / mapState.mapProperties.coordinatesRange.latitude.span).toFloat()
        scaleMatrix.scale(scaleX, scaleY)
        originalPath.transform(orientationMatrix)
        originalPath.transform(scaleMatrix)
        val densityScale = Matrix()
        densityScale.scale(mapState.density.density, mapState.density.density)
        originalPath.transform(densityScale)
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
                                    path = originalPath,
                                    threshold = padding,
                                    checkForInsideClick = parameters.checkForClickInsidePath,
                                    convertScreenOffsetToProjectedCoordinates = {
                                        val untranslatedPoint = it.plus(ScreenOffset(bounds.left - padding, bounds.top - padding))
                                        return@detectPathGestures ProjectedCoordinates(
                                            untranslatedPoint.x * orientationX / (scaleX * mapState.density.density),
                                            untranslatedPoint.y * orientationY / (scaleY * mapState.density.density),
                                        )
                                    }
                                )
                            }
                        } ?: Modifier)
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
