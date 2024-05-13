package io.github.rafambn.kmap

import io.github.rafambn.kmap.utils.Degrees

interface MotionInterface {

    fun setCenter(position: Position)

    fun moveBy(position: Position)

    fun animatePositionTo(position: Position, decayRate: Double = -5.0)

    fun setZoom(zoom: Float)

    fun zoomBy(zoom: Float, position: Position? = null)

    fun animateZoomTo(zoom: Float, decayRate: Double = -5.0, position: Position? = null)

    fun setRotation(angle: Degrees)

    fun rotateBy(angle: Degrees, position: Position? = null)

    fun animateRotationTo(angle: Degrees, decayRate: Double = -5.0, position: Position? = null)

    fun setPosZoomRotate(position: Position, zoom: Float, angle: Degrees)

    fun animatePosZoomRotate(position: Position, zoom: Float, angle: Degrees, decayRate: Double = -5.0)
}