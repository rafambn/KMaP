package io.github.rafambn.kmap.gestures

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Velocity
import io.github.rafambn.kmap.utils.DifferentialScreenOffset
import io.github.rafambn.kmap.utils.ScreenOffset

interface GestureInterface {
    fun onTap(screenOffset: ScreenOffset)
    fun onDoubleTap(screenOffset: ScreenOffset)
    fun onTwoFingersTap(screenOffset: ScreenOffset)
    fun onLongPress(screenOffset: ScreenOffset)
    fun onTapLongPress(differentialScreenOffset: DifferentialScreenOffset)
    fun onTapSwipe(screenOffset: ScreenOffset, zoom: Float)

    fun onGesture(screenOffset: ScreenOffset, differentialScreenOffset: DifferentialScreenOffset, zoom: Float, rotation: Float)

    fun onCtrlGesture(rotation: Float)

    fun onDrag(differentialScreenOffset: DifferentialScreenOffset)
    fun onGestureStart(gestureType: GestureState, screenOffset: ScreenOffset)
    fun onGestureEnd(gestureType: GestureState)

    fun onFling(velocity: Velocity)
    fun onFlingZoom(screenOffset: ScreenOffset, velocity: Float)
    fun onFlingRotation(screenOffset: ScreenOffset?, velocity: Float)

    fun onHover(screenOffset: ScreenOffset)
    fun onScroll(screenOffset: ScreenOffset, scrollAmount: Float)
}
