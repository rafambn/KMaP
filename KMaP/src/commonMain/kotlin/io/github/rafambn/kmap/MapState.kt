package io.github.rafambn.kmap

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.util.lerp
import io.github.rafambn.kmap.utils.Degrees
import io.github.rafambn.kmap.utils.applyOrientation
import io.github.rafambn.kmap.utils.lerp
import io.github.rafambn.kmap.utils.loopInRange
import io.github.rafambn.kmap.utils.rotate
import io.github.rafambn.kmap.utils.scaleToMap
import io.github.rafambn.kmap.utils.scaleToZoom
import io.github.rafambn.kmap.utils.toCanvasReference
import io.github.rafambn.kmap.utils.toMapReference
import io.github.rafambn.kmap.utils.toOffset
import io.github.rafambn.kmap.utils.toPosition
import io.github.rafambn.kmap.utils.toRadians
import io.github.rafambn.kmap.utils.toViewportReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.pow

class MapState(
    private val coroutineScope: CoroutineScope,
    initialPosition: Position = Position.Zero,
    initialZoom: Float = 0F,
    initialRotation: Float = 0F,
    maxZoom: Int = 19,
    minZoom: Int = 0,
    val mapProperties: MapProperties = MapProperties(zoomLevels = OSMZoomlevelsRange, mapCoordinatesRange = OSMCoordinatesRange),
    private val density: Density
) : MotionInterface, CanvasSizeChangeListener {

    val tileCanvasState = TileCanvasState(::updateState)

    //User define min/max zoom
    private var maxZoom = maxZoom.coerceIn(mapProperties.zoomLevels.min, mapProperties.zoomLevels.max)
    private var minZoom = minZoom.coerceIn(mapProperties.zoomLevels.min, mapProperties.zoomLevels.max)

    //Control variables
    var zoom = initialZoom
        private set
    var angleDegrees = initialRotation
        private set
    var mapPosition = initialPosition
        private set
    var canvasSize = Offset.Zero
        private set

    //Derivative variables
    val zoomLevel
        get() = floor(zoom).toInt()
    val magnifierScale
        get() = zoom - zoomLevel + 1F
    val boundingBox
        get() = canvasSize.toPosition().toViewportReference(magnifierScale, zoomLevel, angleDegrees.toDouble(), OSMCoordinatesRange, mapPosition)
    internal val positionOffset
        get() = mapPosition.toCanvasReference(zoomLevel, mapProperties.mapCoordinatesRange)

    //Map state variable for recomposition
    internal var state by mutableStateOf(false)

    private fun updateState() {
        state = !state
        tileCanvasState.onStateChange(
            ScreenState(
                boundingBox,
                zoomLevel,
                mapProperties.mapCoordinatesRange,
                mapProperties.outsideTiles
            )
        )
    }

    //Fling variable
    private var flingPositionJob: Job? = null
    private var flingZoomJob: Job? = null
    private var flingRotationJob: Job? = null //TODO coordinate job to reduce flickering

    //Interface functions
    override fun setCenter(position: Position) {
        flingPositionJob?.cancel()
        flingZoomJob?.cancel()
        flingRotationJob?.cancel()
        mapPosition = position.coerceInMap()
        updateState()
    }

    override fun moveBy(position: Position) {
        flingPositionJob?.cancel()
        flingZoomJob?.cancel()
        flingRotationJob?.cancel()
        mapPosition = (mapPosition + position).coerceInMap()
        updateState()
    }

    override fun animatePositionTo(position: Position, decayRate: Double) {
        val startPosition = mapPosition
        flingPositionJob?.cancel()
        flingPositionJob = decayValue(coroutineScope, decayRate) {
            mapPosition = lerp(position, startPosition, it).coerceInMap()
            updateState()
        }
    }

    override fun setZoom(zoom: Float) {
        flingZoomJob?.cancel()
        this.zoom = zoom.coerceZoom()
        updateState()
    }

    override fun zoomBy(zoom: Float, position: Position?) {
        flingZoomJob?.cancel()
        position?.let {
            val previousOffset = positionToCanvasOffset(it)
            this.zoom = (zoom + this.zoom).coerceZoom()
            centerPositionAtOffset(it, previousOffset)
        } ?: run {
            this.zoom = (zoom + this.zoom).coerceZoom()
            updateState()
        }
    }

    override fun animateZoomTo(zoom: Float, decayRate: Double, position: Position?) {
        flingZoomJob?.cancel()
        val startZoom = this.zoom
        val previousOffset = position?.let { positionToCanvasOffset(it) } ?: Offset.Zero
        flingZoomJob = decayValue(coroutineScope, decayRate) { decayValue ->
            this.zoom = lerp(zoom, startZoom, decayValue.toFloat()).coerceZoom() //TODO this is backwards
            position?.let {
                centerPositionAtOffset(it, previousOffset)
            } ?: run {
                updateState()
            }
        }
    }

    override fun setRotation(angle: Degrees) {
        flingRotationJob?.cancel()
        angleDegrees = angle.toFloat()
        updateState()
    }

    override fun rotateBy(angle: Degrees, position: Position?) {
        flingRotationJob?.cancel()
        val previousOffset = position?.let { positionToCanvasOffset(it) } ?: Offset.Zero
        angleDegrees += angle.toFloat()
        position?.let {
            centerPositionAtOffset(it, previousOffset)
        } ?: run {
            updateState()
        }
    }

    override fun animateRotationTo(angle: Degrees, decayRate: Double, position: Position?) {
        flingRotationJob?.cancel()
        val startRotation = angleDegrees
        val previousOffset = position?.let { positionToCanvasOffset(it) } ?: Offset.Zero
        flingRotationJob = decayValue(coroutineScope, decayRate) { decayValue ->
            angleDegrees = lerp(angle.toFloat(), startRotation, decayValue.toFloat())
            position?.let {
                centerPositionAtOffset(it, previousOffset)
            } ?: run {
                updateState()
            }
        }
    }

    override fun setPosZoomRotate(position: Position, zoom: Float, angle: Degrees) {
        setCenter(position)
        setZoom(zoom)
        setRotation(angle)
    }

    override fun animatePosZoomRotate(position: Position, zoom: Float, angle: Degrees, decayRate: Double) {
        animatePositionTo(position, decayRate)
        animateZoomTo(zoom, decayRate)
        animateRotationTo(angle, decayRate)
    }

    override fun onCanvasSizeChanged(size: Offset) {
        canvasSize = size
        updateState()
    }

    //Utility functions
    fun differentialOffsetToMapReference(offset: Offset): Position {
        return offset.toPosition().toMapReference(
            magnifierScale,
            zoomLevel,
            angleDegrees.toDouble(),
            mapProperties.mapCoordinatesRange,
            density
        )
    }

    fun differentialOffsetFromScreenCenterToMapReference(offset: Offset): Position {
        return (canvasSize / 2F - offset).toPosition().toMapReference(
            magnifierScale,
            zoomLevel,
            angleDegrees.toDouble(),
            mapProperties.mapCoordinatesRange,
            density
        )
    }

    fun offsetToMapReference(offset: Offset): Position {
        return differentialOffsetFromScreenCenterToMapReference(offset) + mapPosition
    }

    private fun positionToCanvasOffset(position: Position): Offset {
        return -(position - mapPosition)
            .applyOrientation(mapProperties.mapCoordinatesRange)
            .scaleToMap(1 / mapProperties.mapCoordinatesRange.longitute.span, 1 / mapProperties.mapCoordinatesRange.latitude.span)
            .rotate(angleDegrees.toDouble().toRadians())
            .scaleToZoom((TileCanvasState.TILE_SIZE * magnifierScale * (1 shl zoomLevel)).toDouble())
            .times(density.density.toDouble())
            .minus(Position(canvasSize.x / 2.0, canvasSize.y / 2.0))
            .toOffset()
    }

    private fun Position.coerceInMap(): Position {
        val x = if (mapProperties.boundMap.horizontal == MapBorderType.BOUND)
            horizontal.coerceIn(
                mapProperties.mapCoordinatesRange.longitute.west,
                mapProperties.mapCoordinatesRange.longitute.east
            )
        else
            horizontal.loopInRange(mapProperties.mapCoordinatesRange.longitute)
        val y = if (mapProperties.boundMap.vertical == MapBorderType.BOUND)
            vertical.coerceIn(
                mapProperties.mapCoordinatesRange.latitude.south,
                mapProperties.mapCoordinatesRange.latitude.north
            )
        else
            vertical.loopInRange(mapProperties.mapCoordinatesRange.latitude)
        return Position(x, y)
    }

    private fun Float.coerceZoom(): Float {
        return this.coerceIn(minZoom.toFloat(), maxZoom.toFloat())
    }

    private fun decayValue(coroutineScope: CoroutineScope, decayRate: Double, function: (value: Double) -> Unit) = coroutineScope.launch {
        val steps = 200
        val timeStep = 5L
        for (i in 0 until steps) {
            val x = i.toDouble() / steps
            function((1 - exp(decayRate * (x - 1.0))) / (1 - exp(-decayRate)))
            delay(timeStep)
        }
    }

    private fun centerPositionAtOffset(position: Position, offset: Offset) {
        mapPosition = (mapPosition + position - offsetToMapReference(offset)).coerceInMap()
        updateState()
    }
}

@Composable
inline fun rememberMapState(
    coroutineScope: CoroutineScope,
    density: Density = LocalDensity.current,
    crossinline init: MapState.() -> Unit = {}
): MapState = remember {
    MapState(coroutineScope, density = density).apply(init)
}