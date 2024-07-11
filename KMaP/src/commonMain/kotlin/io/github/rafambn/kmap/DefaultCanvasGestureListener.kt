package io.github.rafambn.kmap

import io.github.rafambn.kmap.core.MotionController
import io.github.rafambn.kmap.core.MotionController.CenterLocation
import io.github.rafambn.kmap.gestures.GestureState
import io.github.rafambn.kmap.utils.offsets.DifferentialScreenOffset
import io.github.rafambn.kmap.utils.offsets.ScreenOffset
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
        motionController?.scroll { zoomCentered(-1 / 3F, CenterLocation.Offset(screenOffset)) }
    }

    fun onTwoFingersTap(screenOffset: ScreenOffset) {
        motionController?.scroll { zoomCentered(1 / 3F, CenterLocation.Offset(screenOffset)) }
    }

    fun onLongPress(screenOffset: ScreenOffset) {
    }

    fun onTapLongPress(differentialScreenOffset: DifferentialScreenOffset) {
        motionController?.scroll { center(CenterLocation.Offset(differentialScreenOffset)) }
    }

    fun onTapSwipe(screenOffset: ScreenOffset, zoom: Float) {
        motionController?.scroll { zoomCentered(zoom, CenterLocation.Offset(screenOffset)) }
    }

    fun onGesture(screenOffset: ScreenOffset, differentialScreenOffset: DifferentialScreenOffset, zoom: Float, rotation: Float) {
        motionController?.scroll {
            rotateCentered(rotation.toDouble(), CenterLocation.Offset(screenOffset))
            zoomCentered(zoom, CenterLocation.Offset(screenOffset))
            center(CenterLocation.Offset(differentialScreenOffset))
        }
    }

    fun onCtrlGesture(rotation: Float) {
        motionController?.scroll { angle(rotation.toDouble()) }
    }

    fun onDrag(differentialScreenOffset: DifferentialScreenOffset) {
        motionController?.scroll { center(CenterLocation.Offset(differentialScreenOffset)) }
    }

    fun onHover(screenOffset: ScreenOffset) {
    }

    fun onScroll(screenOffset: ScreenOffset, scrollAmount: Float) {
        motionController?.scroll { zoomCentered(scrollAmount, CenterLocation.Offset(screenOffset)) }
    }
}