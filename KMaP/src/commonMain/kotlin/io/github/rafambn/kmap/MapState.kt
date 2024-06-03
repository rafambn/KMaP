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
import io.github.rafambn.kmap.utils.CanvasPosition
import io.github.rafambn.kmap.utils.Degrees
import io.github.rafambn.kmap.utils.Position
import io.github.rafambn.kmap.utils.ProjectedCoordinates
import io.github.rafambn.kmap.utils.ScreenOffset
import io.github.rafambn.kmap.utils.lerp
import io.github.rafambn.kmap.utils.loopInRange
import io.github.rafambn.kmap.utils.toCanvasDrawReference
import io.github.rafambn.kmap.utils.toScreenOffset
import io.github.rafambn.kmap.utils.toCanvasPosition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.exp
import kotlin.math.floor

class MapState(
    private val coroutineScope: CoroutineScope,
    initialPosition: ProjectedCoordinates = Position.Zero,
    initialZoom: Float = 0F,
    initialRotation: Degrees = 0.0,
    maxZoom: Int = 19,
    minZoom: Int = 0,
    val mapProperties: MapProperties = MapProperties(zoomLevels = OSMZoomlevelsRange, mapCoordinatesRange = OSMCoordinatesRange, tileSize = 256),
    val density: Density
) : MotionInterface, CanvasSizeChangeListener {

    //User define min/max zoom
    private var maxZoom = maxZoom.coerceIn(mapProperties.zoomLevels.min, mapProperties.zoomLevels.max)
    private var minZoom = minZoom.coerceIn(mapProperties.zoomLevels.min, mapProperties.zoomLevels.max)

    //Control variables
    var zoom = initialZoom //TODO use field
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
        get() = mapPosition.toCanvasDrawReference(zoomLevel, mapProperties.mapCoordinatesRange, mapProperties.tileSize)

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

    private fun redraw() {
        state = !state
    }

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
        mapPosition = position.coerceInMap()
        updateState()
    }

    override fun moveBy(position: CanvasPosition) {
        flingPositionJob?.cancel()
        flingZoomAtPositionJob?.cancel()
        flingRotationAtPositionJob?.cancel()
        mapPosition = (mapPosition + position).coerceInMap()
        updateState()
    }

    override fun animatePositionTo(position: CanvasPosition, decayRate: Double) {
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

    override fun zoomBy(zoom: Float, position: CanvasPosition) {
        flingZoomJob?.cancel()
        flingZoomAtPositionJob?.cancel()
        val previousOffset =
            position.toScreenOffset(mapPosition, canvasSize, magnifierScale, zoomLevel, angleDegrees, mapProperties.mapCoordinatesRange, density,
                mapProperties.tileSize)
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

    override fun animateZoomTo(zoom: Float, decayRate: Double, position: CanvasPosition) {
        flingPositionJob?.cancel()
        flingZoomJob?.cancel()
        flingZoomAtPositionJob?.cancel()
        val startZoom = this.zoom
        val previousOffset =
            position.toScreenOffset(mapPosition, canvasSize, magnifierScale, zoomLevel, angleDegrees, mapProperties.mapCoordinatesRange, density,
                mapProperties.tileSize)
        flingZoomAtPositionJob = decayValue(coroutineScope, decayRate) { decayValue ->
            this.zoom = lerp(startZoom, zoom, decayValue.toFloat()).coerceZoom()
            centerPositionAtOffset(position, previousOffset)
            updateState()
        }
    }

    override fun setRotation(angle: Degrees) {
        flingRotationJob?.cancel()
        flingRotationAtPositionJob?.cancel()
        angleDegrees = angle
        updateState()
    }

    override fun rotateBy(angle: Degrees) {
        flingRotationJob?.cancel()
        flingRotationAtPositionJob?.cancel()
        angleDegrees += angle
        updateState()
    }

    override fun rotateBy(angle: Degrees, position: CanvasPosition) {
        flingRotationJob?.cancel()
        flingRotationAtPositionJob?.cancel()
        val previousOffset =
            position.toScreenOffset(mapPosition, canvasSize, magnifierScale, zoomLevel, angleDegrees, mapProperties.mapCoordinatesRange, density,
                mapProperties.tileSize)
        angleDegrees += angle.toFloat()
        centerPositionAtOffset(position, previousOffset)
        updateState()
    }

    override fun animateRotationTo(angle: Degrees, decayRate: Double) {
        flingRotationJob?.cancel()
        flingRotationAtPositionJob?.cancel()
        val startRotation = angleDegrees
        flingRotationJob = decayValue(coroutineScope, decayRate) { decayValue ->
            angleDegrees = lerp(startRotation, angle, decayValue)
            updateState()
        }
    }

    override fun animateRotationTo(angle: Degrees, decayRate: Double, position: CanvasPosition) {
        flingPositionJob?.cancel()
        flingRotationJob?.cancel()
        flingRotationAtPositionJob?.cancel()
        val startRotation = angleDegrees
        val previousOffset =
            position.toScreenOffset(mapPosition, canvasSize, magnifierScale, zoomLevel, angleDegrees, mapProperties.mapCoordinatesRange, density,
                mapProperties.tileSize)
        flingRotationAtPositionJob = decayValue(coroutineScope, decayRate) { decayValue ->
            angleDegrees = lerp(startRotation, angle, decayValue)
            centerPositionAtOffset(position, previousOffset)
            updateState()
        }
    }

    override fun onCanvasSizeChanged(size: Offset) {
        canvasSize = size
        updateState()
    }

    //Utility functions

    private fun getBoundingBox(): BoundingBox {
        return BoundingBox(
            Offset.Zero.toCanvasPosition(
                mapPosition,
                canvasSize,
                magnifierScale,
                zoomLevel,
                angleDegrees,
                mapProperties.mapCoordinatesRange,
                density,
                mapProperties.tileSize
            ),
            Offset(canvasSize.x, 0F).toCanvasPosition(
                mapPosition,
                canvasSize,
                magnifierScale,
                zoomLevel,
                angleDegrees,
                mapProperties.mapCoordinatesRange,
                density,
                mapProperties.tileSize
            ),
            Offset(0F, canvasSize.y).toCanvasPosition(
                mapPosition,
                canvasSize,
                magnifierScale,
                zoomLevel,
                angleDegrees,
                mapProperties.mapCoordinatesRange,
                density,
                mapProperties.tileSize
            ),
            canvasSize.toCanvasPosition(
                mapPosition,
                canvasSize,
                magnifierScale,
                zoomLevel,
                angleDegrees,
                mapProperties.mapCoordinatesRange,
                density,
                mapProperties.tileSize
            ),
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

    private fun centerPositionAtOffset(position: CanvasPosition, offset: ScreenOffset) {
        mapPosition = (mapPosition + position - offset.toCanvasPosition(
            mapPosition,
            canvasSize,
            magnifierScale,
            zoomLevel,
            angleDegrees,
            mapProperties.mapCoordinatesRange,
            density,
            mapProperties.tileSize
        )).coerceInMap()
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