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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.exp
import kotlin.math.floor

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
    internal val positionOffset
        get() = mapPosition.toCanvasReference(zoomLevel, mapProperties.mapCoordinatesRange)

    //Map state variable for recomposition
    internal var state by mutableStateOf(false)
    val tileCanvasState = TileCanvasState(::redraw, zoomLevel)

    private fun updateState() {
        tileCanvasState.onStateChange(
            ScreenState(
                getBoundingBox(),
                zoomLevel,
                mapProperties.mapCoordinatesRange,
                mapProperties.outsideTiles,
                maxZoom,
                minZoom
            )
        )
        redraw()
    }

    private fun redraw(){
        state = !state
    }

    //Fling variable
    private var flingPositionJob: Job? = null
    private var flingZoomJob: Job? = null
    private var flingZoomAtPositionJob: Job? = null
    private var flingRotationJob: Job? = null
    private var flingRotationAtPositionJob: Job? = null

    //Interface functions
    override fun setCenter(position: Position) {
        flingPositionJob?.cancel()
        flingZoomAtPositionJob?.cancel()
        flingRotationAtPositionJob?.cancel()
        mapPosition = position.coerceInMap()
        updateState()
    }

    override fun moveBy(position: Position) {
        flingPositionJob?.cancel()
        flingZoomAtPositionJob?.cancel()
        flingRotationAtPositionJob?.cancel()
        mapPosition = (mapPosition + position).coerceInMap()
        updateState()
    }

    override fun animatePositionTo(position: Position, decayRate: Double) {
        flingPositionJob?.cancel()
        flingZoomAtPositionJob?.cancel()
        flingRotationAtPositionJob?.cancel()
        val startPosition = mapPosition
        flingPositionJob = decayValue(coroutineScope, decayRate) {
            mapPosition = lerp(startPosition, position, it).coerceInMap()
            updateState()
        }
    }

    override fun setZoom(zoom: Float) {
        flingZoomJob?.cancel()
        flingZoomAtPositionJob?.cancel()
        this.zoom = zoom.coerceZoom()
        updateState()
    }

    override fun zoomBy(zoom: Float) {
        flingZoomJob?.cancel()
        flingZoomAtPositionJob?.cancel()
        this.zoom = (zoom + this.zoom).coerceZoom()
        updateState()
    }

    override fun zoomBy(zoom: Float, position: Position) {
        flingZoomJob?.cancel()
        flingZoomAtPositionJob?.cancel()
        val previousOffset = positionToCanvasOffset(position)
        this.zoom = (zoom + this.zoom).coerceZoom()
        centerPositionAtOffset(position, previousOffset)
        updateState()
    }

    override fun animateZoomTo(zoom: Float, decayRate: Double) {
        flingZoomJob?.cancel()
        flingZoomAtPositionJob?.cancel()
        val startZoom = this.zoom
        flingZoomJob = decayValue(coroutineScope, decayRate) { decayValue ->
            this.zoom = lerp(startZoom, zoom, decayValue.toFloat()).coerceZoom()
            updateState()
        }
    }

    override fun animateZoomTo(zoom: Float, decayRate: Double, position: Position) {
        flingPositionJob?.cancel()
        flingZoomJob?.cancel()
        flingZoomAtPositionJob?.cancel()
        val startZoom = this.zoom
        val previousOffset = positionToCanvasOffset(position)
        flingZoomAtPositionJob = decayValue(coroutineScope, decayRate) { decayValue ->
            this.zoom = lerp(startZoom, zoom, decayValue.toFloat()).coerceZoom()
            centerPositionAtOffset(position, previousOffset)
            updateState()
        }
    }

    override fun setRotation(angle: Degrees) {
        flingRotationJob?.cancel()
        flingRotationAtPositionJob?.cancel()
        angleDegrees = angle.toFloat()
        updateState()
    }

    override fun rotateBy(angle: Degrees) {
        flingRotationJob?.cancel()
        flingRotationAtPositionJob?.cancel()
        angleDegrees += angle.toFloat()
        updateState()
    }

    override fun rotateBy(angle: Degrees, position: Position) {
        flingRotationJob?.cancel()
        flingRotationAtPositionJob?.cancel()
        val previousOffset = positionToCanvasOffset(position)
        angleDegrees += angle.toFloat()
        centerPositionAtOffset(position, previousOffset)
        updateState()
    }

    override fun animateRotationTo(angle: Degrees, decayRate: Double) {
        flingRotationJob?.cancel()
        flingRotationAtPositionJob?.cancel()
        val startRotation = angleDegrees
        flingRotationJob = decayValue(coroutineScope, decayRate) { decayValue ->
            angleDegrees = lerp(startRotation, angle.toFloat(), decayValue.toFloat())
            updateState()
        }
    }

    override fun animateRotationTo(angle: Degrees, decayRate: Double, position: Position) {
        flingPositionJob?.cancel()
        flingRotationJob?.cancel()
        flingRotationAtPositionJob?.cancel()
        val startRotation = angleDegrees
        val previousOffset = positionToCanvasOffset(position)
        flingRotationAtPositionJob = decayValue(coroutineScope, decayRate) { decayValue ->
            angleDegrees = lerp(startRotation, angle.toFloat(), decayValue.toFloat())
            centerPositionAtOffset(position, previousOffset)
            updateState()
        }
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

    private fun getBoundingBox(): BoundingBox {
        return BoundingBox(
            offsetToMapReference(Offset.Zero),
            offsetToMapReference(Offset(canvasSize.x, 0F)),
            offsetToMapReference(Offset(0F, canvasSize.y)),
            offsetToMapReference(canvasSize),
        )
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
        val steps = 100
        val timeStep = 10L
        for (i in 0 until steps) {
            val x = i.toDouble() / steps
            function((1 - exp(decayRate * x)) / (1 - exp(decayRate)))
            delay(timeStep)
        }
    }

    private fun centerPositionAtOffset(position: Position, offset: Offset) {
        mapPosition = (mapPosition + position - offsetToMapReference(offset)).coerceInMap()
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