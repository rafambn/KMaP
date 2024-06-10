package io.github.rafambn.kmap.core.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import io.github.rafambn.kmap.config.MapProperties
import io.github.rafambn.kmap.config.MapSource
import io.github.rafambn.kmap.config.sources.openStreetMaps.OSMMapSource
import io.github.rafambn.kmap.core.CanvasSizeChangeListener
import io.github.rafambn.kmap.model.BoundingBox
import io.github.rafambn.kmap.model.TileCanvasStateModel
import io.github.rafambn.kmap.utils.Degrees
import io.github.rafambn.kmap.utils.offsets.ProjectedCoordinates
import io.github.rafambn.kmap.utils.toIntFloor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class MapState(
    coroutineScope: CoroutineScope,
    initialPosition: ProjectedCoordinates,
    initialZoom: Float,
    initialRotation: Degrees,
    val density: Density,
    val mapProperties: MapProperties = MapProperties(),
    val mapSource: MapSource  //TODO add source future -- online, db, cache or mapFile
) : CanvasSizeChangeListener {

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
    var zoom = initialZoom
        internal set
    var angleDegrees = initialRotation
        internal set
    var mapPosition = mapSource.toCanvasPosition(initialPosition) //TODO make if projection
        internal set
    var canvasSize = Offset.Zero
        internal set

    //Derivative variables
    val zoomLevel
        get() = zoom.toIntFloor()
    val magnifierScale
        get() = zoom - zoomLevel + 1F

    //Controllers
    private val tileCanvasState = TileCanvasState(::redraw,mapSource::getTile, zoomLevel)
    val motionController = MotionController(this, coroutineScope)

    //Map state variable for recomposition
    private val _tileCanvasStateFlow = MutableStateFlow(
        TileCanvasStateModel(
            canvasSize / 2F,
            angleDegrees.toFloat(),
            magnifierScale,
            tileCanvasState.tileLayers,
            with(motionController) {
                mapPosition.toCanvasDrawReference()
            },
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
                with(motionController) {
                    mapPosition.toCanvasDrawReference()
                },
                mapSource.tileSize,
                !it.trigger //TODO when TileCanvas became possible to be set remove this
            )
        }
    }

    override fun onCanvasSizeChanged(size: Offset) {
        canvasSize = size
        updateState()
    }

    //Utility functions
    private fun getBoundingBox(): BoundingBox {
        return with(motionController) {
            BoundingBox(
                Offset.Zero.fromScreenOffsetToCanvasPosition(),
                Offset(canvasSize.x, 0F).fromScreenOffsetToCanvasPosition(),
                Offset(0F, canvasSize.y).fromScreenOffsetToCanvasPosition(),
                canvasSize.fromScreenOffsetToCanvasPosition(),
            )
        }
    }
}

@Composable
fun rememberMapState(
    coroutineScope: CoroutineScope,
    mapSource: MapSource,
    initialPosition: ProjectedCoordinates = ProjectedCoordinates.Zero,
    initialZoom: Float = 0F,
    initialRotation: Degrees = 0.0,
    density: Density = LocalDensity.current,
): MapState = remember {
    MapState(
        coroutineScope,
        initialPosition = initialPosition,
        initialZoom = initialZoom,
        initialRotation = initialRotation,
        density = density,
        mapSource = mapSource
    )
}