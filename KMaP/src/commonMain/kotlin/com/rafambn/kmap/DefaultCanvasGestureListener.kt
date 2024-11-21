package com.rafambn.kmap

import com.rafambn.kmap.core.MotionController
import com.rafambn.kmap.gestures.GestureState
import com.rafambn.kmap.utils.DifferentialScreenOffset
import com.rafambn.kmap.utils.ScreenOffset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

open class DefaultCanvasGestureListener {
    private var motionController: MotionController? = null

    internal val _currentGestureFlow = MutableStateFlow(GestureState.START_GESTURE)
    val currentGestureFlow = _currentGestureFlow.asStateFlow()

    internal fun setMotionController(motionController: MotionController) {
        this.motionController = motionController
    }

    fun onTap(screenOffset: ScreenOffset) {
    }

    fun onDoubleTap(screenOffset: ScreenOffset) {
        motionController?.move { zoomByCentered(-1 / 3F, screenOffset) }
    }

    fun onTwoFingersTap(screenOffset: ScreenOffset) {
        motionController?.move { zoomByCentered(1 / 3F, screenOffset) }
    }

    fun onLongPress(screenOffset: ScreenOffset) {
    }

    fun onTapLongPress(differentialScreenOffset: DifferentialScreenOffset) {
        motionController?.move { positionBy(differentialScreenOffset) }
    }

    fun onTapSwipe(screenOffset: ScreenOffset, zoom: Float) {
        motionController?.move { zoomByCentered(zoom, screenOffset) }
    }

    fun onGesture(screenOffset: ScreenOffset, differentialScreenOffset: DifferentialScreenOffset, zoom: Float, rotation: Float) {
        motionController?.move {
            rotateByCentered(rotation.toDouble(), screenOffset)
            zoomByCentered(zoom, screenOffset)
            positionBy(differentialScreenOffset)
        }
    }

    fun onCtrlGesture(rotation: Float) {
        motionController?.move { rotateBy(rotation.toDouble()) }
    }

    fun onDrag(differentialScreenOffset: DifferentialScreenOffset) {
        motionController?.move { positionBy(differentialScreenOffset) }
    }

    fun onHover(screenOffset: ScreenOffset) {
    }

    fun onScroll(screenOffset: ScreenOffset, scrollAmount: Float) {
        motionController?.move { zoomByCentered(scrollAmount, screenOffset) }
    }
}