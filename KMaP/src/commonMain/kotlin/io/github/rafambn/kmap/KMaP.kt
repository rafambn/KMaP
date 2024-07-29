package io.github.rafambn.kmap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import io.github.rafambn.kmap.core.CanvasData
import io.github.rafambn.kmap.core.Component
import io.github.rafambn.kmap.core.ComponentType
import io.github.rafambn.kmap.core.MapComponentInfo
import io.github.rafambn.kmap.core.MarkerData
import io.github.rafambn.kmap.core.MotionController
import io.github.rafambn.kmap.core.TileCanvas
import io.github.rafambn.kmap.core.componentInfo
import io.github.rafambn.kmap.core.state.MapState
import io.github.rafambn.kmap.gestures.detectMapGestures
import io.github.rafambn.kmap.utils.offsets.ScreenOffset
import kotlin.math.pow

@Composable
fun KMaP(
    modifier: Modifier = Modifier,
    motionController: MotionController,
    mapState: MapState,
    canvasGestureListener: DefaultCanvasGestureListener = DefaultCanvasGestureListener(),
    content: KMaPConfig.() -> Unit
) {
    val kmapContent = remember { KMaPContent().also { content.invoke(it) } }
    val density = LocalDensity.current
    LaunchedEffect(Unit) {
        motionController.setMap(mapState)
        canvasGestureListener.setMotionController(motionController)
        mapState.setDensity(density)
    }
    Layout(
        content = {
            kmapContent.markers.forEach {
                Layout(
                    content = { it.second.invoke(it.first) },
                    modifier = Modifier.componentInfo(MapComponentInfo(it.first, ComponentType.MARKER)),
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
            kmapContent.canvas.forEach {
                Layout(
                    content = {
                        TileCanvas(
                            it.second,
                            mapState.magnifierScale,
                            mapState.angleDegrees.toFloat(),
                            mapState.canvasSize / 2F,
                            mapState.mapProperties.tileSize,
                            mapState.drawReference,
                            mapState.zoomLevel,
                            mapState.visibleTiles
                        )
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .componentInfo(MapComponentInfo(it.first, ComponentType.CANVAS)),
                    measurePolicy = { measurables, constraints ->
                        if (measurables.isEmpty())
                            throw IllegalArgumentException("No canvas declared")
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
        },
        modifier
            .background(Color.Gray)
            .clipToBounds()
            .wrapContentSize()
            .onGloballyPositioned { coordinates ->
                mapState.canvasSize = Offset(
                    coordinates.size.width.toFloat(),
                    coordinates.size.height.toFloat()
                )
            }
            .pointerInput(PointerEventPass.Main) {
                detectMapGestures(
                    onTap = { offset -> canvasGestureListener.onTap(offset) },
                    onDoubleTap = { offset -> canvasGestureListener.onDoubleTap(offset) },
                    onLongPress = { offset -> canvasGestureListener.onLongPress(offset) },
                    onTapLongPress = { offset -> canvasGestureListener.onTapLongPress(offset) },
                    onTapSwipe = { centroid, zoom -> canvasGestureListener.onTapSwipe(centroid, zoom) },
                    onDrag = { dragAmount -> canvasGestureListener.onDrag(dragAmount) },
                    onTwoFingersTap = { offset -> canvasGestureListener.onTwoFingersTap(offset) },
                    onGesture = { centroid, pan, zoom, rotation -> canvasGestureListener.onGesture(centroid, pan, zoom, rotation) },
                    onHover = { offset -> canvasGestureListener.onHover(offset) },
                    onScroll = { mouseOffset, scrollAmount -> canvasGestureListener.onScroll(mouseOffset, scrollAmount) },
                    onCtrlGesture = { rotation -> canvasGestureListener.onCtrlGesture(rotation) },
                    currentGestureFlow = canvasGestureListener._currentGestureFlow
                )
            }
    ) { measurables, constraints ->
        val canvasComponent = measurables
            .filter { it.componentInfo.type == ComponentType.CANVAS }
            .map { Component(it.componentInfo.data, it.measure(constraints)) }

        val markersComponent = measurables
            .filter { it.componentInfo.type == ComponentType.MARKER }
            .map { Component(it.componentInfo.data, it.measure(constraints)) }

        val clusterComponent = measurables
            .filter { it.componentInfo.type == ComponentType.CLUSTER }
            .map { Component(it.componentInfo.data, it.measure(constraints)) }

        layout(constraints.maxWidth, constraints.maxHeight) {
            canvasComponent.forEach {
                val componentData = it.data as CanvasData
                it.placeable.placeRelative(
                    x = 0,
                    y = 0,
                    zIndex = componentData.zIndex
                )
            }
            markersComponent.forEach {
                val componentData = it.data as MarkerData
                val coordinates: ScreenOffset = with(mapState) {//TODO make it so that this context change doesn't happen here
                    componentData.coordinates.toCanvasPosition().toScreenOffset()
                }
                it.placeable.placeWithLayer(
                    x = 0,
                    y = 0,
                    zIndex = componentData.zIndex
                ) {
                    alpha = componentData.alpha
                    translationX = coordinates.x - componentData.drawPosition.x * it.placeable.width
                    translationY = coordinates.y - componentData.drawPosition.y * it.placeable.height
                    transformOrigin = TransformOrigin(componentData.drawPosition.x, componentData.drawPosition.y)
                    if (componentData.scaleWithMap) {
                        scaleX = 2F.pow(mapState.zoom - componentData.zoomToFix)
                        scaleY = 2F.pow(mapState.zoom - componentData.zoomToFix)
                    }
                    rotationZ =
                        if (componentData.rotateWithMap)
                            (mapState.angleDegrees + componentData.rotation).toFloat()
                        else
                            componentData.rotation.toFloat()
                }
            }
        }
    }
}