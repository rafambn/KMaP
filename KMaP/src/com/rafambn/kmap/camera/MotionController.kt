package com.rafambn.kmap.camera

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.ui.util.lerp
import com.rafambn.kmap.MapState
import com.rafambn.kmap.geometry.angle.Degrees
import com.rafambn.kmap.geometry.plane.*
import com.rafambn.kmap.utils.lerp
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
            mapState.updateCamera(tilePoint = lerp(startPosition, endPosition, value.toDouble()))
        }
    }

    override suspend fun positionBy(center: Reference, animationSpec: AnimationSpec<Float>) {
        val startPosition = mapState.cameraState.tilePoint
        val endPosition = getTilePoint(center) + mapState.cameraState.tilePoint
        animatable.snapTo(0F)
        animatable.animateTo(1f, animationSpec) {
            mapState.updateCamera(tilePoint = lerp(startPosition, endPosition, value.toDouble()))
        }
    }

    override suspend fun zoomTo(zoom: Float, animationSpec: AnimationSpec<Float>) {
        val startZoom = mapState.cameraState.zoom
        animatable.snapTo(0F)
        animatable.animateTo(1f, animationSpec) {
            mapState.updateCamera(zoom = lerp(startZoom, zoom, value))
        }
    }

    override suspend fun zoomBy(zoom: Float, animationSpec: AnimationSpec<Float>) {
        val startZoom = mapState.cameraState.zoom
        val endZoom = startZoom + zoom
        animatable.snapTo(0F)
        animatable.animateTo(1f, animationSpec) {
            mapState.updateCamera(zoom = lerp(startZoom, endZoom, value))
        }
    }

    override suspend fun zoomToCentered(
        zoom: Float,
        center: Reference,
        animationSpec: AnimationSpec<Float>
    ) {
        val startZoom = mapState.cameraState.zoom
        val previousOffset = (getScreenOffset(center) - mapState.viewportSize.asScreenOffset() / 2.0)
            .asDifferentialScreenOffset()
        val previousPosition = getTilePoint(center)
        animatable.snapTo(0F)
        animatable.animateTo(1f, animationSpec) {
            mapState.updateCamera(
                zoom = lerp(startZoom, zoom, value),
                tilePoint = previousPosition,
                centerOffset = previousOffset,
            )
        }
    }

    override suspend fun zoomByCentered(
        zoom: Float,
        center: Reference,
        animationSpec: AnimationSpec<Float>
    ) {
        val startZoom = mapState.cameraState.zoom
        val endZoom = mapState.cameraState.zoom + zoom
        val previousOffset = (getScreenOffset(center) - mapState.viewportSize.asScreenOffset() / 2.0)
            .asDifferentialScreenOffset()
        val previousPosition = getTilePoint(center)
        animatable.snapTo(0F)
        animatable.animateTo(1f, animationSpec) {
            mapState.updateCamera(
                zoom = lerp(startZoom, endZoom, value),
                tilePoint = previousPosition,
                centerOffset = previousOffset,
            )
        }
    }

    override suspend fun rotateTo(degrees: Degrees, animationSpec: AnimationSpec<Float>) {
        val startAngle = mapState.cameraState.angleDegrees
        animatable.snapTo(0F)
        animatable.animateTo(1f, animationSpec) {
            mapState.updateCamera(angle = Degrees(lerp(startAngle.value, degrees.value, value.toDouble())))
        }
    }

    override suspend fun rotateBy(degrees: Degrees, animationSpec: AnimationSpec<Float>) {
        val startAngle = mapState.cameraState.angleDegrees
        val endAngle = mapState.cameraState.angleDegrees + degrees
        animatable.snapTo(0f)
        animatable.animateTo(1f, animationSpec) {
            mapState.updateCamera(angle = Degrees(lerp(startAngle.value, endAngle.value, value.toDouble())))
        }
    }

    override suspend fun rotateToCentered(
        degrees: Degrees,
        center: Reference,
        animationSpec: AnimationSpec<Float>
    ) {
        val startAngle = mapState.cameraState.angleDegrees
        val previousOffset = (getScreenOffset(center) - mapState.viewportSize.asScreenOffset() / 2.0)
            .asDifferentialScreenOffset()
        val previousPosition = getTilePoint(center)
        animatable.snapTo(0F)
        animatable.animateTo(1f, animationSpec) {
            mapState.updateCamera(
                angle = Degrees(lerp(startAngle.value, degrees.value, value.toDouble())),
                tilePoint = previousPosition,
                centerOffset = previousOffset,
            )
        }
    }

    override suspend fun rotateByCentered(
        degrees: Degrees,
        center: Reference,
        animationSpec: AnimationSpec<Float>
    ) {
        val startAngle = mapState.cameraState.angleDegrees
        val endAngle = mapState.cameraState.angleDegrees + degrees
        val previousOffset = (getScreenOffset(center) - mapState.viewportSize.asScreenOffset() / 2.0)
            .asDifferentialScreenOffset()
        val previousPosition = getTilePoint(center)
        animatable.snapTo(0F)
        animatable.animateTo(1f, animationSpec) {
            mapState.updateCamera(
                angle = Degrees(lerp(startAngle.value, endAngle.value, value.toDouble())),
                tilePoint = previousPosition,
                centerOffset = previousOffset,
            )
        }
    }

    override fun positionTo(center: Reference) {
        mapState.updateCamera(tilePoint = getTilePoint(center))
    }

    override fun positionBy(center: Reference) {
        mapState.updateCamera(tilePoint = getTilePoint(center) + mapState.cameraState.tilePoint)
    }

    override fun zoomTo(zoom: Float) {
        mapState.updateCamera(zoom = zoom)
    }

    override fun zoomBy(zoom: Float) {
        mapState.updateCamera(zoom = mapState.cameraState.zoom + zoom)
    }

    override fun zoomToCentered(zoom: Float, center: Reference) {
        val previousOffset = (getScreenOffset(center) - mapState.viewportSize.asScreenOffset() / 2.0)
            .asDifferentialScreenOffset()
        val previousPosition = getTilePoint(center)
        mapState.updateCamera(
            zoom = zoom,
            tilePoint = previousPosition,
            centerOffset = previousOffset,
        )
    }

    override fun zoomByCentered(zoom: Float, center: Reference) {
        val previousOffset = (getScreenOffset(center) - mapState.viewportSize.asScreenOffset() / 2.0)
            .asDifferentialScreenOffset()
        val previousPosition = getTilePoint(center)
        mapState.updateCamera(
            zoom = mapState.cameraState.zoom + zoom,
            tilePoint = previousPosition,
            centerOffset = previousOffset,
        )
    }

    override fun rotateTo(degrees: Degrees) {
        mapState.updateCamera(angle = degrees)
    }

    override fun rotateBy(degrees: Degrees) {
        mapState.updateCamera(angle = degrees + mapState.cameraState.angleDegrees)
    }

    override fun rotateToCentered(degrees: Degrees, center: Reference) {
        val previousOffset = (getScreenOffset(center) - mapState.viewportSize.asScreenOffset() / 2.0)
            .asDifferentialScreenOffset()
        val previousPosition = getTilePoint(center)
        mapState.updateCamera(
            angle = degrees,
            tilePoint = previousPosition,
            centerOffset = previousOffset,
        )
    }

    override fun rotateByCentered(degrees: Degrees, center: Reference) {
        val previousOffset = (getScreenOffset(center) - mapState.viewportSize.asScreenOffset() / 2.0)
            .asDifferentialScreenOffset()
        val previousPosition = getTilePoint(center)
        mapState.updateCamera(
            angle = degrees + mapState.cameraState.angleDegrees,
            tilePoint = previousPosition,
            centerOffset = previousOffset,
        )
    }

    fun getTilePoint(center: Reference): TilePoint {
        return context(mapState) {
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
        return context(mapState) {
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
