package io.github.rafambn.kmap.core.motion

import io.github.rafambn.kmap.core.state.MapState
import io.github.rafambn.kmap.utils.Degrees
import io.github.rafambn.kmap.utils.offsets.CanvasPosition
import io.github.rafambn.kmap.utils.offsets.ProjectedCoordinates

typealias MilliSeconds = Long

object MapAnimateFactory {
    fun animatePositionTo(position: CanvasPosition, decayRate: Double, duration: MilliSeconds): (MapState) -> Unit = { mapState ->
//        val startPosition = mapState.rawPosition
//        flingPositionJob = decayValue(coroutineScope, decayRate) {
//            mapState.rawPosition = lerp(startPosition, position, it).coerceInMap()
//            mapState.updateState()
//        }
    }

    fun animatePositionTo(coordinates: ProjectedCoordinates, decayRate: Double, duration: MilliSeconds): (MapState) -> Unit = { mapState ->
//        val startPosition = mapState.rawPosition
//        flingPositionJob = decayValue(coroutineScope, decayRate) {
//            mapState.rawPosition = lerp(startPosition, position, it).coerceInMap()
//            mapState.updateState()
//        }
    }

    fun animateZoomTo(zoom: Float, decayRate: Double, duration: MilliSeconds): (MapState) -> Unit = { mapState ->
//        val startZoom = mapState.zoom
//        flingZoomJob = decayValue(coroutineScope, decayRate) { decayValue ->
//            mapState.zoom = lerp(startZoom, zoom, decayValue.toFloat()).coerceZoom()
//            mapState.updateState()
//        }
    }

    fun animateZoomTo(zoom: Float, position: CanvasPosition, decayRate: Double, duration: MilliSeconds): (MapState) -> Unit = { mapState ->
//        val startZoom = mapState.zoom
//        val previousOffset =
//            position.toScreenOffset()
//        flingZoomAtPositionJob = decayValue(coroutineScope, decayRate) { decayValue ->
//            mapState.zoom = lerp(startZoom, zoom, decayValue.toFloat()).coerceZoom()
//            centerPositionAtOffset(position, previousOffset)
//            mapState.updateState()
//        }
    }

    fun animateZoomTo(zoom: Float, coordinates: ProjectedCoordinates, decayRate: Double, duration: MilliSeconds): (MapState) -> Unit =
        { mapState ->
//        val startZoom = mapState.zoom
//        val previousOffset =
//            position.toScreenOffset()
//        flingZoomAtPositionJob = decayValue(coroutineScope, decayRate) { decayValue ->
//            mapState.zoom = lerp(startZoom, zoom, decayValue.toFloat()).coerceZoom()
//            centerPositionAtOffset(position, previousOffset)
//            mapState.updateState()
//        }
        }

    fun animateRotationTo(angle: Degrees, decayRate: Double, duration: MilliSeconds): (MapState) -> Unit = { mapState ->
//        val startRotation = mapState.angleDegrees
//        flingRotationJob = decayValue(coroutineScope, decayRate) { decayValue ->
//            mapState.angleDegrees = lerp(startRotation, angle, decayValue)
//            mapState.updateState()
//        }
    }

    fun animateRotationTo(angle: Degrees, position: CanvasPosition, decayRate: Double, duration: MilliSeconds): (MapState) -> Unit =
        { mapState ->
//        val startRotation = mapState.angleDegrees
//        val previousOffset =
//            position.toScreenOffset()
//        flingRotationAtPositionJob = decayValue(coroutineScope, decayRate) { decayValue ->
//            mapState.angleDegrees = lerp(startRotation, angle, decayValue)
//            centerPositionAtOffset(position, previousOffset)
//            mapState.updateState()
//        }
        }

    fun animateRotationTo(angle: Degrees, coordinates: ProjectedCoordinates, decayRate: Double, duration: MilliSeconds): (MapState) -> Unit =
        { mapState ->
//        val startRotation = mapState.angleDegrees
//        val previousOffset =
//            position.toScreenOffset()
//        flingRotationAtPositionJob = decayValue(coroutineScope, decayRate) { decayValue ->
//            mapState.angleDegrees = lerp(startRotation, angle, decayValue)
//            centerPositionAtOffset(position, previousOffset)
//            mapState.updateState()
//        }
        }
}