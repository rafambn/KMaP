package io.github.rafambn.kmap

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Velocity
import io.github.rafambn.kmap.gestures.GestureInterface
import io.github.rafambn.kmap.gestures.GestureState

open class DefaultCanvasGestureListener(private val mapState: MapState) : GestureInterface {
    override fun onTap(offset: Offset) {
    }

    override fun onDoubleTap(centroid: Offset) {
        mapState.zoomBy(-1 / 3F, mapState.offsetToMapReference(centroid))
    }

    override fun onTwoFingersTap(centroid: Offset) {
        mapState.zoomBy(1 / 3F, mapState.offsetToMapReference(centroid))
    }

    override fun onLongPress(offset: Offset) {
    }

    override fun onTapLongPress(offset: Offset) {
        mapState.moveBy(mapState.differentialOffsetToMapReference(offset))
    }

    override fun onTapSwipe(centroid: Offset, zoom: Float) {
        mapState.zoomBy(zoom, mapState.offsetToMapReference(centroid))
    }

    override fun onGesture(centroid: Offset, pan: Offset, zoom: Float, rotation: Float) {
        mapState.rotateBy(rotation.toDouble(), mapState.offsetToMapReference(centroid))
        mapState.zoomBy(zoom, mapState.offsetToMapReference(centroid))
        mapState.moveBy(mapState.differentialOffsetToMapReference(pan))
    }

    override fun onCtrlGesture(rotation: Float) {
        mapState.rotateBy(rotation.toDouble())
    }

    override fun onDrag(offset: Offset) {
        mapState.moveBy(mapState.differentialOffsetToMapReference(offset))
    }

    override fun onGestureStart(gestureType: GestureState, offset: Offset) {
    }

    override fun onGestureEnd(gestureType: GestureState) {
    }

    override fun onFling(velocity: Velocity) {
//        mapState.animatePositionTo(mapState.differentialOffsetToMapReference(Offset(velocity.x, velocity.y)) + mapState.mapPosition)
    }

    override fun onFlingZoom(centroid: Offset, velocity: Float) {
//        mapState.animateZoomTo(velocity + mapState.zoom, position = mapState.offsetToMapReference(centroid))
    }

    override fun onFlingRotation(centroid: Offset?, velocity: Float) {
//        centroid?.let {
//            mapState.animateRotationTo((velocity + mapState.angleDegrees).toDouble(), position = mapState.offsetToMapReference(it))
//        } ?: run {
//            mapState.animateRotationTo((velocity + mapState.angleDegrees).toDouble())
//        }
    }

    override fun onHover(offset: Offset) {
    }

    override fun onScroll(mouseOffset: Offset, scrollAmount: Float) {
        mapState.zoomBy(scrollAmount, mapState.offsetToMapReference(mouseOffset))
    }
}