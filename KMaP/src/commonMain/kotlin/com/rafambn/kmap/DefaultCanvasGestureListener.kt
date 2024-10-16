package com.rafambn.kmap

import com.rafambn.kmap.core.MotionController
import com.rafambn.kmap.gestures.GestureState
import com.rafambn.kmap.utils.offsets.DifferentialScreenOffset
import com.rafambn.kmap.utils.offsets.ScreenOffset
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
        motionController?.scroll { zoomCentered(-1 / 3F, MotionController.CenterLocation.Offset(screenOffset)) }
    }

    fun onTwoFingersTap(screenOffset: ScreenOffset) {
        motionController?.scroll { zoomCentered(1 / 3F, MotionController.CenterLocation.Offset(screenOffset)) }
    }

    fun onLongPress(screenOffset: ScreenOffset) {
    }

    fun onTapLongPress(differentialScreenOffset: DifferentialScreenOffset) {
        motionController?.scroll { center(MotionController.CenterLocation.Offset(differentialScreenOffset)) }
    }

    fun onTapSwipe(screenOffset: ScreenOffset, zoom: Float) {
        motionController?.scroll { zoomCentered(zoom, MotionController.CenterLocation.Offset(screenOffset)) }
    }

    fun onGesture(screenOffset: ScreenOffset, differentialScreenOffset: DifferentialScreenOffset, zoom: Float, rotation: Float) {
        motionController?.scroll {
            rotateCentered(rotation.toDouble(), MotionController.CenterLocation.Offset(screenOffset))
            zoomCentered(zoom, MotionController.CenterLocation.Offset(screenOffset))
            center(MotionController.CenterLocation.Offset(differentialScreenOffset))
        }
    }

    fun onCtrlGesture(rotation: Float) {
        motionController?.scroll { angle(rotation.toDouble()) }
    }

    fun onDrag(differentialScreenOffset: DifferentialScreenOffset) {
        motionController?.scroll { center(MotionController.CenterLocation.Offset(differentialScreenOffset)) }
    }

    fun onHover(screenOffset: ScreenOffset) {
    }

    fun onScroll(screenOffset: ScreenOffset, scrollAmount: Float) {
        motionController?.scroll { zoomCentered(scrollAmount, MotionController.CenterLocation.Offset(screenOffset)) }
    }
}