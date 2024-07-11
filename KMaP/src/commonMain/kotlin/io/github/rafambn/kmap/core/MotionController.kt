package io.github.rafambn.kmap.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.util.lerp
import io.github.rafambn.kmap.core.state.MapState
import io.github.rafambn.kmap.utils.lerp
import io.github.rafambn.kmap.utils.offsets.CanvasPosition
import io.github.rafambn.kmap.utils.offsets.ProjectedCoordinates
import io.github.rafambn.kmap.utils.offsets.ScreenOffset
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.exp

typealias MilliSeconds = Long

@Composable
fun rememberMotionController(): MotionController = remember {
    MotionController()
}

class MotionController {
    private var mapState: MapState? = null
    private var onMapChanged: ((MapState) -> Unit)? = null
    private var animationJob: Job? = null
    private var cancellableContinuation: CancellableContinuation<Unit>? = null

    sealed class CenterLocation {
        data class Position(val position: CanvasPosition) : CenterLocation()
        data class Coordinates(val projectedCoordinates: ProjectedCoordinates) : CenterLocation()
        data class Offset(val offset: ScreenOffset) : CenterLocation()
    }

    interface MoveInterface {
        fun center(center: CenterLocation)
        fun zoom(zoom: Float)
        fun zoomCentered(zoom: Float, center: CenterLocation)
        fun angle(degrees: Double)
        fun rotateCentered(degrees: Double, center: CenterLocation)
    }

    interface AnimationInterface {
        suspend fun positionTo(center: CenterLocation, decayRate: Double = 5.0, duration: MilliSeconds = 1000)
        suspend fun positionBy(center: CenterLocation, decayRate: Double = 5.0, duration: MilliSeconds = 1000)
        suspend fun zoomTo(zoom: Float, decayRate: Double = 5.0, duration: MilliSeconds = 1000)
        suspend fun zoomBy(zoom: Float, decayRate: Double = 5.0, duration: MilliSeconds = 1000)
        suspend fun zoomToCentered(zoom: Float, center: CenterLocation, decayRate: Double = 5.0, duration: MilliSeconds = 1000)
        suspend fun zoomByCentered(zoom: Float, center: CenterLocation, decayRate: Double = 5.0, duration: MilliSeconds = 1000)
        suspend fun angleTo(degrees: Double, decayRate: Double = 5.0, duration: MilliSeconds = 1000)
        suspend fun angleBy(degrees: Double, decayRate: Double = 5.0, duration: MilliSeconds = 1000)
        suspend fun rotateToCentered(degrees: Double, center: CenterLocation, decayRate: Double = 5.0, duration: MilliSeconds = 1000)
        suspend fun rotateByCentered(degrees: Double, center: CenterLocation, decayRate: Double = 5.0, duration: MilliSeconds = 1000)
    }

    internal fun setMap(mapState: MapState) {
        this.mapState = mapState
        onMapChanged?.let {
            onMapChanged = null
            it(mapState)
        }
        cancellableContinuation?.resume(Unit)
    }

    fun set(block: MoveInterface.() -> Unit) {
        animationJob?.let {
            it.cancel(CancellationException("Animation cancelled by set"))
            onMapChanged = null
        }
        val map = mapState
        if (map == null) {
            onMapChanged = {
                block(setObject)
                it.updateState()
                onMapChanged?.let { callback -> callback(it) }
            }
        } else {
            block(setObject)
            map.updateState()
        }
    }

    fun scroll(block: MoveInterface.() -> Unit) {
        animationJob?.let {
            it.cancel(CancellationException("Animation cancelled by scroll"))
            onMapChanged = null
        }
        val map = mapState
        if (map == null) {
            onMapChanged = {
                block(scrollObject)
                it.updateState()
                onMapChanged?.let { callback -> callback(it) }
            }
        } else {
            block(scrollObject)
            map.updateState()
        }
    }

