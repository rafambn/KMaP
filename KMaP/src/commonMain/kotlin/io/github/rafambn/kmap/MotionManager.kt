package io.github.rafambn.kmap

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.FloatExponentialDecaySpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.generateDecayAnimationSpec
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.toSize
import io.github.rafambn.kmap.enums.MapComponentType
import io.github.rafambn.kmap.gestures.GestureInterface
import io.github.rafambn.kmap.gestures.GestureState
import io.github.rafambn.kmap.gestures.detectMapGestures
import io.github.rafambn.kmap.states.MapState
import kotlinx.coroutines.launch

@Composable
internal fun MotionManager(
    modifier: Modifier = Modifier,
    mapState: MapState,
    content: @Composable () -> Unit
) {

    val coroutineScope = rememberCoroutineScope()

    val flingAnimatable: Animatable<Offset, AnimationVector2D> = Animatable(Offset.Zero, Offset.VectorConverter)
    val flingZoomAnimatable = Animatable(0f)
    val flingRotationAnimatable = Animatable(0f)

    val flingSpec = rememberSplineBasedDecay<Offset>()
    val flingZoomSpec = FloatExponentialDecaySpec().generateDecayAnimationSpec<Float>()
    val flingRotationSpec = FloatExponentialDecaySpec().generateDecayAnimationSpec<Float>()

    val gestureListener = object : GestureInterface {
        override fun onTap(offset: Offset) {
        }

        override fun onDoubleTap(offset: Offset) {
            mapState.scale(offset, -1 / 3F)
        }

        override fun onTwoFingersTap(offset: Offset) {
            mapState.scale(offset, 1 / 3F)
        }

        override fun onLongPress(offset: Offset) {
        }

        override fun onTapLongPress(offset: Offset) {
            mapState.move(offset)
        }

        override fun onTapSwipe(centroid: Offset, zoom: Float) {
            mapState.scale(centroid, zoom)
        }

        override fun onGesture(centroid: Offset, pan: Offset, zoom: Float, rotation: Float) {
            mapState.rotate(centroid, rotation)
            mapState.scale(centroid, zoom)
            mapState.move(pan)
        }

        override fun onDrag(dragAmount: Offset) {
            mapState.move(dragAmount)
        }

        override fun onGestureStart(gestureType: GestureState, offset: Offset) {
        }

        override fun onGestureEnd(gestureType: GestureState) {
        }

        override fun onFling(velocity: Velocity) {
            coroutineScope.launch {
                flingAnimatable.snapTo(Offset.Zero)
                flingAnimatable.animateDecay(
                    initialVelocity = Offset(velocity.x, velocity.y),
                    animationSpec = flingSpec,
                ) {
                    mapState.move(value)
                }
            }
        }

        override fun onFlingZoom(centroid: Offset, velocity: Float) {
            coroutineScope.launch {
                flingZoomAnimatable.snapTo(0F)
                flingZoomAnimatable.animateDecay(
                    initialVelocity = velocity,
                    animationSpec = flingZoomSpec,
                ) {
                    mapState.scale(centroid, value)
                }
            }
        }

        override fun onFlingRotation(centroid: Offset, velocity: Float) {
            coroutineScope.launch {
                flingRotationAnimatable.snapTo(0F)
                flingRotationAnimatable.animateDecay(
                    initialVelocity = velocity,
                    animationSpec = flingRotationSpec,
                ) {
                    mapState.rotate(centroid, value)
                }
            }
        }

        override fun onHover(offset: Offset) {

        }

        override fun onScroll(mouseOffset: Offset, scrollAmount: Float) {
            mapState.scale(mouseOffset, scrollAmount)
        }
    }

    Layout(
        content = content,
        modifier
            .background(Color.Gray)
            .clipToBounds()
            .fillMaxSize()
            .pointerInput(PointerEventPass.Main) {
                detectMapGestures(
                    onTap = { offset -> gestureListener.onTap(offset) },
                    onDoubleTap = { offset -> gestureListener.onDoubleTap(offset) },
                    onTwoFingersTap = { offset -> gestureListener.onTwoFingersTap(offset) },
                    onLongPress = { offset -> gestureListener.onLongPress(offset) },
                    onTapLongPress = { offset -> gestureListener.onTapLongPress(offset) },
                    onTapSwipe = { centroid, zoom -> gestureListener.onTapSwipe(centroid, zoom) },
                    onGesture = { centroid, pan, zoom, rotation -> gestureListener.onGesture(centroid, pan, zoom, rotation) },
                    onDrag = { dragAmount -> gestureListener.onDrag(dragAmount) },
                    onGestureStart = { gestureType, offset -> gestureListener.onGestureStart(gestureType, offset) },
                    onGestureEnd = { gestureType -> gestureListener.onGestureEnd(gestureType) },
                    onFling = { targetLocation -> gestureListener.onFling(targetLocation) },
                    onFlingZoom = { centroid, targetZoom -> gestureListener.onFlingZoom(centroid, targetZoom) },
                    onFlingRotation = { centroid, targetRotation -> gestureListener.onFlingRotation(centroid, targetRotation) },
                    onHover = { offset -> gestureListener.onHover(offset) },
                    onScroll = { mouseOffset, scrollAmount -> gestureListener.onScroll(mouseOffset, scrollAmount) }
                )
            }
            .onGloballyPositioned { coordinates ->
                mapState.mapViewSize = coordinates.size.toSize().toRect().bottomRight
            }
    ) { measurables, constraints ->

        val canvasPlaceable = measurables.first {
            it.layoutId == MapComponentType.CANVAS
        }.measure(constraints)

        layout(constraints.maxWidth, constraints.maxHeight) {
            canvasPlaceable.placeRelativeWithLayer(x = 0, y = 0, zIndex = 0F)
        }
    }
}