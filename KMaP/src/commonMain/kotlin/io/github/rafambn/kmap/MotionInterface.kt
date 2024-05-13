package io.github.rafambn.kmap

import io.github.rafambn.kmap.utils.Degrees

interface MotionInterface {

    fun setCenter(position: Position)

    fun moveBy(position: Position)

    fun animatePositionTo(position: Position, decayRate: Double = -5.0)

    fun setZoom(zoom: Float)

    fun zoomBy(zoom: Float)

    fun zoomBy(zoom: Float, position: Position)

    fun animateZoomTo(zoom: Float, decayRate: Double = -5.0)

    fun animateZoomTo(zoom: Float, decayRate: Double = -5.0, position: Position)

    fun setRotation(angle: Degrees)

    fun rotateBy(angle: Degrees)

    fun rotateBy(angle: Degrees, position: Position)

    fun animateRotationTo(angle: Degrees, decayRate: Double = -5.0)

    fun animateRotationTo(angle: Degrees, decayRate: Double = -5.0, position: Position)
}