    suspend fun animate(block: suspend AnimationInterface.() -> Unit) {
        val myJob = currentCoroutineContext()[Job]
        animationJob = myJob
        animationJob?.invokeOnCompletion {
            it?.let {
                if (it !is CancellationException) {
                    throw it
                }
                animationJob = null
                cancellableContinuation = null
            } ?: run {
                animationJob = null
                cancellableContinuation = null
            }
        }
        try {
            val map = mapState
            if (map == null) {
                suspendCancellableCoroutine { cancellableContinuation ->
                    this.cancellableContinuation = cancellableContinuation
                }
                block(animationObject)
            } else
                block(animationObject)
        } finally {
            animationJob = null
            cancellableContinuation = null
        }
    }

    private val setObject = object : MoveInterface {
        override fun center(center: CenterLocation) {
            when (center) {
                is CenterLocation.Offset -> with(mapState!!) {
                    mapState!!.rawPosition = center.offset.fromScreenOffsetToCanvasPosition()
                }

                is CenterLocation.Position -> mapState!!.rawPosition = center.position
                is CenterLocation.Coordinates -> mapState!!.projection = center.projectedCoordinates
            }
        }

        override fun zoom(zoom: Float) {
            mapState!!.zoom = zoom
        }

        override fun zoomCentered(zoom: Float, center: CenterLocation) {
            when (center) {
                is CenterLocation.Coordinates -> {
                    with(mapState!!) {
                        val previousOffset = center.projectedCoordinates.toCanvasPosition().toScreenOffset()
                        val previousPosition = center.projectedCoordinates.toCanvasPosition()
                        mapState!!.zoom = zoom
                        centerPositionAtOffset(previousPosition, previousOffset)
                    }
                }

                is CenterLocation.Offset -> {
                    with(mapState!!) {
                        val position = center.offset.fromScreenOffsetToCanvasPosition()
                        this.zoom = zoom
                        centerPositionAtOffset(position, center.offset)
                    }
                }

                is CenterLocation.Position -> {
                    with(mapState!!) {
                        val previousOffset = center.position.toScreenOffset()
                        this.zoom = zoom
                        centerPositionAtOffset(center.position, previousOffset)
                    }
                }
            }
        }

        override fun angle(degrees: Double) {
            mapState!!.angleDegrees = degrees
        }

        override fun rotateCentered(degrees: Double, center: CenterLocation) {
            when (center) {
                is CenterLocation.Coordinates -> {
                    with(mapState!!) {
                        val previousOffset = center.projectedCoordinates.toCanvasPosition().toScreenOffset()
                        val previousPosition = center.projectedCoordinates.toCanvasPosition()
                        mapState!!.angleDegrees = degrees
                        centerPositionAtOffset(previousPosition, previousOffset)
                    }
                }

                is CenterLocation.Offset -> {
                    with(mapState!!) {
                        val position = center.offset.fromScreenOffsetToCanvasPosition()
                        this.angleDegrees = degrees
                        centerPositionAtOffset(position, center.offset)
                    }
                }

                is CenterLocation.Position -> {
                    with(mapState!!) {
                        val previousOffset = center.position.toScreenOffset()
                        this.angleDegrees = degrees
                        centerPositionAtOffset(center.position, previousOffset)
                    }
                }
            }
        }
    }

