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
import io.github.rafambn.kmap.config.DefaultMapProperties
import io.github.rafambn.kmap.config.MapProperties
import io.github.rafambn.kmap.config.characteristics.MapSource
import io.github.rafambn.kmap.core.ComponentType
import io.github.rafambn.kmap.core.MapComponentData
import io.github.rafambn.kmap.core.MotionController
import io.github.rafambn.kmap.core.componentData
import io.github.rafambn.kmap.core.state.MapState
import io.github.rafambn.kmap.gestures.detectMapGestures
import io.github.rafambn.kmap.utils.offsets.ScreenOffset
import kotlin.math.pow

@Composable
fun KMaP(
    modifier: Modifier = Modifier,
    motionController: MotionController,
    mapProperties: MapProperties = DefaultMapProperties(),
    mapState: MapState,
    mapSource: MapSource, //TODO(2): make have multiple tile canvas
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
                    onTwoFingersTap = { offset -> canvasGestureListener.onTwoFingersTap(offset) },
                    onLongPress = { offset -> canvasGestureListener.onLongPress(offset) },
                    onTapLongPress = { offset -> canvasGestureListener.onTapLongPress(offset) },
                    onTapSwipe = { centroid, zoom -> canvasGestureListener.onTapSwipe(centroid, zoom) },
                    onGesture = { centroid, pan, zoom, rotation -> canvasGestureListener.onGesture(centroid, pan, zoom, rotation) },
                    onDrag = { dragAmount -> canvasGestureListener.onDrag(dragAmount) },
                    onGestureStart = { gestureType, offset -> canvasGestureListener.onGestureStart(gestureType, offset) },
                    onGestureEnd = { gestureType -> canvasGestureListener.onGestureEnd(gestureType) },
                    onHover = { offset -> canvasGestureListener.onHover(offset) },
                    onScroll = { mouseOffset, scrollAmount -> canvasGestureListener.onScroll(mouseOffset, scrollAmount) },
                    onCtrlGesture = { rotation -> canvasGestureListener.onCtrlGesture(rotation) }
                )
            }
    ) { measurables, constraints ->
        val canvasData: List<MapComponentData>
        val canvasPlaceable = measurables
            .filter { it.componentData.componentType == ComponentType.CANVAS }
            .also { measurableCanvas -> canvasData = measurableCanvas.map { it.componentData } }
            .map { it.measure(constraints) }

        val placersData: List<MapComponentData>
        val placersPlaceable = measurables
            .filter { it.componentData.componentType == ComponentType.PLACER }
            .also { measurableMarkers -> placersData = measurableMarkers.map { it.componentData } }
            .map { it.measure(constraints) }

        layout(constraints.maxWidth, constraints.maxHeight) {
            canvasPlaceable.forEachIndexed { index, placeable ->
                placeable.placeRelative(
                    x = 0,
                    y = 0,
                    zIndex = canvasData[index].placer.zIndex
                )
            }
            placersPlaceable.forEachIndexed { index, placeable ->
                val coordinates: ScreenOffset = with(mapState) {
                    mapState.mapSource.toCanvasPosition(placersData[index].placer.coordinates).toScreenOffset()
                }
                placeable.placeWithLayer(
                    x = 0,
                    y = 0,
                    zIndex = placersData[index].placer.zIndex
                ) {
                    alpha = placersData[index].placer.alpha
                    translationX = coordinates.x - placersData[index].placer.drawPosition.x * placeable.width
                    translationY = coordinates.y - placersData[index].placer.drawPosition.y * placeable.height
                    transformOrigin = TransformOrigin(placersData[index].placer.drawPosition.x, placersData[index].placer.drawPosition.y)
                    if (placersData[index].placer.scaleWithMap) {
                        scaleX = 2F.pow(mapState.zoom - placersData[index].placer.zoomToFix)
                        scaleY = 2F.pow(mapState.zoom - placersData[index].placer.zoomToFix)
                    }
                    rotationZ =
                        if (placersData[index].placer.rotateWithMap)
                            (mapState.angleDegrees + placersData[index].placer.rotation).toFloat()
                        else
                            placersData[index].placer.rotation.toFloat()
                }
            }
        }
    }
}