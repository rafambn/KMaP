package com.rafambn.kmap.core

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.ui.util.lerp
import com.rafambn.kmap.utils.Coordinates
import com.rafambn.kmap.utils.DifferentialScreenOffset
import com.rafambn.kmap.utils.Reference
import com.rafambn.kmap.utils.ScreenOffset
import com.rafambn.kmap.utils.TilePoint
import com.rafambn.kmap.utils.lerp
import com.rafambn.kmap.utils.plus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext

class MotionController(private val mapState: MapState) : AnimateInterface, MoveInterface {
    private var animationJob: Job? = null
    private val animatable = Animatable(0f)

    fun move(block: MoveInterface.() -> Unit) {
        animationJob?.cancel(CancellationException("Animation cancelled by move"))
        block(this)
    }

    suspend fun animate(
        block: suspend AnimateInterface.() -> Unit
    ) {
        val myJob = currentCoroutineContext()[Job]
        animationJob = myJob
        animationJob?.invokeOnCompletion {
            it?.let {
                if (it !is CancellationException)
                    throw it
            }
            animationJob = null
        }
        try {
            block(this)
        } finally {
            animationJob = null
        }
    }

    override suspend fun positionTo(center: Reference, animationSpec: AnimationSpec<Float>) {
        val startPosition = mapState.cameraState.tilePoint
        val endPosition = getTilePoint(center)
        animatable.snapTo(0F)
        animatable.animateTo(1f, animationSpec) {
            mapState.setPosition(lerp(startPosition, endPosition, value.toDouble()))
        }
    }

    override suspend fun positionBy(center: Reference, animationSpec: AnimationSpec<Float>) {
        val startPosition = mapState.cameraState.tilePoint
        val endPosition = getTilePoint(center) + mapState.cameraState.tilePoint
        animatable.snapTo(0F)
        animatable.animateTo(1f, animationSpec) {
            mapState.setPosition(lerp(startPosition, endPosition, value.toDouble()))
        }
    }

    override suspend fun zoomTo(zoom: Float, animationSpec: AnimationSpec<Float>) {
        val startZoom = mapState.cameraState.zoom
        animatable.snapTo(0F)
        animatable.animateTo(1f, animationSpec) {
            mapState.setZoom(lerp(startZoom, zoom, value))
        }
    }

    override suspend fun zoomBy(zoom: Float, animationSpec: AnimationSpec<Float>) {
        val startZoom = mapState.cameraState.zoom
        val endZoom = startZoom + zoom
        animatable.snapTo(0F)
        animatable.animateTo(1f, animationSpec) {
            mapState.setZoom(lerp(startZoom, endZoom, value))
        }
    }

    override suspend fun zoomToCentered(
        zoom: Float,
        center: Reference,
        animationSpec: AnimationSpec<Float>
    ) {
        val startZoom = mapState.cameraState.zoom
        val previousOffset = getScreenOffset(center)
        val previousPosition = getTilePoint(center)
        animatable.snapTo(0F)
        animatable.animateTo(1f, animationSpec) {
            mapState.setZoom(lerp(startZoom, zoom, value))
            mapState.centerPointAtOffset(previousPosition, previousOffset)
        }
    }

    override suspend fun zoomByCentered(
        zoom: Float,
        center: Reference,
        animationSpec: AnimationSpec<Float>
    ) {
        val startZoom = mapState.cameraState.zoom
        val endZoom = mapState.cameraState.zoom + zoom
        val previousOffset = getScreenOffset(center)
        val previousPosition = getTilePoint(center)
        animatable.snapTo(0F)
        animatable.animateTo(1f, animationSpec) {
            mapState.setZoom(lerp(startZoom, endZoom, value))
            mapState.centerPointAtOffset(previousPosition, previousOffset)
        }
    }

    override suspend fun rotateTo(degrees: Double, animationSpec: AnimationSpec<Float>) {
        val startZoom = mapState.cameraState.angleDegrees
        animatable.snapTo(0F)
        animatable.animateTo(1f, animationSpec) {
            mapState.setAngle(lerp(startZoom, degrees, value.toDouble()))
        }
    }

