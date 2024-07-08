package io.github.rafambn.kmap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.fastForEachReversed
import io.github.rafambn.kmap.config.DefaultMapProperties
import io.github.rafambn.kmap.config.MapProperties
import io.github.rafambn.kmap.config.characteristics.MapSource
import io.github.rafambn.kmap.core.CanvasData
import io.github.rafambn.kmap.core.ComponentType
import io.github.rafambn.kmap.core.MotionController
import io.github.rafambn.kmap.core.PlacerData
import io.github.rafambn.kmap.core.componentData
import io.github.rafambn.kmap.core.state.MapState
import io.github.rafambn.kmap.gestures.detectMapGestures
import io.github.rafambn.kmap.model.Component
import io.github.rafambn.kmap.utils.offsets.ScreenOffset
import kotlin.math.pow
import kotlin.math.sqrt

@Composable
fun KMaP(
    modifier: Modifier = Modifier,
    motionController: MotionController,
    mapProperties: MapProperties = DefaultMapProperties(),
    mapState: MapState,
    mapSource: MapSource,
    canvasGestureListener: DefaultCanvasGestureListener = DefaultCanvasGestureListener(),
    onCanvasChangeSize: (Offset) -> Unit,
    content: @Composable KMaPScope.() -> Unit = {}
) {
    val density = LocalDensity.current
    LaunchedEffect(Unit) {
        motionController.setMap(mapState)
        canvasGestureListener.setMotionController(motionController)
        mapState.setProperties(mapProperties)
        mapState.setMapSource(mapSource)
        mapState.setDensity(density)
    }
    Layout(
        content = {
            KMaPScope.content()
            mapState.trigger.value
        },
        modifier
            .background(Color.Gray)
            .clipToBounds()
            .wrapContentSize()
            .onGloballyPositioned { coordinates -> onCanvasChangeSize(coordinates.size.toSize().toRect().bottomRight) }
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
        val canvasComponents: List<Component> = measurables
            .filter { it.componentData.componentType == ComponentType.CANVAS }
            .map { Component(it.componentData.componentData, it.measure(constraints)) } //TODO improve naming

        val placersComponents: MutableList<Component> = measurables
            .filter { it.componentData.componentType == ComponentType.PLACER }
            .map { Component(it.componentData.componentData, it.measure(constraints)) }
            .toMutableList()

        val groupComponents: MutableList<Component> = measurables
            .filter { it.componentData.componentType == ComponentType.GROUP }
            .map { Component(it.componentData.componentData, it.measure(constraints)) }
            .toMutableList()

        layout(constraints.maxWidth, constraints.maxHeight) {
            //place all canvas
            canvasComponents.forEach {
                it.placeable.placeRelative(
                    x = 0,
                    y = 0,
                    zIndex = (it.data as CanvasData).zIndex
                )
            }
            //filter not visible placers
            placersComponents.fastForEachReversed {
                val coordinates = with(mapState) {
                    mapState.mapSource.toCanvasPosition((it.data as PlacerData).coordinates).toScreenOffset()
                }
                val isOutsideHorizontalBounds = coordinates.x > it.placeable.width + mapState.canvasSize.x || coordinates.x < -it.placeable.width
                val isOutsideVerticalBounds = coordinates.y > it.placeable.height + mapState.canvasSize.y || coordinates.y < -it.placeable.height
                if (isOutsideHorizontalBounds && isOutsideVerticalBounds)
                    placersComponents.remove(it)
            }
            groupComponents[0].placeable.placeWithLayer(
                x = 0,
                y = 0,
                zIndex = 0F
            ) {
                translationX = 0F
                translationY = 0F
                transformOrigin = TransformOrigin(0.5F, 0.5F)
            }
            groupComponents[0].placeable.placeWithLayer(
                x = 0,
                y = 0,
                zIndex = 0F
            ) {
                translationX = mapState.canvasSize.x
                translationY = 0F
                transformOrigin = TransformOrigin(0.5F, 0.5F)
            }
//            groupComponents.forEach { component ->
//                val placerOfGroup = placersComponents.filter { (it.data as PlacerData).tag == (component.data as GroupData).tag }.toMutableList()
//                if (placerOfGroup.size < 2)
//                    return@forEach
//                var lastRadius: Float? = null
//                var lastCoordinates: Offset? = null
//                var pairMedianCoordinates: Pair<Int, Offset> = Pair(0, Offset.Zero)
//                placerOfGroup.forEach { placer ->
//                    lastRadius?.let {
//                        val thisRadius =
//                            sqrt(placer.placeable.height.toFloat().pow(2F) + placer.placeable.width.toFloat().pow(2F))
//                        val thisCoordinates = with(mapState) {
//                            mapState.mapSource.toCanvasPosition((placer.data as PlacerData).coordinates).toScreenOffset()
//                        }
//                        if (distance(thisCoordinates, lastCoordinates!!) < thisRadius + lastRadius!!) {
//                            placersComponents.remove(placerOfGroup.first())
//                            placersComponents.remove(placer)
//                            val midPoint = midpoint(eachCoordinates, lastCoordinates)
//                            component.placeable.placeWithLayer(
//                                x = 0,
//                                y = 0,
//                                zIndex = 0F
//                            ) {
//                                translationX = midPoint.x
//                                translationY = midPoint.y
//                                transformOrigin = TransformOrigin(0.5F, 0.5F)
//                            }
//                        }
//                    } ?: run {
//                        lastRadius = sqrt(placer.placeable.height.toFloat().pow(2F) + placer.placeable.width.toFloat().pow(2F))
//                        lastCoordinates = with(mapState) {
//                            mapState.mapSource.toCanvasPosition((placer.data as PlacerData).coordinates).toScreenOffset()
//                        }
//                        pairMedianCoordinates = Pair(pairMedianCoordinates.first + 1, pairMedianCoordinates.second + lastCoordinates!!)
//                        return@forEach
//                    }
//                }
//            }

            //place leftovers
            placersComponents.forEach {
                val placerData = it.data as PlacerData
                val coordinates: ScreenOffset = with(mapState) {
                    mapState.mapSource.toCanvasPosition(placerData.coordinates).toScreenOffset()
                }
                it.placeable.placeWithLayer(
                    x = 0,
                    y = 0,
                    zIndex = placerData.zIndex
                ) {
                    alpha = placerData.alpha
                    translationX = coordinates.x - placerData.drawPosition.x * it.placeable.width
                    translationY = coordinates.y - placerData.drawPosition.y * it.placeable.height
                    transformOrigin = TransformOrigin(placerData.drawPosition.x, placerData.drawPosition.y)
                    if (placerData.scaleWithMap) {
                        scaleX = 2F.pow(mapState.zoom - placerData.zoomToFix)
                        scaleY = 2F.pow(mapState.zoom - placerData.zoomToFix)
                    }
                    rotationZ =
                        if (placerData.rotateWithMap)
                            (mapState.angleDegrees + placerData.rotation).toFloat()
                        else
                            placerData.rotation.toFloat()
                }
            }
        }
    }
}

fun distance(point1: Offset, point2: Offset): Float {
    val xDifference = point2.x - point1.x
    val yDifference = point2.y - point1.y
    return sqrt(xDifference.pow(2) + yDifference.pow(2))
}

fun midpoint(point1: Offset, point2: Offset): Offset {
    val midX = (point1.x + point2.x) / 2
    val midY = (point1.y + point2.y) / 2
    return Offset(midX, midY)
}