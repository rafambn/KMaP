package io.github.rafambn.kmap.gestures

import androidx.compose.ui.unit.Velocity
import io.github.rafambn.kmap.core.state.MotionController
import io.github.rafambn.kmap.utils.offsets.DifferentialScreenOffset
import io.github.rafambn.kmap.utils.offsets.ScreenOffset

open class DefaultCanvasGestureListener(private val motionController: MotionController) : GestureInterface {
    override fun onTap(screenOffset: ScreenOffset) {
    }

    override fun onDoubleTap(screenOffset: ScreenOffset) {
        with(motionController) {
            motionController.zoomBy(-1 / 3F, screenOffset.fromScreenOffsetToCanvasPosition())
        }
    }

    override fun onTwoFingersTap(screenOffset: ScreenOffset) {
        with(motionController) {
            motionController.zoomBy(1 / 3F, screenOffset.fromScreenOffsetToCanvasPosition())
        }
    }

    override fun onLongPress(screenOffset: ScreenOffset) {
    }

    override fun onTapLongPress(differentialScreenOffset: DifferentialScreenOffset) {
        with(motionController) {
            motionController.moveBy(differentialScreenOffset.fromDifferentialScreenOffsetToCanvasPosition())
        }
    }

    override fun onTapSwipe(screenOffset: ScreenOffset, zoom: Float) {
        with(motionController) {
            motionController.zoomBy(zoom, screenOffset.fromDifferentialScreenOffsetToCanvasPosition())
        }
    }

    override fun onGesture(screenOffset: ScreenOffset, differentialScreenOffset: DifferentialScreenOffset, zoom: Float, rotation: Float) {
        with(motionController) {
            motionController.rotateBy(rotation.toDouble(), screenOffset.fromScreenOffsetToCanvasPosition())
            motionController.zoomBy(zoom, screenOffset.fromScreenOffsetToCanvasPosition())
            motionController.moveBy(differentialScreenOffset.fromScreenOffsetToCanvasPosition())
        }
    }

    override fun onCtrlGesture(rotation: Float) {
        with(motionController) {
            motionController.rotateBy(rotation.toDouble())
        }
    }

    override fun onDrag(differentialScreenOffset: DifferentialScreenOffset) {
        with(motionController) {
            motionController.moveBy(differentialScreenOffset.fromDifferentialScreenOffsetToCanvasPosition())
        }
    }

    override fun onGestureStart(gestureType: GestureState, screenOffset: ScreenOffset) {
    }

    override fun onGestureEnd(gestureType: GestureState) {
    }

    //TODO fix flings
    override fun onFling(velocity: Velocity) {
//        with(motionController) {
//            motionController.animatePositionTo(Offset(velocity.x, velocity.y).fromScreenOffsetToCanvasPosition() + mapState.mapPosition)
//        }
    }

    override fun onFlingZoom(screenOffset: ScreenOffset, velocity: Float) {
//        with(motionController) {
//            motionController.animateZoomTo(velocity + mapState.zoom, position = screenOffset.fromScreenOffsetToCanvasPosition())
//        }
    }

    override fun onFlingRotation(screenOffset: ScreenOffset?, velocity: Float) {
//        with(motionController) {
//            screenOffset?.let {
//                motionController.animateRotationTo(velocity + mapState.angleDegrees, position = screenOffset.fromScreenOffsetToCanvasPosition())
//            } ?: run {
//                motionController.animateRotationTo((velocity + mapState.angleDegrees).toDouble())
//            }
//        }
    }

    override fun onHover(screenOffset: ScreenOffset) {
    }

    override fun onScroll(screenOffset: ScreenOffset, scrollAmount: Float) {
        with(motionController) {
            motionController.zoomBy(scrollAmount, screenOffset.fromScreenOffsetToCanvasPosition())
        }
    }
}