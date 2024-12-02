package com.rafambn.kmap

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.rafambn.kmap.core.CanvasParameters
import com.rafambn.kmap.core.ClusterParameters
import com.rafambn.kmap.core.Component
import com.rafambn.kmap.core.ComponentType
import com.rafambn.kmap.core.MapComponentInfo
import com.rafambn.kmap.core.MarkerParameters
import com.rafambn.kmap.core.MotionController
import com.rafambn.kmap.core.PathParameters
import com.rafambn.kmap.core.TileCanvas
import com.rafambn.kmap.core.componentInfo
import com.rafambn.kmap.core.state.MapState
import com.rafambn.kmap.utils.asOffset
import kotlin.math.pow

@Composable
fun KMaP(
    modifier: Modifier = Modifier,
    motionController: MotionController,
    mapState: MapState,
    content: KMaPScope.() -> Unit //TODO understand why this reference doesnt change for markers map
) {
    val density = LocalDensity.current
    LaunchedEffect(Unit) {
        motionController.setMap(mapState)
        mapState.setDensity(density)
    }
    //TODO improve this code to prevent unnecessary recompositions
    val kmapContent = KMaPContent(content)
    kmapContent.updateCluster(mapState)
    Layout(
        content = {
            kmapContent.visibleMarkers.forEach {
                Layout(
                    content = { it.markerContent.invoke(it.markerParameters) },
                    modifier = Modifier.Companion.componentInfo(
                        MapComponentInfo(
                            it.markerParameters, it.placementOffset.asOffset(), ComponentType.MARKER
                        )
                    ),
                    measurePolicy = { measurables, constraints ->
                        if (measurables.isEmpty())
                            return@Layout layout(0, 0) {}
                        val listPlaceables = measurables.map { measurable ->
                            measurable.measure(constraints)
                        }
                        val maxWidth = listPlaceables.maxOf { placeable -> placeable.width }
                        val maxHeight = listPlaceables.maxOf { placeable -> placeable.height }

                        layout(maxWidth, maxHeight) {
                            listPlaceables.forEach { placeable ->
                                placeable.place(
                                    x = 0,
                                    y = 0
                                )
                            }
                        }
                    }
                )
            }
            kmapContent.visibleClusters.forEach {
                Layout(
                    content = { it.clusterContent.invoke(it.clusterParameters, it.size) },
                    modifier = Modifier.componentInfo(MapComponentInfo(it.clusterParameters, it.placementOffset.asOffset(), ComponentType.CLUSTER)),
                    measurePolicy = { measurables, constraints ->
                        if (measurables.isEmpty())
                            return@Layout layout(0, 0) {}
                        val listPlaceables = measurables.map { measurable ->
                            measurable.measure(constraints)
                        }
                        val maxWidth = listPlaceables.maxOf { placeable -> placeable.width }
                        val maxHeight = listPlaceables.maxOf { placeable -> placeable.height }

                        layout(maxWidth, maxHeight) {
                            listPlaceables.forEach { placeable ->
                                placeable.place(
                                    x = 0,
                                    y = 0
                                )
                            }
                        }
                    }
                )
            }
            kmapContent.visibleCanvas.forEach {
                Layout(
                    content = {
                        TileCanvas(
                            getTile = it.getTile,
                            magnifierScale = mapState.magnifierScale,
                            rotationDegrees = mapState.cameraState.angleDegrees.toFloat(),
                            translation = mapState.cameraState.canvasSize.asOffset() / 2F,
                            tileSize = mapState.mapProperties.tileSize,
                            positionOffset = mapState.drawReference,
                            zoomLevel = mapState.zoomLevel,
                            visibleTiles = mapState.visibleTiles,
                            maxCacheTiles = it.canvasParameters.maxCacheTiles
                        )
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .componentInfo(MapComponentInfo(it.canvasParameters, Offset.Zero, ComponentType.CANVAS))
                        .then(it.gestureDetection?.let { Modifier.pointerInput(PointerEventPass.Main) { it(this) } } ?: Modifier),
                    measurePolicy = { measurables, constraints ->
                        val placeable = measurables.first().measure(constraints)
                        layout(placeable.width, placeable.height) {
                            placeable.place(
                                x = 0,
                                y = 0
                            )
                        }
                    }
                )
            }
//            PathMeasure(path, false)
            val path = Path().apply {
                moveTo(0f, 0f)
                lineTo(200f, 100f)
                lineTo(200f, 200f)
                lineTo(100f, 300f)
                close()
            }
            Layout(
                content = {
                    val halfScreen = mapState.cameraState.canvasSize.asOffset() / 2F
                    Canvas(
                        modifier = Modifier
                    ) {
                        println("${mapState.drawReference} -- ${mapState.pathDrawReference}")
                        withTransform({
                            scale(mapState.cameraState.zoom + 1F)
                            rotate(mapState.cameraState.angleDegrees.toFloat())
                            translate(mapState.pathDrawReference.horizontal.toFloat() + halfScreen.x, mapState.pathDrawReference.vertical.toFloat() + halfScreen.y)
                        }) {
                            drawPath(
                                path = path,
                                color = Color.Red,
                                style = Stroke(
                                    width = 4.dp.toPx(),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                )
                            )
                        }
                    }
                },
                modifier = Modifier
                    .size(mapState.mapProperties.tileSize.dp)
                    .componentInfo(MapComponentInfo(PathParameters(zIndex = 5F), Offset.Zero, ComponentType.PATH))
//                        .then(it.gestureDetection?.let { Modifier.pointerInput(PointerEventPass.Main) { it(this) } } ?: Modifier),
                ,
                measurePolicy = { measurables, constraints ->
                    val placeable = measurables.first().measure(constraints)
                    layout(placeable.width, placeable.height) {
                        placeable.place(
                            x = 0,
                            y = 0
                        )
                    }
                }
            )
        },
        modifier
            .background(Color.Gray)
            .clipToBounds()
            .wrapContentSize()
            .onGloballyPositioned { coordinates ->
                mapState.setCanvasSize(
                    Offset(
                        coordinates.size.width.toFloat(),
                        coordinates.size.height.toFloat()
                    )
                )
            }
    ) { measurables, constraints ->
        val canvasComponent = measurables
            .filter { it.componentInfo.type == ComponentType.CANVAS }
            .map { Component(it.componentInfo.data, it.componentInfo.placementOffset, it.measure(constraints)) }

        val markersComponent = measurables
            .filter { it.componentInfo.type == ComponentType.MARKER }
            .map { Component(it.componentInfo.data, it.componentInfo.placementOffset, it.measure(constraints)) }

        val clusterComponent = measurables
            .filter { it.componentInfo.type == ComponentType.CLUSTER }
            .map { Component(it.componentInfo.data, it.componentInfo.placementOffset, it.measure(constraints)) }

        val pathComponent = measurables
            .filter { it.componentInfo.type == ComponentType.PATH }
            .map { Component(it.componentInfo.data, it.componentInfo.placementOffset, it.measure(constraints)) }

        layout(constraints.maxWidth, constraints.maxHeight) {
            canvasComponent.forEach {
                val componentData = it.data as CanvasParameters
                it.placeable.placeWithLayer(
                    x = 0,
                    y = 0,
                    zIndex = componentData.zIndex
                ) {
                    alpha = componentData.alpha
                }
            }
            pathComponent.forEach {
                val componentData = it.data as PathParameters
                it.placeable.placeWithLayer(
                    x = 0,
                    y = 0,
                    zIndex = componentData.zIndex
                ) {
                    alpha = componentData.alpha
                }
            }
            markersComponent.forEach {
                val componentData = it.data as MarkerParameters
                val componentOffset = it.placementOffset
                it.placeable.placeWithLayer(
                    x = 0,
                    y = 0,
                    zIndex = componentData.zIndex
                ) {
                    alpha = componentData.alpha
                    translationX = componentOffset.x - componentData.drawPosition.x * it.placeable.width
                    translationY = componentOffset.y - componentData.drawPosition.y * it.placeable.height
                    transformOrigin = TransformOrigin(componentData.drawPosition.x, componentData.drawPosition.y)
                    componentData.zoomToFix?.let { zoom ->
                        scaleX = 2F.pow(mapState.cameraState.zoom - zoom)
                        scaleY = 2F.pow(mapState.cameraState.zoom - zoom)
                    }
                    rotationZ =
                        if (componentData.rotateWithMap)
                            (mapState.cameraState.angleDegrees + componentData.rotation).toFloat()
                        else
                            componentData.rotation.toFloat()
                }
            }
            clusterComponent.forEach {
                val componentData = it.data as ClusterParameters
                val componentOffset = it.placementOffset
                it.placeable.placeWithLayer(
                    x = 0,
                    y = 0,
                    zIndex = componentData.zIndex
                ) {
                    alpha = componentData.alpha
                    translationX = componentOffset.x - componentData.drawPosition.x * it.placeable.width
                    translationY = componentOffset.y - componentData.drawPosition.y * it.placeable.height
                    transformOrigin = TransformOrigin(componentData.drawPosition.x, componentData.drawPosition.y)
                    rotationZ =
                        if (componentData.rotateWithMap)
                            (mapState.cameraState.angleDegrees + componentData.rotation).toFloat()
                        else
                            componentData.rotation.toFloat()
                }
            }
        }
    }
}