package io.github.rafambn.kmap.gestures

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Velocity

interface GestureInterface {
    fun onTap(offset: Offset)
    fun onDoubleTap(offset: Offset)
    fun onTwoFingersTap(offset: Offset)
    fun onLongPress(offset: Offset)
    fun onTapLongPress(offset: Offset)
    fun onTapSwipe(centroid: Offset, zoom: Float)

    fun onGesture(centroid: Offset, pan: Offset, zoom: Float, rotation: Float)

    fun onDrag(dragAmount: Offset)
    fun onGestureStart(gestureType: GestureState, offset: Offset)
    fun onGestureEnd(gestureType: GestureState)

    fun onFling(velocity: Velocity)
    fun onFlingZoom(centroid: Offset, velocity: Float)
    fun onFlingRotation(centroid: Offset, velocity: Float)

    fun onHover(offset: Offset)
    fun onScroll(mouseOffset: Offset, scrollAmount: Float)
}
