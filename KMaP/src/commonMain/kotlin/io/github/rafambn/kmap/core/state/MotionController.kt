package io.github.rafambn.kmap.core.state

import androidx.compose.ui.util.lerp
import io.github.rafambn.kmap.config.border.MapBorderType
import io.github.rafambn.kmap.config.characteristics.MapCoordinatesRange
import io.github.rafambn.kmap.core.MotionInterface
import io.github.rafambn.kmap.utils.Degrees
import io.github.rafambn.kmap.utils.lerp
import io.github.rafambn.kmap.utils.loopInRange
import io.github.rafambn.kmap.utils.offsets.CanvasDrawReference
import io.github.rafambn.kmap.utils.offsets.CanvasPosition
import io.github.rafambn.kmap.utils.offsets.DifferentialScreenOffset
import io.github.rafambn.kmap.utils.offsets.ScreenOffset
import io.github.rafambn.kmap.utils.offsets.toPosition
import io.github.rafambn.kmap.utils.rotate
import io.github.rafambn.kmap.utils.toRadians
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.exp

class MotionController(
    private val mapState: MapState,
    private val coroutineScope: CoroutineScope
) : MotionInterface {

    //Fling variable
    private var flingPositionJob: Job? = null
    private var flingZoomJob: Job? = null
    private var flingZoomAtPositionJob: Job? = null
    private var flingRotationJob: Job? = null
    private var flingRotationAtPositionJob: Job? = null

    //Interface functions
    override fun setCenter(position: CanvasPosition) {
        flingPositionJob?.cancel()
        flingZoomAtPositionJob?.cancel()
        flingRotationAtPositionJob?.cancel()
        mapState.mapPosition = position.coerceInMap()
        mapState.updateState()
    }

    override fun moveBy(position: CanvasPosition) {
        flingPositionJob?.cancel()
        flingZoomAtPositionJob?.cancel()
        flingRotationAtPositionJob?.cancel()
        mapState.mapPosition = (mapState.mapPosition + position).coerceInMap()
        mapState.updateState()
    }

    override fun animatePositionTo(position: CanvasPosition, decayRate: Double) {
        flingPositionJob?.cancel()
        flingZoomAtPositionJob?.cancel()
        flingRotationAtPositionJob?.cancel()
        val startPosition = mapState.mapPosition
        flingPositionJob = decayValue(coroutineScope, decayRate) {
            mapState.mapPosition = lerp(startPosition, position, it).coerceInMap()
            mapState.updateState()
        }
    }

    override fun setZoom(zoom: Float) {
        flingZoomJob?.cancel()
        flingZoomAtPositionJob?.cancel()
        mapState.zoom = zoom.coerceZoom()
        mapState.updateState()
    }

    override fun zoomBy(zoom: Float) {
        flingZoomJob?.cancel()
        flingZoomAtPositionJob?.cancel()
        mapState.zoom = (zoom + mapState.zoom).coerceZoom()
        mapState.updateState()
    }

    override fun zoomBy(zoom: Float, position: CanvasPosition) {
        flingZoomJob?.cancel()
        flingZoomAtPositionJob?.cancel()
        val previousOffset = position.toScreenOffset()
        mapState.zoom = (zoom + mapState.zoom).coerceZoom()
        centerPositionAtOffset(position, previousOffset)
        mapState.updateState()
    }

    override fun animateZoomTo(zoom: Float, decayRate: Double) {
        flingZoomJob?.cancel()
        flingZoomAtPositionJob?.cancel()
        val startZoom = mapState.zoom
        flingZoomJob = decayValue(coroutineScope, decayRate) { decayValue ->
            mapState.zoom = lerp(startZoom, zoom, decayValue.toFloat()).coerceZoom()
            mapState.updateState()
        }
    }

    override fun animateZoomTo(zoom: Float, decayRate: Double, position: CanvasPosition) {
        flingPositionJob?.cancel()
        flingZoomJob?.cancel()
        flingZoomAtPositionJob?.cancel()
        val startZoom = mapState.zoom
        val previousOffset =
            position.toScreenOffset()
        flingZoomAtPositionJob = decayValue(coroutineScope, decayRate) { decayValue ->
            mapState.zoom = lerp(startZoom, zoom, decayValue.toFloat()).coerceZoom()
            centerPositionAtOffset(position, previousOffset)
            mapState.updateState()
        }
    }

    override fun setRotation(angle: Degrees) {
        flingRotationJob?.cancel()
        flingRotationAtPositionJob?.cancel()
        mapState.angleDegrees = angle
        mapState.updateState()
    }

    override fun rotateBy(angle: Degrees) {
        flingRotationJob?.cancel()
        flingRotationAtPositionJob?.cancel()
        mapState.angleDegrees += angle
        mapState.updateState()
    }

    override fun rotateBy(angle: Degrees, position: CanvasPosition) {
        flingRotationJob?.cancel()
        flingRotationAtPositionJob?.cancel()
        val previousOffset = position.toScreenOffset()
        mapState.angleDegrees += angle.toFloat()
        centerPositionAtOffset(position, previousOffset)
        mapState.updateState()
    }

    override fun animateRotationTo(angle: Degrees, decayRate: Double) {
        flingRotationJob?.cancel()
        flingRotationAtPositionJob?.cancel()
        val startRotation = mapState.angleDegrees
        flingRotationJob = decayValue(coroutineScope, decayRate) { decayValue ->
            mapState.angleDegrees = lerp(startRotation, angle, decayValue)
            mapState.updateState()
        }
    }

    override fun animateRotationTo(angle: Degrees, decayRate: Double, position: CanvasPosition) {
        flingPositionJob?.cancel()
        flingRotationJob?.cancel()
        flingRotationAtPositionJob?.cancel()
        val startRotation = mapState.angleDegrees
        val previousOffset =
            position.toScreenOffset()
        flingRotationAtPositionJob = decayValue(coroutineScope, decayRate) { decayValue ->
            mapState.angleDegrees = lerp(startRotation, angle, decayValue)
            centerPositionAtOffset(position, previousOffset)
            mapState.updateState()
        }
    }


    private fun CanvasPosition.coerceInMap(): CanvasPosition {
        val x = if (mapState.mapProperties.boundMap.horizontal == MapBorderType.BOUND)
            horizontal.coerceIn(
                mapState.mapProperties.mapSource.mapCoordinatesRange.longitute.west,
                mapState.mapProperties.mapSource.mapCoordinatesRange.longitute.east
            )
        else
            horizontal.loopInRange(mapState.mapProperties.mapSource.mapCoordinatesRange.longitute)
        val y = if (mapState.mapProperties.boundMap.vertical == MapBorderType.BOUND)
            vertical.coerceIn(
                mapState.mapProperties.mapSource.mapCoordinatesRange.latitude.south,
                mapState.mapProperties.mapSource.mapCoordinatesRange.latitude.north
            )
        else
            vertical.loopInRange(mapState.mapProperties.mapSource.mapCoordinatesRange.latitude)
        return CanvasPosition(x, y)
    }

    private fun Float.coerceZoom(): Float {
        return this.coerceIn(mapState.minZoom.toFloat(), mapState.maxZoom.toFloat())
    }

    private fun decayValue(coroutineScope: CoroutineScope, decayRate: Double, function: (value: Double) -> Unit) = coroutineScope.launch {
        val steps = 100
        val timeStep = 10L
        for (i in 0 until steps) {
            val x = i.toDouble() / steps
            function((1 - exp(decayRate * x)) / (1 - exp(decayRate)))
            delay(timeStep)
        }
    }

    private fun centerPositionAtOffset(position: CanvasPosition, offset: ScreenOffset) {
        mapState.mapPosition = (mapState.mapPosition + position - offset.fromScreenOffsetToCanvasPosition()).coerceInMap()
    }

    //Conversion Functions
    fun ScreenOffset.fromScreenOffsetToCanvasPosition(): CanvasPosition = this.toCanvasPositionFromScreenCenter() + mapState.mapPosition

    fun DifferentialScreenOffset.toCanvasPositionFromScreenCenter(): CanvasPosition = (mapState.canvasSize / 2F - this).fromDifferentialScreenOffsetToCanvasPosition()

    fun DifferentialScreenOffset.fromDifferentialScreenOffsetToCanvasPosition(): CanvasPosition = (this.toPosition() / mapState.density.density.toDouble())
        .scaleToZoom(1 / (mapState.mapProperties.mapSource.tileSize * mapState.magnifierScale * (1 shl mapState.zoomLevel)))
        .rotate(-mapState.angleDegrees.toRadians())
        .scaleToMap(
            mapState.mapProperties.mapSource.mapCoordinatesRange.longitute.span,
            mapState.mapProperties.mapSource.mapCoordinatesRange.latitude.span
        )
        .applyOrientation(mapState.mapProperties.mapSource.mapCoordinatesRange)

    fun CanvasPosition.toCanvasDrawReference(): CanvasDrawReference {
        val canvasDrawReference = this.applyOrientation(mapState.mapProperties.mapSource.mapCoordinatesRange)
            .moveToTrueCoordinates(mapState.mapProperties.mapSource.mapCoordinatesRange)
            .scaleToZoom((mapState.mapProperties.mapSource.tileSize * (1 shl mapState.zoomLevel)).toFloat())
            .scaleToMap(
                1 / mapState.mapProperties.mapSource.mapCoordinatesRange.longitute.span,
                1 / mapState.mapProperties.mapSource.mapCoordinatesRange.latitude.span
            )
        return CanvasDrawReference(canvasDrawReference.horizontal, canvasDrawReference.vertical)
    }

    fun CanvasPosition.toScreenOffset(): ScreenOffset = -(this - mapState.mapPosition)
        .applyOrientation(mapState.mapProperties.mapSource.mapCoordinatesRange)
        .scaleToMap(
            1 / mapState.mapProperties.mapSource.mapCoordinatesRange.longitute.span,
            1 / mapState.mapProperties.mapSource.mapCoordinatesRange.latitude.span
        )
        .rotate(mapState.angleDegrees.toRadians())
        .scaleToZoom(mapState.mapProperties.mapSource.tileSize * mapState.magnifierScale * (1 shl mapState.zoomLevel))
        .times(mapState.density.density.toDouble()).toOffset()
        .minus(mapState.canvasSize / 2F)


    fun CanvasPosition.scaleToZoom(zoomScale: Float): CanvasPosition {
        return CanvasPosition(horizontal * zoomScale, vertical * zoomScale)
    }

    fun CanvasPosition.moveToTrueCoordinates(mapCoordinatesRange: MapCoordinatesRange): CanvasPosition {
        return CanvasPosition(horizontal - mapCoordinatesRange.longitute.span / 2, vertical - mapCoordinatesRange.latitude.span / 2)
    }

    fun CanvasPosition.scaleToMap(horizontal: Double, vertical: Double): CanvasPosition {
        return CanvasPosition(this.horizontal * horizontal, this.vertical * vertical)
    }

    fun CanvasPosition.applyOrientation(mapCoordinatesRange: MapCoordinatesRange): CanvasPosition {
        return CanvasPosition(horizontal * mapCoordinatesRange.longitute.orientation, vertical * mapCoordinatesRange.latitude.orientation)
    }
}