    override suspend fun rotateBy(degrees: Double, animationSpec: AnimationSpec<Float>) {
        val startAngle = mapState.cameraState.angleDegrees
        val endAngle = mapState.cameraState.angleDegrees + degrees
        animatable.snapTo(0f)
        animatable.animateTo(1f, animationSpec) {
            mapState.setAngle(lerp(startAngle, endAngle, value.toDouble()))
        }
    }

    override suspend fun rotateToCentered(
        degrees: Double,
        center: Reference,
        animationSpec: AnimationSpec<Float>
    ) {
        val startAngle = mapState.cameraState.angleDegrees
        val previousOffset = getScreenOffset(center)
        val previousPosition = getTilePoint(center)
        animatable.snapTo(0F)
        animatable.animateTo(1f, animationSpec) {
            mapState.setAngle(lerp(startAngle, degrees, value.toDouble()))
            mapState.centerPointAtOffset(previousPosition, previousOffset)
        }
    }

    override suspend fun rotateByCentered(
        degrees: Double,
        center: Reference,
        animationSpec: AnimationSpec<Float>
    ) {
        val startAngle = mapState.cameraState.angleDegrees
        val endAngle = mapState.cameraState.angleDegrees + degrees
        val previousOffset = getScreenOffset(center)
        val previousPosition = getTilePoint(center)
        animatable.snapTo(0F)
        animatable.animateTo(1f, animationSpec) {
            mapState.setAngle(lerp(startAngle, endAngle, value.toDouble()))
            mapState.centerPointAtOffset(previousPosition, previousOffset)
        }
    }

    override fun positionTo(center: Reference) {
        mapState.setPosition(getTilePoint(center))
    }

    override fun positionBy(center: Reference) {
        mapState.setPosition(getTilePoint(center) + mapState.cameraState.tilePoint)
    }

    override fun zoomTo(zoom: Float) {
        mapState.setZoom(zoom)
    }

    override fun zoomBy(zoom: Float) {
        mapState.setZoom(mapState.cameraState.zoom + zoom)
    }

    override fun zoomToCentered(zoom: Float, center: Reference) {
        val previousOffset = getScreenOffset(center)
        val previousPosition = getTilePoint(center)
        mapState.setZoom(zoom)
        mapState.centerPointAtOffset(previousPosition, previousOffset)
    }

    override fun zoomByCentered(zoom: Float, center: Reference) {
        val previousOffset = getScreenOffset(center)
        val previousPosition = getTilePoint(center)
        mapState.setZoom(mapState.cameraState.zoom + zoom)
        mapState.centerPointAtOffset(previousPosition, previousOffset)
    }

    override fun rotateTo(degrees: Double) {
        mapState.setAngle(degrees)
    }

    override fun rotateBy(degrees: Double) {
        mapState.setAngle(degrees + mapState.cameraState.angleDegrees)
    }

    override fun rotateToCentered(degrees: Double, center: Reference) {
        val previousOffset = getScreenOffset(center)
        val previousPosition = getTilePoint(center)
        mapState.setAngle(degrees)
        mapState.centerPointAtOffset(previousPosition, previousOffset)
    }

    override fun rotateByCentered(degrees: Double, center: Reference) {
        val previousOffset = getScreenOffset(center)
        val previousPosition = getTilePoint(center)
        mapState.setAngle(degrees + mapState.cameraState.angleDegrees)
        mapState.centerPointAtOffset(previousPosition, previousOffset)
    }

    fun getTilePoint(center: Reference): TilePoint {
        return with(mapState) {
            when (center) {
                is ScreenOffset -> center.toTilePoint()
                is TilePoint -> center
                is Coordinates -> center.toTilePoint()
                is DifferentialScreenOffset -> center.toTilePoint()
                else -> throw IllegalArgumentException("Center must be a reference type")
            }
        }
    }

    fun getScreenOffset(center: Reference): ScreenOffset {
        return with(mapState) {
            when (center) {
                is ScreenOffset -> center
                is TilePoint -> center.toScreenOffset()
                is Coordinates -> center.toTilePoint().toScreenOffset()
                is DifferentialScreenOffset -> center.toTilePoint().toScreenOffset()
                else -> throw IllegalArgumentException("Center must be a reference type")
            }
        }
    }
}
