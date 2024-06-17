package io.github.rafambn.kmap.core.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import io.github.rafambn.kmap.config.DefaultMapProperties
import io.github.rafambn.kmap.config.MapProperties
import io.github.rafambn.kmap.config.MapSource
import io.github.rafambn.kmap.config.border.MapBorderType
import io.github.rafambn.kmap.config.characteristics.MapCoordinatesRange
import io.github.rafambn.kmap.config.sources.openStreetMaps.OSMMapSource
import io.github.rafambn.kmap.core.CanvasSizeChangeListener
import io.github.rafambn.kmap.model.BoundingBox
import io.github.rafambn.kmap.model.TileCanvasStateModel
import io.github.rafambn.kmap.utils.loopInRange
import io.github.rafambn.kmap.utils.offsets.CanvasDrawReference
import io.github.rafambn.kmap.utils.offsets.CanvasPosition
import io.github.rafambn.kmap.utils.offsets.DifferentialScreenOffset
import io.github.rafambn.kmap.utils.offsets.ScreenOffset
import io.github.rafambn.kmap.utils.offsets.toPosition
import io.github.rafambn.kmap.utils.rotate
import io.github.rafambn.kmap.utils.toIntFloor
import io.github.rafambn.kmap.utils.toRadians
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.exp

@Composable
fun rememberMapState(): MapState = remember { MapState() }

class MapState : CanvasSizeChangeListener {
    //Map controllers
    private var mapProperties: MapProperties = DefaultMapProperties()
    var mapSource: MapSource = OSMMapSource  //TODO(3) add source future -- online, db, cache or mapFile
    private var density: Density = Density(1F)
    private val tileCanvasState = TileCanvasState(::redraw, mapSource::getTile, zoomLevel)

    //User define min/max zoom
    var maxZoomPreference = mapSource.zoomLevels.max
        set(value) {
            field = value.coerceIn(mapSource.zoomLevels.min, mapSource.zoomLevels.max)
        }
    var minZoomPreference = mapSource.zoomLevels.min
        set(value) {
            field = value.coerceIn(mapSource.zoomLevels.min, mapSource.zoomLevels.max)
        }

    //Control variables
    var zoom = 0F
        internal set
    var angleDegrees = 0.0
        internal set
    var rawPosition = CanvasPosition.Zero //TODO(2) make if projection
        internal set
    var canvasSize = Offset.Zero
        internal set

    //Derivative variables
    val zoomLevel
        get() = zoom.toIntFloor()
    val magnifierScale
        get() = zoom - zoomLevel + 1F

    //Map state variable for recomposition
    private val _tileCanvasStateFlow = MutableStateFlow(
        TileCanvasStateModel(
            canvasSize / 2F,
            angleDegrees.toFloat(),
            magnifierScale,
            tileCanvasState.tileLayers,
            rawPosition.toCanvasDrawReference(),
            mapSource.tileSize,
            false
        )
    )
    val tileCanvasStateFlow: StateFlow<TileCanvasStateModel> = _tileCanvasStateFlow

    fun updateState() {
        tileCanvasState.onStateChange(
            getBoundingBox(),
            zoomLevel,
            mapSource.mapCoordinatesRange,
            mapProperties.outsideTiles
        )
        redraw()
    }

    private fun redraw() {
        _tileCanvasStateFlow.update {
            TileCanvasStateModel(
                canvasSize / 2F,
                angleDegrees.toFloat(),
                magnifierScale,
                tileCanvasState.tileLayers,
                rawPosition.toCanvasDrawReference(),
                mapSource.tileSize,
                !it.trigger //TODO(1) when TileCanvas became possible to be set remove this
            )
        }
    }

    fun set(action: (MapState, MapSource) -> Unit) {
        action(this, mapSource)
        updateState()
    }

    fun set(action: (MapState) -> Unit) {
        action(this)
        updateState()
    }

    fun scroll(action: (MapState, MapSource) -> Unit) {
        action(this, mapSource)
        updateState()
    }

    fun scroll(action: (MapState) -> Unit) {
        action(this)
        updateState()
    }

    fun animate(action: (MapState, MapSource) -> Unit) {
        action(this, mapSource)
        updateState()
    }

