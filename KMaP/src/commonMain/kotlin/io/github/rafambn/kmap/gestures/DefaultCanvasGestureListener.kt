package io.github.rafambn.kmap.gestures

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Velocity
import io.github.rafambn.kmap.core.state.MapState
import io.github.rafambn.kmap.utils.offsets.DifferentialScreenOffset
import io.github.rafambn.kmap.utils.offsets.ScreenOffset
import io.github.rafambn.kmap.utils.offsets.toCanvasPosition

open class DefaultCanvasGestureListener(private val mapState: MapState) : GestureInterface {
    override fun onTap(screenOffset: ScreenOffset) {
    }

    override fun onDoubleTap(screenOffset: ScreenOffset) {
        mapState.zoomBy(
            -1 / 3F, screenOffset.toCanvasPosition(
                mapState.mapPosition,
                mapState.canvasSize,
                mapState.magnifierScale,
                mapState.zoomLevel,
                mapState.angleDegrees,
                mapState.density,
                mapState.mapProperties.mapSource
            )
        )
    }

    override fun onTwoFingersTap(screenOffset: ScreenOffset) {
        mapState.zoomBy(
            1 / 3F, screenOffset.toCanvasPosition(
                mapState.mapPosition,
                mapState.canvasSize,
                mapState.magnifierScale,
                mapState.zoomLevel,
                mapState.angleDegrees,
                mapState.density,
                mapState.mapProperties.mapSource
            )
        )
    }

    override fun onLongPress(screenOffset: ScreenOffset) {
    }

    override fun onTapLongPress(differentialScreenOffset: DifferentialScreenOffset) {
        mapState.moveBy(
            differentialScreenOffset.toCanvasPosition(
                mapState.magnifierScale,
                mapState.zoomLevel,
                mapState.angleDegrees,
                mapState.density,
                mapState.mapProperties.mapSource
            )
        )
    }

    override fun onTapSwipe(screenOffset: ScreenOffset, zoom: Float) {
        mapState.zoomBy(
            zoom, screenOffset.toCanvasPosition(
                mapState.mapPosition,
                mapState.canvasSize,
                mapState.magnifierScale,
                mapState.zoomLevel,
                mapState.angleDegrees,
                mapState.density,
                mapState.mapProperties.mapSource
            )
        )
    }

    override fun onGesture(screenOffset: ScreenOffset, differentialScreenOffset: DifferentialScreenOffset, zoom: Float, rotation: Float) {
        mapState.rotateBy(
            rotation.toDouble(), screenOffset.toCanvasPosition(
                mapState.mapPosition,
                mapState.canvasSize,
                mapState.magnifierScale,
                mapState.zoomLevel,
                mapState.angleDegrees,
                mapState.density,
                mapState.mapProperties.mapSource
            )
        )
        mapState.zoomBy(
            zoom, screenOffset.toCanvasPosition(
                mapState.mapPosition,
                mapState.canvasSize,
                mapState.magnifierScale,
                mapState.zoomLevel,
                mapState.angleDegrees,
                mapState.density,
                mapState.mapProperties.mapSource
            )
        )
        mapState.moveBy(
            differentialScreenOffset.toCanvasPosition(
                mapState.magnifierScale,
                mapState.zoomLevel,
                mapState.angleDegrees,
                mapState.density,
                mapState.mapProperties.mapSource
            )
        )
    }

    override fun onCtrlGesture(rotation: Float) {
        mapState.rotateBy(rotation.toDouble())
    }

    override fun onDrag(differentialScreenOffset: DifferentialScreenOffset) {
        mapState.moveBy(
            differentialScreenOffset.toCanvasPosition(
                mapState.magnifierScale,
                mapState.zoomLevel,
                mapState.angleDegrees,
                mapState.density,
                mapState.mapProperties.mapSource
            )
        )
    }

    override fun onGestureStart(gestureType: GestureState, screenOffset: ScreenOffset) {
    }

    override fun onGestureEnd(gestureType: GestureState) {
    }

    //TODO fix flings
    override fun onFling(velocity: Velocity) {
        mapState.animatePositionTo(
            Offset(velocity.x, velocity.y).toCanvasPosition(
                mapState.magnifierScale,
                mapState.zoomLevel,
                mapState.angleDegrees,
                mapState.density,
                mapState.mapProperties.mapSource
            ) + mapState.mapPosition
        )
    }

    override fun onFlingZoom(screenOffset: ScreenOffset, velocity: Float) {
        mapState.animateZoomTo(
            velocity + mapState.zoom, position = screenOffset.toCanvasPosition(
                mapState.mapPosition,
                mapState.canvasSize,
                mapState.magnifierScale,
                mapState.zoomLevel,
                mapState.angleDegrees,
                mapState.density,
                mapState.mapProperties.mapSource
            )
        )
    }

    override fun onFlingRotation(screenOffset: ScreenOffset?, velocity: Float) {
        screenOffset?.let {
            mapState.animateRotationTo(
                velocity + mapState.angleDegrees, position = screenOffset.toCanvasPosition(
                    mapState.mapPosition,
                    mapState.canvasSize,
                    mapState.magnifierScale,
                    mapState.zoomLevel,
                    mapState.angleDegrees,
                    mapState.density,
                    mapState.mapProperties.mapSource
                )
            )
        } ?: run {
            mapState.animateRotationTo((velocity + mapState.angleDegrees).toDouble())
        }
    }

    override fun onHover(screenOffset: ScreenOffset) {
    }

    override fun onScroll(screenOffset: ScreenOffset, scrollAmount: Float) {
        mapState.zoomBy(
            scrollAmount, screenOffset.toCanvasPosition(
                mapState.mapPosition,
                mapState.canvasSize,
                mapState.magnifierScale,
                mapState.zoomLevel,
                mapState.angleDegrees,
                mapState.density,
                mapState.mapProperties.mapSource
            )
        )
    }
}