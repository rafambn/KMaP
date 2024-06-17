package io.github.rafambn.kmap

import androidx.compose.ui.unit.Velocity
import io.github.rafambn.kmap.core.motion.MapScrollFactory
import io.github.rafambn.kmap.core.motion.MotionController
import io.github.rafambn.kmap.gestures.GestureState
import io.github.rafambn.kmap.utils.offsets.DifferentialScreenOffset
import io.github.rafambn.kmap.utils.offsets.ScreenOffset

open class DefaultCanvasGestureListener { //TODO(3) make it expect
    private var motionController: MotionController? = null

    internal fun setMotionController(motionController: MotionController) {
        this.motionController = motionController
    }

    fun onTap(screenOffset: ScreenOffset) {
    }

    fun onDoubleTap(screenOffset: ScreenOffset) {
        motionController?.scroll(MapScrollFactory.zoomBy(-1 / 3F, screenOffset))
    }

    fun onTwoFingersTap(screenOffset: ScreenOffset) {
        motionController?.scroll(MapScrollFactory.zoomBy(1 / 3F, screenOffset))
    }

    fun onLongPress(screenOffset: ScreenOffset) {
    }

    fun onTapLongPress(differentialScreenOffset: DifferentialScreenOffset) {
        motionController?.scroll(MapScrollFactory.moveBy(differentialScreenOffset))
    }

    fun onTapSwipe(screenOffset: ScreenOffset, zoom: Float) {
        motionController?.scroll(MapScrollFactory.zoomBy(zoom, screenOffset))
    }

    fun onGesture(screenOffset: ScreenOffset, differentialScreenOffset: DifferentialScreenOffset, zoom: Float, rotation: Float) {
        motionController?.scroll(MapScrollFactory.rotateBy(rotation.toDouble(), screenOffset))
        motionController?.scroll(MapScrollFactory.zoomBy(zoom, screenOffset))
        motionController?.scroll(MapScrollFactory.moveBy(differentialScreenOffset))
    }

    fun onCtrlGesture(rotation: Float) {
        motionController?.scroll(MapScrollFactory.rotateBy(rotation.toDouble()))
    }

    fun onDrag(differentialScreenOffset: DifferentialScreenOffset) {
        motionController?.scroll(MapScrollFactory.moveBy(differentialScreenOffset))
    }

    fun onGestureStart(gestureType: GestureState, screenOffset: ScreenOffset) {
    }

    fun onGestureEnd(gestureType: GestureState) {
    }

    fun onFling(velocity: Velocity) {
//        with(motionController) {
//            motionController.animatePositionTo(Offset(velocity.x, velocity.y).fromScreenOffsetToCanvasPosition() + mapState.mapPosition)
//        }
    }

    fun onFlingZoom(screenOffset: ScreenOffset, velocity: Float) {
//        with(motionController) {
//            motionController.animateZoomTo(velocity + mapState.zoom, position = screenOffset.fromScreenOffsetToCanvasPosition())
//        }
    }

    fun onFlingRotation(screenOffset: ScreenOffset?, velocity: Float) {
//        with(motionController) {
//            screenOffset?.let {
//                motionController.animateRotationTo(velocity + mapState.angleDegrees, position = screenOffset.fromScreenOffsetToCanvasPosition())
//            } ?: run {
//                motionController.animateRotationTo((velocity + mapState.angleDegrees).toDouble())
//            }
//        }
    }

    fun onHover(screenOffset: ScreenOffset) {
    }

    fun onScroll(screenOffset: ScreenOffset, scrollAmount: Float) {
        motionController?.scroll(MapScrollFactory.zoomBy(scrollAmount, screenOffset))
    }
}