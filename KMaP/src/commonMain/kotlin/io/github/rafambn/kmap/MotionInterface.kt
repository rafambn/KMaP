package io.github.rafambn.kmap

import io.github.rafambn.kmap.utils.CanvasPosition
import io.github.rafambn.kmap.utils.Degrees

interface MotionInterface {

    fun setCenter(position: CanvasPosition)

    fun moveBy(position: CanvasPosition)

    fun animatePositionTo(position: CanvasPosition, decayRate: Double = -5.0)

    fun setZoom(zoom: Float)

    fun zoomBy(zoom: Float)

    fun zoomBy(zoom: Float, position: CanvasPosition)

    fun animateZoomTo(zoom: Float, decayRate: Double = -5.0)

    fun animateZoomTo(zoom: Float, decayRate: Double = -5.0, position: CanvasPosition)

    fun setRotation(angle: Degrees)

    fun rotateBy(angle: Degrees)

    fun rotateBy(angle: Degrees, position: CanvasPosition)

    fun animateRotationTo(angle: Degrees, decayRate: Double = -5.0)

    fun animateRotationTo(angle: Degrees, decayRate: Double = -5.0, position: CanvasPosition)
}