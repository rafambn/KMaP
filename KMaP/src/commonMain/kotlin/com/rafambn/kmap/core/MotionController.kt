package com.rafambn.kmap.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.util.lerp
import com.rafambn.kmap.core.state.MapState
import com.rafambn.kmap.utils.CanvasPosition
import com.rafambn.kmap.utils.DifferentialScreenOffset
import com.rafambn.kmap.utils.ProjectedCoordinates
import com.rafambn.kmap.utils.lerp
import com.rafambn.kmap.utils.Reference
import com.rafambn.kmap.utils.ScreenOffset
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

    interface MoveInterface {
        fun positionTo(center: Reference)
        fun positionBy(center: Reference)
        fun zoomTo(zoom: Float)
        fun zoomBy(zoom: Float)
        fun zoomToCentered(zoom: Float, center: Reference)
        fun zoomByCentered(zoom: Float, center: Reference)
        fun rotateTo(degrees: Double)
        fun rotateBy(degrees: Double)
        fun rotateToCentered(degrees: Double, center: Reference)
        fun rotateByCentered(degrees: Double, center: Reference)
    }

    interface AnimationInterface {
        suspend fun positionTo(center: Reference, decayRate: Double = 5.0, duration: MilliSeconds = 1000)
        suspend fun positionBy(center: Reference, decayRate: Double = 5.0, duration: MilliSeconds = 1000)
        suspend fun zoomTo(zoom: Float, decayRate: Double = 5.0, duration: MilliSeconds = 1000)
        suspend fun zoomBy(zoom: Float, decayRate: Double = 5.0, duration: MilliSeconds = 1000)
        suspend fun zoomToCentered(zoom: Float, center: Reference, decayRate: Double = 5.0, duration: MilliSeconds = 1000)
        suspend fun zoomByCentered(zoom: Float, center: Reference, decayRate: Double = 5.0, duration: MilliSeconds = 1000)
        suspend fun rotateTo(degrees: Double, decayRate: Double = 5.0, duration: MilliSeconds = 1000)
        suspend fun rotateBy(degrees: Double, decayRate: Double = 5.0, duration: MilliSeconds = 1000)
        suspend fun rotateToCentered(degrees: Double, center: Reference, decayRate: Double = 5.0, duration: MilliSeconds = 1000)
        suspend fun rotateByCentered(degrees: Double, center: Reference, decayRate: Double = 5.0, duration: MilliSeconds = 1000)
    }

    internal fun setMap(mapState: MapState) {
        this.mapState = mapState
        onMapChanged?.let {
            onMapChanged = null
            it(mapState)
        }
        cancellableContinuation?.resume(Unit)
    }

    fun move(block: MoveInterface.() -> Unit) {
        animationJob?.let {
            it.cancel(CancellationException("Animation cancelled by set"))
            onMapChanged = null
        }
        val map = mapState
        if (map == null) {
            onMapChanged = {
                block(moveObject)
                onMapChanged?.let { callback -> callback(it) }
            }
        } else {
            block(moveObject)
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

    private val moveObject = object : MoveInterface {

        override fun positionTo(center: Reference) {
            when (center) {
                is ScreenOffset -> with(mapState!!) {
                    mapState!!.setRawPosition(center.toCanvasPosition())
                }

                is CanvasPosition -> mapState!!.setRawPosition(center)
                is ProjectedCoordinates -> mapState!!.projection = center
            }
        }

        override fun positionBy(center: Reference) {
            when (center) {
                is DifferentialScreenOffset -> with(mapState!!) {
                    mapState!!.setRawPosition(center.toCanvasPosition() + mapState!!.cameraState.rawPosition)
                }

                is CanvasPosition -> mapState!!.setRawPosition(mapState!!.cameraState.rawPosition + center)
                is ProjectedCoordinates -> mapState!!.projection += center
            }
        }

        override fun zoomTo(zoom: Float) {
            mapState!!.setZoom(zoom)
        }

        override fun zoomBy(zoom: Float) {
            mapState!!.setZoom(mapState!!.cameraState.zoom + zoom)
        }

        override fun zoomToCentered(zoom: Float, center: Reference) {
            when (center) {
                is ProjectedCoordinates -> {
                    with(mapState!!) {
                        val previousOffset = center.toCanvasPosition().toScreenOffset()
                        val previousPosition = center.toCanvasPosition()
                        mapState!!.setZoom(zoom)
                        centerPositionAtOffset(previousPosition, previousOffset)
                    }
                }

                is ScreenOffset -> {
                    with(mapState!!) {
                        val position = center.toCanvasPosition()
                        mapState!!.setZoom(zoom)
                        centerPositionAtOffset(position, center)
                    }
                }

                is CanvasPosition -> {
                    with(mapState!!) {
                        val previousOffset = center.toScreenOffset()
                        mapState!!.setZoom(zoom)
                        centerPositionAtOffset(center, previousOffset)
                    }
                }
            }
        }

        override fun zoomByCentered(zoom: Float, center: Reference) {
            when (center) {
                is ProjectedCoordinates -> {
                    with(mapState!!) {
                        val previousOffset = center.toCanvasPosition().toScreenOffset()
                        val previousPosition = center.toCanvasPosition()
                        mapState!!.setZoom(mapState!!.cameraState.zoom + zoom)
                        centerPositionAtOffset(previousPosition, previousOffset)
                    }
                }

                is ScreenOffset -> {
                    with(mapState!!) {
                        val position = center.toCanvasPosition()
                        mapState!!.setZoom(mapState!!.cameraState.zoom + zoom)
                        centerPositionAtOffset(position, center)
                    }
                }

                is CanvasPosition -> {
                    with(mapState!!) {
                        val previousOffset = center.toScreenOffset()
                        mapState!!.setZoom(mapState!!.cameraState.zoom + zoom)
                        centerPositionAtOffset(center, previousOffset)
                    }
                }
            }
        }

        override fun rotateTo(degrees: Double) {
            mapState!!.setAngle(degrees)
        }

        override fun rotateBy(degrees: Double) {
            mapState!!.setAngle(degrees + mapState!!.cameraState.angleDegrees)
        }

        override fun rotateToCentered(degrees: Double, center: Reference) {
            when (center) {
                is ProjectedCoordinates -> {
                    with(mapState!!) {
                        val previousOffset = center.toCanvasPosition().toScreenOffset()
                        val previousPosition = center.toCanvasPosition()
                        mapState!!.setAngle(degrees)
                        centerPositionAtOffset(previousPosition, previousOffset)
                    }
                }

                is ScreenOffset -> {
                    with(mapState!!) {
                        val position = center.toCanvasPosition()
                        mapState!!.setAngle(degrees)
                        centerPositionAtOffset(position, center)
                    }
                }

                is CanvasPosition -> {
                    with(mapState!!) {
                        val previousOffset = center.toScreenOffset()
                        mapState!!.setAngle(degrees)
                        centerPositionAtOffset(center, previousOffset)
                    }
                }
            }
        }

        override fun rotateByCentered(degrees: Double, center: Reference) {
            when (center) {
                is ProjectedCoordinates -> {
                    with(mapState!!) {
                        val previousOffset = center.toCanvasPosition().toScreenOffset()
                        val previousPosition = center.toCanvasPosition()
                        mapState!!.setAngle(degrees + mapState!!.cameraState.angleDegrees)
                        centerPositionAtOffset(previousPosition, previousOffset)
                    }
                }

                is ScreenOffset -> {
                    with(mapState!!) {
                        val position = center.toCanvasPosition()
                        mapState!!.setAngle(degrees + mapState!!.cameraState.angleDegrees)
                        centerPositionAtOffset(position, center)
                    }
                }

                is CanvasPosition -> {
                    with(mapState!!) {
                        val previousOffset = center.toScreenOffset()
                        mapState!!.setAngle(degrees + mapState!!.cameraState.angleDegrees)
                        centerPositionAtOffset(center, previousOffset)
                    }
                }
            }
        }
    }

    private val animationObject = object : AnimationInterface {
        override suspend fun positionTo(center: Reference, decayRate: Double, duration: MilliSeconds) {
            if (decayRate <= 0.0)
                throw IllegalArgumentException("decay rate must be greater than 0")
            if (duration <= 0)
                throw IllegalArgumentException("duration must be greater than 0")
            val startPosition = mapState!!.cameraState.rawPosition
            val endPosition = when (center) {
                is ProjectedCoordinates -> {
                    with(mapState!!) {
                        center.toCanvasPosition()
                    }
                }

                is ScreenOffset -> {
                    with(mapState!!) {
                        center.toCanvasPosition()
                    }
                }

                is CanvasPosition -> center

                else -> throw IllegalArgumentException("Unknown reference type")
            }
            decayValue(decayRate, duration) {
                mapState!!.setRawPosition(lerp(startPosition, endPosition, it))
            }
        }

        override suspend fun positionBy(center: Reference, decayRate: Double, duration: MilliSeconds) {
            if (decayRate <= 0.0)
                throw IllegalArgumentException("decay rate must be greater than 0")
            if (duration <= 0)
                throw IllegalArgumentException("duration must be greater than 0")
            val startPosition = mapState!!.cameraState.rawPosition
            val endPosition = when (center) {
                is ProjectedCoordinates -> {
                    with(mapState!!) {
                        center.toCanvasPosition()
                    }
                }

                is DifferentialScreenOffset -> {
                    with(mapState!!) {
                        center.toCanvasPosition()
                    }
                }

                is CanvasPosition -> center
                else -> throw IllegalArgumentException("Unknown reference type")
            } + mapState!!.cameraState.rawPosition
            decayValue(decayRate, duration) {
                mapState!!.setRawPosition(lerp(startPosition, endPosition, it))
            }
        }

        override suspend fun zoomTo(zoom: Float, decayRate: Double, duration: MilliSeconds) {
            if (decayRate <= 0.0)
                throw IllegalArgumentException("decay rate must be greater than 0")
            if (duration <= 0)
                throw IllegalArgumentException("duration must be greater than 0")
            val startZoom = mapState!!.cameraState.zoom
            decayValue(decayRate, duration) {
                mapState!!.setZoom(lerp(startZoom, zoom, it.toFloat()))
            }
        }

        override suspend fun zoomBy(zoom: Float, decayRate: Double, duration: MilliSeconds) {
            if (decayRate <= 0.0)
                throw IllegalArgumentException("decay rate must be greater than 0")
            if (duration <= 0)
                throw IllegalArgumentException("duration must be greater than 0")
            val startZoom = mapState!!.cameraState.zoom
            val endZoom = startZoom + zoom
            decayValue(decayRate, duration) {
                mapState!!.setZoom(lerp(startZoom, endZoom, it.toFloat()))
            }
        }

        override suspend fun zoomToCentered(zoom: Float, center: Reference, decayRate: Double, duration: MilliSeconds) {
            if (decayRate <= 0.0)
                throw IllegalArgumentException("decay rate must be greater than 0")
            if (duration <= 0)
                throw IllegalArgumentException("duration must be greater than 0")
            val startZoom = mapState!!.cameraState.zoom
            val centerPosition = when (center) {
                is ProjectedCoordinates -> {
                    with(mapState!!) {
                        center.toCanvasPosition()
                    }
                }

                is ScreenOffset -> {
                    with(mapState!!) {
                        center.toCanvasPosition()
                    }
                }

                is CanvasPosition -> center
                else -> throw IllegalArgumentException("Unknown reference type")
            }
            val centerOffset = when (center) {
                is ProjectedCoordinates -> {
                    with(mapState!!) {
                        center.toCanvasPosition().toScreenOffset()
                    }
                }

                is ScreenOffset -> center

                is CanvasPosition -> {
                    with(mapState!!) {
                        center.toScreenOffset()
                    }
                }

                else -> throw IllegalArgumentException("Unknown reference type")
            }
            decayValue(decayRate, duration) {
                mapState!!.setZoom(lerp(startZoom, zoom, it.toFloat()))
                with(mapState!!) {
                    centerPositionAtOffset(centerPosition, centerOffset)
                }
            }
        }

        override suspend fun zoomByCentered(zoom: Float, center: Reference, decayRate: Double, duration: MilliSeconds) {
            if (decayRate <= 0.0)
                throw IllegalArgumentException("decay rate must be greater than 0")
            if (duration <= 0)
                throw IllegalArgumentException("duration must be greater than 0")
            val startZoom = mapState!!.cameraState.zoom
            val endZoom = mapState!!.cameraState.zoom + zoom
            val centerPosition = when (center) {
                is ProjectedCoordinates -> {
                    with(mapState!!) {
                        center.toCanvasPosition()
                    }
                }

                is ScreenOffset -> {
                    with(mapState!!) {
                        center.toCanvasPosition()
                    }
                }

                is CanvasPosition -> center
                else -> throw IllegalArgumentException("Unknown reference type")
            }
            val centerOffset = when (center) {
                is ProjectedCoordinates -> {
                    with(mapState!!) {
                        center.toCanvasPosition().toScreenOffset()
                    }
                }

                is ScreenOffset -> center

                is CanvasPosition -> {
                    with(mapState!!) {
                        center.toScreenOffset()
                    }
                }

                else -> throw IllegalArgumentException("Unknown reference type")
            }
            decayValue(decayRate, duration) {
                mapState!!.setZoom(lerp(startZoom, endZoom, it.toFloat()))
                with(mapState!!) {
                    centerPositionAtOffset(centerPosition, centerOffset)
                }
            }
        }

        override suspend fun rotateTo(degrees: Double, decayRate: Double, duration: MilliSeconds) {
            if (decayRate <= 0.0)
                throw IllegalArgumentException("decay rate must be greater than 0")
            if (duration <= 0)
                throw IllegalArgumentException("duration must be greater than 0")
            val startAngle = mapState!!.cameraState.angleDegrees
            decayValue(decayRate, duration) {
                mapState!!.setAngle(lerp(startAngle, degrees, it))
            }
        }

        override suspend fun rotateBy(degrees: Double, decayRate: Double, duration: MilliSeconds) {
            if (decayRate <= 0.0)
                throw IllegalArgumentException("decay rate must be greater than 0")
            if (duration <= 0)
                throw IllegalArgumentException("duration must be greater than 0")
            val startAngle = mapState!!.cameraState.angleDegrees
            val endAngle = mapState!!.cameraState.angleDegrees + degrees
            decayValue(decayRate, duration) {
                mapState!!.setAngle(lerp(startAngle, endAngle, it))
            }
        }

        override suspend fun rotateToCentered(degrees: Double, center: Reference, decayRate: Double, duration: MilliSeconds) {
            if (decayRate <= 0.0)
                throw IllegalArgumentException("decay rate must be greater than 0")
            if (duration <= 0)
                throw IllegalArgumentException("duration must be greater than 0")
            val startAngle = mapState!!.cameraState.angleDegrees
            val centerPosition = when (center) {
                is ProjectedCoordinates -> {
                    with(mapState!!) {
                        center.toCanvasPosition()
                    }
                }

                is ScreenOffset -> {
                    with(mapState!!) {
                        center.toCanvasPosition()
                    }
                }

                is CanvasPosition -> center
                else -> throw IllegalArgumentException("Unknown reference type")
            }
            val centerOffset = when (center) {
                is ProjectedCoordinates -> {
                    with(mapState!!) {
                        center.toCanvasPosition().toScreenOffset()
                    }
                }

                is ScreenOffset -> center

                is CanvasPosition -> {
                    with(mapState!!) {
                        center.toScreenOffset()
                    }
                }

                else -> throw IllegalArgumentException("Unknown reference type")
            }
            decayValue(decayRate, duration) {
                mapState!!.setAngle(lerp(startAngle, degrees, it))
                with(mapState!!) {
                    centerPositionAtOffset(centerPosition, centerOffset)
                }
            }
        }

        override suspend fun rotateByCentered(degrees: Double, center: Reference, decayRate: Double, duration: MilliSeconds) {
            if (decayRate <= 0.0)
                throw IllegalArgumentException("decay rate must be greater than 0")
            if (duration <= 0)
                throw IllegalArgumentException("duration must be greater than 0")
            val startAngle = mapState!!.cameraState.angleDegrees
            val endAngle = mapState!!.cameraState.angleDegrees + degrees
            val centerPosition = when (center) {
                is ProjectedCoordinates -> {
                    with(mapState!!) {
                        center.toCanvasPosition()
                    }
                }

                is ScreenOffset -> {
                    with(mapState!!) {
                        center.toCanvasPosition()
                    }
                }

                is CanvasPosition -> center
                else -> throw IllegalArgumentException("Unknown reference type")
            }
            val centerOffset = when (center) {
                is ProjectedCoordinates -> {
                    with(mapState!!) {
                        center.toCanvasPosition().toScreenOffset()
                    }
                }

                is ScreenOffset -> center

                is CanvasPosition -> {
                    with(mapState!!) {
                        center.toScreenOffset()
                    }
                }

                else -> throw IllegalArgumentException("Unknown reference type")
            }
            decayValue(decayRate, duration) {
                mapState!!.setAngle(lerp(startAngle, endAngle, it))
                with(mapState!!) {
                    centerPositionAtOffset(centerPosition, centerOffset)
                }
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