    fun animate(action: (MapState) -> Unit) {
        action(this)
        updateState()
    }

    override fun onCanvasSizeChanged(size: Offset) {
        canvasSize = size
        updateState()
    }

    internal fun setProperties(mapProperties: MapProperties) {
        this.mapProperties = mapProperties
    }

    internal fun setMapSource(mapSource: MapSource) {
        this.mapSource = mapSource
    }

    internal fun setDensity(density: Density) {
        this.density = density
    }

    //Utility functions
    private fun getBoundingBox(): BoundingBox {
        return BoundingBox(
            Offset.Zero.fromScreenOffsetToCanvasPosition(),
            Offset(canvasSize.x, 0F).fromScreenOffsetToCanvasPosition(),
            Offset(0F, canvasSize.y).fromScreenOffsetToCanvasPosition(),
            canvasSize.fromScreenOffsetToCanvasPosition(),
        )
    }


    fun CanvasPosition.coerceInMap(): CanvasPosition {
        val x = if (mapProperties.boundMap.horizontal == MapBorderType.BOUND)
            horizontal.coerceIn(
                mapSource.mapCoordinatesRange.longitute.west,
                mapSource.mapCoordinatesRange.longitute.east
            )
        else
            horizontal.loopInRange(mapSource.mapCoordinatesRange.longitute)
        val y = if (mapProperties.boundMap.vertical == MapBorderType.BOUND)
            vertical.coerceIn(
                mapSource.mapCoordinatesRange.latitude.south,
                mapSource.mapCoordinatesRange.latitude.north
            )
        else
            vertical.loopInRange(mapSource.mapCoordinatesRange.latitude)
        return CanvasPosition(x, y)
    }

    fun Float.coerceZoom(): Float {
        return this.coerceIn(minZoomPreference.toFloat(), maxZoomPreference.toFloat())
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

    fun centerPositionAtOffset(position: CanvasPosition, offset: ScreenOffset) {
        rawPosition = (rawPosition + position - offset.fromScreenOffsetToCanvasPosition()).coerceInMap()
    }

    //Conversion Functions
    fun ScreenOffset.fromScreenOffsetToCanvasPosition(): CanvasPosition = this.toCanvasPositionFromScreenCenter() + rawPosition

    fun DifferentialScreenOffset.toCanvasPositionFromScreenCenter(): CanvasPosition =
        (canvasSize / 2F - this).fromDifferentialScreenOffsetToCanvasPosition()

    fun DifferentialScreenOffset.fromDifferentialScreenOffsetToCanvasPosition(): CanvasPosition =
        (this.toPosition() / density.density.toDouble())
            .scaleToZoom(1 / (mapSource.tileSize * magnifierScale * (1 shl zoomLevel)))
            .rotate(-angleDegrees.toRadians())
            .scaleToMap(
                mapSource.mapCoordinatesRange.longitute.span,
                mapSource.mapCoordinatesRange.latitude.span
            )
            .applyOrientation(mapSource.mapCoordinatesRange)

    fun CanvasPosition.toCanvasDrawReference(): CanvasDrawReference {
        val canvasDrawReference = this.applyOrientation(mapSource.mapCoordinatesRange)
            .moveToTrueCoordinates(mapSource.mapCoordinatesRange)
            .scaleToZoom((mapSource.tileSize * (1 shl zoomLevel)).toFloat())
            .scaleToMap(
                1 / mapSource.mapCoordinatesRange.longitute.span,
                1 / mapSource.mapCoordinatesRange.latitude.span
            )
        return CanvasDrawReference(canvasDrawReference.horizontal, canvasDrawReference.vertical)
    }

    fun CanvasPosition.toScreenOffset(): ScreenOffset = -(this - rawPosition)
        .applyOrientation(mapSource.mapCoordinatesRange)
        .scaleToMap(
            1 / mapSource.mapCoordinatesRange.longitute.span,
            1 / mapSource.mapCoordinatesRange.latitude.span
        )
        .rotate(angleDegrees.toRadians())
        .scaleToZoom(mapSource.tileSize * magnifierScale * (1 shl zoomLevel))
        .times(density.density.toDouble()).toOffset()
        .minus(canvasSize / 2F)


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