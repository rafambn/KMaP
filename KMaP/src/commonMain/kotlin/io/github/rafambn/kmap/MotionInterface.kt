package io.github.rafambn.kmap

import androidx.compose.animation.core.Spring
import io.github.rafambn.kmap.utils.Degrees

interface MotionInterface {

    fun setCenter(position: Position)

    fun moveBy(position: Position)

    fun animatePositionTo(position: Position, stiffness: Float = Spring.StiffnessMedium)

    fun setZoom(zoom: Float)

    fun zoomBy(zoom: Float, position: Position? = null)

    fun animateZoomTo(zoom: Float, stiffness: Float = Spring.StiffnessMedium , position: Position? = null)

    fun setRotation(angle: Degrees)

    fun rotateBy(angle: Degrees, position: Position? = null)

    fun animateRotationTo(angle: Degrees, stiffness: Float = Spring.StiffnessMedium, position: Position? = null)

    fun setPosZoomRotate(position: Position, zoom: Float, angle: Degrees)

    fun animatePosZoomRotate(position: Position, zoom: Float, angle: Degrees, stiffness: Float = Spring.StiffnessMedium)
}