    private val scrollObject = object : MoveInterface {
        override fun center(center: CenterLocation) {
            when (center) {
                is CenterLocation.Offset -> with(mapState!!) {
                    mapState!!.rawPosition += center.offset.fromDifferentialScreenOffsetToCanvasPosition()
                }

                is CenterLocation.Position -> mapState!!.rawPosition += center.position
                is CenterLocation.Coordinates -> mapState!!.projection += center.projectedCoordinates
            }
        }

        override fun zoom(zoom: Float) {
            mapState!!.zoom += zoom
        }

        override fun zoomCentered(zoom: Float, center: CenterLocation) {
            when (center) {
                is CenterLocation.Coordinates -> {
                    with(mapState!!) {
                        val previousOffset = center.projectedCoordinates.toCanvasPosition().toScreenOffset()
                        val previousPosition = center.projectedCoordinates.toCanvasPosition()
                        mapState!!.zoom += zoom
                        centerPositionAtOffset(previousPosition, previousOffset)
                    }
                }

                is CenterLocation.Offset -> {
                    with(mapState!!) {
                        val position = center.offset.fromScreenOffsetToCanvasPosition()
                        this.zoom += zoom
                        centerPositionAtOffset(position, center.offset)
                    }
                }

                is CenterLocation.Position -> {
                    with(mapState!!) {
                        val previousOffset = center.position.toScreenOffset()
                        this.zoom += zoom
                        centerPositionAtOffset(center.position, previousOffset)
                    }
                }
            }
        }

        override fun angle(degrees: Double) {
            mapState!!.angleDegrees += degrees
        }

        override fun rotateCentered(degrees: Double, center: CenterLocation) {
            when (center) {
                is CenterLocation.Coordinates -> {
                    with(mapState!!) {
                        val previousOffset = center.projectedCoordinates.toCanvasPosition().toScreenOffset()
                        val previousPosition = center.projectedCoordinates.toCanvasPosition()
                        mapState!!.angleDegrees += degrees
                        centerPositionAtOffset(previousPosition, previousOffset)
                    }
                }

                is CenterLocation.Offset -> {
                    with(mapState!!) {
                        val position = center.offset.fromScreenOffsetToCanvasPosition()
                        this.angleDegrees += degrees
                        centerPositionAtOffset(position, center.offset)
                    }
                }

                is CenterLocation.Position -> {
                    with(mapState!!) {
                        val previousOffset = center.position.toScreenOffset()
                        this.angleDegrees += degrees
                        centerPositionAtOffset(center.position, previousOffset)
                    }
                }
            }
        }
    }

