package com.rafambn.kmap.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.copy
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.Layout
import com.rafambn.kmap.MapState
import com.rafambn.kmap.components.parameters.*
import com.rafambn.kmap.geometry.plane.ProjectedCoordinates
import com.rafambn.kmap.geometry.plane.ScreenOffset
import com.rafambn.kmap.gesture.MapGestureWrapper
import com.rafambn.kmap.gesture.PathGestureWrapper
import com.rafambn.kmap.gesture.internal.detectPathGestures
import com.rafambn.kmap.gesture.internal.sharedPointerInput
import com.rafambn.kmap.source.internal.render.RasterTileCanvas
import com.rafambn.kmap.source.internal.render.VectorTileCanvas

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
                    gestureWrapper = gestureWrapper,
                    activeTiles = { mapState.canvasKernel.getActiveTiles(parameters.id) },
                    magnifierScale = mapState.drawMagScale,
                    positionOffset = mapState.drawReference,
                    tileSize = mapState.drawTileSize,
                    rotationDegrees = mapState.drawRotationDegrees,
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
                    gestureWrapper = gestureWrapper,
                    activeTiles = { mapState.canvasKernel.getActiveTiles(parameters.id) },
                    magnifierScale = mapState.drawMagScale,
                    positionOffset = mapState.drawReference,
                    tileSize = mapState.drawTileSize,
                    rotationDegrees = mapState.drawRotationDegrees,
                    style = { parameters.style },
                    zoom = { mapState.internalCameraState.zoom.toDouble() }
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
        val padding = parameters.totalPadding
        val orientationMatrix = Matrix()
        val orientationX = (mapState.mapProperties.coordinatesRange.longitude.orientation).toFloat()
        val orientationY = (mapState.mapProperties.coordinatesRange.latitude.orientation).toFloat()
        orientationMatrix.scale(orientationX, orientationY)
        val scaleMatrix = Matrix()
        val scale = with(mapState) {
            val scaleX = (mapState.mapProperties.tileSize.width.value / mapState.mapProperties.coordinatesRange.longitude.span).toFloat()
            val scaleY = (mapState.mapProperties.tileSize.height.value / mapState.mapProperties.coordinatesRange.latitude.span).toFloat()
            Offset(scaleX, scaleY)
        }
        scaleMatrix.scale(scale.x, scale.y)
        originalPath.transform(orientationMatrix)
        originalPath.transform(scaleMatrix)
        val densityScale = Matrix()
        densityScale.scale(mapState.density, mapState.density)
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
                                        val untranslatedPoint = it + ScreenOffset(
                                            (bounds.left - padding).toDouble(),
                                            (bounds.top - padding).toDouble()
                                        )
                                        return@detectPathGestures ProjectedCoordinates(
                                            untranslatedPoint.x * orientationX / (scale.x * mapState.density),
                                            untranslatedPoint.y * orientationY / (scale.y * mapState.density),
                                        )
                                    }
                                )
                            }
                        } ?: Modifier)
                        .drawBehind {
                            withTransform({ translate(-bounds.left + padding, -bounds.top + padding) }) {
                                drawIntoCanvas {
                                    drawPath(
                                        path = originalPath,
                                        color = parameters.color,
                                        colorFilter = parameters.colorFilter,
                                        blendMode = parameters.blendMode,
                                        style = parameters.style
                                    )
                                }
                            }
                        }) { _, _ ->
                    layout((bounds.width + padding * 2).toInt(), (bounds.height + padding * 2).toInt()) {}
                }
            }
        )
    }
}
