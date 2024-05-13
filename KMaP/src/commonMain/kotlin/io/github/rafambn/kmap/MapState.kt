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
import io.github.rafambn.kmap.utils.decayValue
import io.github.rafambn.kmap.utils.lerp
import io.github.rafambn.kmap.utils.loopInRange
import io.github.rafambn.kmap.utils.offsetToMapReference
import io.github.rafambn.kmap.utils.positionToCanvasOffset
import io.github.rafambn.kmap.utils.toCanvasDrawReference
import io.github.rafambn.kmap.utils.toPosition
import io.github.rafambn.kmap.utils.toViewportReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlin.math.floor

class MapState(
    private val coroutineScope: CoroutineScope,
    initialPosition: Position = Position.Zero,
    initialZoom: Float = 0F,
    initialRotation: Float = 0F,
    maxZoom: Int = 19,
    minZoom: Int = 0,
    val mapProperties: MapProperties = MapProperties(zoomLevels = OSMZoomlevelsRange, mapCoordinatesRange = OSMCoordinatesRange),
    val density: Density
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
        get() = mapPosition.toCanvasDrawReference(zoomLevel, mapProperties.mapCoordinatesRange)

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
            val previousOffset = it.positionToCanvasOffset(
                mapPosition,
                magnifierScale,
                zoomLevel,
                mapProperties.mapCoordinatesRange,
                angleDegrees.toDouble(),
                density,
                canvasSize
            )
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
        val previousOffset = position?.positionToCanvasOffset(
            mapPosition,
            magnifierScale,
            zoomLevel,
            mapProperties.mapCoordinatesRange,
            angleDegrees.toDouble(),
            density,
            canvasSize
        ) ?: Offset.Zero
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
        val previousOffset = position?.positionToCanvasOffset(
            mapPosition,
            magnifierScale,
            zoomLevel,
            mapProperties.mapCoordinatesRange,
            angleDegrees.toDouble(),
            density,
            canvasSize
        ) ?: Offset.Zero
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
        val previousOffset = position?.positionToCanvasOffset(
            mapPosition,
            magnifierScale,
            zoomLevel,
            mapProperties.mapCoordinatesRange,
            angleDegrees.toDouble(),
            density,
            canvasSize
        ) ?: Offset.Zero
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

    private fun centerPositionAtOffset(position: Position, offset: Offset) {
        mapPosition = (mapPosition + position - offset.offsetToMapReference(
            mapPosition,
            canvasSize,
            magnifierScale,
            zoomLevel,
            angleDegrees.toDouble(),
            mapProperties.mapCoordinatesRange,
            density
        )).coerceInMap()
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