    private val animationObject = object : AnimationInterface {
        override suspend fun positionTo(center: CenterLocation, decayRate: Double, duration: MilliSeconds) {
            if (decayRate <= 0.0)
                throw IllegalArgumentException("decay rate must be greater than 0")
            if (duration <= 0)
                throw IllegalArgumentException("duration must be greater than 0")
            val startPosition = mapState!!.rawPosition
            val endPosition = when (center) {
                is CenterLocation.Coordinates -> {
                    with(mapState!!) {
                        center.projectedCoordinates.toCanvasPosition()
                    }
                }

                is CenterLocation.Offset -> {
                    with(mapState!!) {
                        center.offset.fromScreenOffsetToCanvasPosition()
                    }
                }

                is CenterLocation.Position -> center.position
            }
            decayValue(decayRate, duration) {
                mapState!!.rawPosition = lerp(startPosition, endPosition, it)
                mapState!!.updateState()
            }
        }

        override suspend fun positionBy(center: CenterLocation, decayRate: Double, duration: MilliSeconds) {
            if (decayRate <= 0.0)
                throw IllegalArgumentException("decay rate must be greater than 0")
            if (duration <= 0)
                throw IllegalArgumentException("duration must be greater than 0")
            val startPosition = mapState!!.rawPosition
            val endPosition = when (center) {
                is CenterLocation.Coordinates -> {
                    with(mapState!!) {
                        center.projectedCoordinates.toCanvasPosition()
                    }
                }

                is CenterLocation.Offset -> {
                    with(mapState!!) {
                        center.offset.fromDifferentialScreenOffsetToCanvasPosition()
                    }
                }

                is CenterLocation.Position -> center.position
            } + mapState!!.rawPosition
            decayValue(decayRate, duration) {
                mapState!!.rawPosition = lerp(startPosition, endPosition, it)
                mapState!!.updateState()
            }
        }

        override suspend fun zoomTo(zoom: Float, decayRate: Double, duration: MilliSeconds) {
            if (decayRate <= 0.0)
                throw IllegalArgumentException("decay rate must be greater than 0")
            if (duration <= 0)
                throw IllegalArgumentException("duration must be greater than 0")
            val startZoom = mapState!!.zoom
            decayValue(decayRate, duration) {
                mapState!!.zoom = lerp(startZoom, zoom, it.toFloat())
                mapState!!.updateState()
            }
        }

        override suspend fun zoomBy(zoom: Float, decayRate: Double, duration: MilliSeconds) {
            if (decayRate <= 0.0)
                throw IllegalArgumentException("decay rate must be greater than 0")
            if (duration <= 0)
                throw IllegalArgumentException("duration must be greater than 0")
            val startZoom = mapState!!.zoom
            val endZoom = startZoom + zoom
            decayValue(decayRate, duration) {
                mapState!!.zoom = lerp(startZoom, endZoom, it.toFloat())
                mapState!!.updateState()
            }
        }

        override suspend fun zoomToCentered(zoom: Float, center: CenterLocation, decayRate: Double, duration: MilliSeconds) {
            if (decayRate <= 0.0)
                throw IllegalArgumentException("decay rate must be greater than 0")
            if (duration <= 0)
                throw IllegalArgumentException("duration must be greater than 0")
            val startZoom = mapState!!.zoom
            val centerPosition = when (center) {
                is CenterLocation.Coordinates -> {
                    with(mapState!!) {
                        center.projectedCoordinates.toCanvasPosition()
                    }
                }

                is CenterLocation.Offset -> {
                    with(mapState!!) {
                        center.offset.fromScreenOffsetToCanvasPosition()
                    }
                }

                is CenterLocation.Position -> center.position
            }
            val centerOffset = when (center) {
                is CenterLocation.Coordinates -> {
                    with(mapState!!) {
                        center.projectedCoordinates.toCanvasPosition().toScreenOffset()
                    }
                }

                is CenterLocation.Offset -> center.offset

                is CenterLocation.Position -> {
                    with(mapState!!) {
                        center.position.toScreenOffset()
                    }
                }
            }
            decayValue(decayRate, duration) {
                mapState!!.zoom = lerp(startZoom, zoom, it.toFloat())
                with(mapState!!) {
                    centerPositionAtOffset(centerPosition, centerOffset)
                }
                mapState!!.updateState()
            }
        }

        override suspend fun zoomByCentered(zoom: Float, center: CenterLocation, decayRate: Double, duration: MilliSeconds) {
            if (decayRate <= 0.0)
                throw IllegalArgumentException("decay rate must be greater than 0")
            if (duration <= 0)
                throw IllegalArgumentException("duration must be greater than 0")
            val startZoom = mapState!!.zoom
            val endZoom = mapState!!.zoom + zoom
            val centerPosition = when (center) {
                is CenterLocation.Coordinates -> {
                    with(mapState!!) {
                        center.projectedCoordinates.toCanvasPosition()
                    }
                }

                is CenterLocation.Offset -> {
                    with(mapState!!) {
                        center.offset.fromScreenOffsetToCanvasPosition()
                    }
                }

                is CenterLocation.Position -> center.position
            }
            val centerOffset = when (center) {
                is CenterLocation.Coordinates -> {
                    with(mapState!!) {
                        center.projectedCoordinates.toCanvasPosition().toScreenOffset()
                    }
                }

                is CenterLocation.Offset -> center.offset

                is CenterLocation.Position -> {
                    with(mapState!!) {
                        center.position.toScreenOffset()
                    }
                }
            }
            decayValue(decayRate, duration) {
                mapState!!.zoom = lerp(startZoom, endZoom, it.toFloat())
                with(mapState!!) {
                    centerPositionAtOffset(centerPosition, centerOffset)
                }
                mapState!!.updateState()
            }
        }

        override suspend fun angleTo(degrees: Double, decayRate: Double, duration: MilliSeconds) {
            if (decayRate <= 0.0)
                throw IllegalArgumentException("decay rate must be greater than 0")
            if (duration <= 0)
                throw IllegalArgumentException("duration must be greater than 0")
            val startAngle = mapState!!.angleDegrees
            decayValue(decayRate, duration) {
                mapState!!.angleDegrees = lerp(startAngle, degrees, it)
                mapState!!.updateState()
            }
        }

        override suspend fun angleBy(degrees: Double, decayRate: Double, duration: MilliSeconds) {
            if (decayRate <= 0.0)
                throw IllegalArgumentException("decay rate must be greater than 0")
            if (duration <= 0)
                throw IllegalArgumentException("duration must be greater than 0")
            val startAngle = mapState!!.angleDegrees
            val endAngle = mapState!!.angleDegrees + degrees
            decayValue(decayRate, duration) {
                mapState!!.angleDegrees = lerp(startAngle, endAngle, it)
                mapState!!.updateState()
            }
        }

        override suspend fun rotateToCentered(degrees: Double, center: CenterLocation, decayRate: Double, duration: MilliSeconds) {
            if (decayRate <= 0.0)
                throw IllegalArgumentException("decay rate must be greater than 0")
            if (duration <= 0)
                throw IllegalArgumentException("duration must be greater than 0")
            val startAngle = mapState!!.angleDegrees
            val centerPosition = when (center) {
                is CenterLocation.Coordinates -> {
                    with(mapState!!) {
                        center.projectedCoordinates.toCanvasPosition()
                    }
                }

                is CenterLocation.Offset -> {
                    with(mapState!!) {
                        center.offset.fromScreenOffsetToCanvasPosition()
                    }
                }

                is CenterLocation.Position -> center.position
            }
            val centerOffset = when (center) {
                is CenterLocation.Coordinates -> {
                    with(mapState!!) {
                        center.projectedCoordinates.toCanvasPosition().toScreenOffset()
                    }
                }

                is CenterLocation.Offset -> center.offset

                is CenterLocation.Position -> {
                    with(mapState!!) {
                        center.position.toScreenOffset()
                    }
                }
            }
            decayValue(decayRate, duration) {
                mapState!!.angleDegrees = lerp(startAngle, degrees, it)
                with(mapState!!) {
                    centerPositionAtOffset(centerPosition, centerOffset)
                }
                mapState!!.updateState()
            }
        }

        override suspend fun rotateByCentered(degrees: Double, center: CenterLocation, decayRate: Double, duration: MilliSeconds) {
            if (decayRate <= 0.0)
                throw IllegalArgumentException("decay rate must be greater than 0")
            if (duration <= 0)
                throw IllegalArgumentException("duration must be greater than 0")
            val startAngle = mapState!!.angleDegrees
            val endAngle = mapState!!.angleDegrees + degrees
            val centerPosition = when (center) {
                is CenterLocation.Coordinates -> {
                    with(mapState!!) {
                        center.projectedCoordinates.toCanvasPosition()
                    }
                }

                is CenterLocation.Offset -> {
                    with(mapState!!) {
                        center.offset.fromScreenOffsetToCanvasPosition()
                    }
                }

                is CenterLocation.Position -> center.position
            }
            val centerOffset = when (center) {
                is CenterLocation.Coordinates -> {
                    with(mapState!!) {
                        center.projectedCoordinates.toCanvasPosition().toScreenOffset()
                    }
                }

                is CenterLocation.Offset -> center.offset

                is CenterLocation.Position -> {
                    with(mapState!!) {
                        center.position.toScreenOffset()
                    }
                }
            }
            decayValue(decayRate, duration) {
                mapState!!.angleDegrees = lerp(startAngle, endAngle, it)
                with(mapState!!) {
                    centerPositionAtOffset(centerPosition, centerOffset)
                }
                mapState!!.updateState()
            }
        }

    }

    private suspend fun decayValue(decayRate: Double, duration: MilliSeconds, function: (value: Double) -> Unit) {
        val durationConst = duration / 1000F
        val steps = (100 * durationConst).toInt()
        val timeStep = (10 * durationConst).toLong()
        for (i in 0 until steps) {
            function((1 - exp(-decayRate * (i.toDouble() / steps))) / (1 - exp(-decayRate)))
            delay(timeStep)
        }
    }
}