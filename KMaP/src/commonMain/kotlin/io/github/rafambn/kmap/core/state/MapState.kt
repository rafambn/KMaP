package io.github.rafambn.kmap.core.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import io.github.rafambn.kmap.config.MapProperties
import io.github.rafambn.kmap.core.CanvasSizeChangeListener
import io.github.rafambn.kmap.model.BoundingBox
import io.github.rafambn.kmap.utils.Degrees
import io.github.rafambn.kmap.utils.offsets.ProjectedCoordinates
import io.github.rafambn.kmap.utils.toIntFloor
import kotlinx.coroutines.CoroutineScope

class MapState(
    coroutineScope: CoroutineScope,
    initialPosition: ProjectedCoordinates,
    initialZoom: Float,
    initialRotation: Degrees,
    val density: Density,
    val mapProperties: MapProperties = MapProperties(),
) : CanvasSizeChangeListener {

    //User define min/max zoom
    var maxZoom = mapProperties.mapSource.zoomLevels.max
        set(value) {
            field = value.coerceIn(mapProperties.mapSource.zoomLevels.min, mapProperties.mapSource.zoomLevels.max)
        }
    var minZoom = mapProperties.mapSource.zoomLevels.min
        set(value) {
            field = value.coerceIn(mapProperties.mapSource.zoomLevels.min, mapProperties.mapSource.zoomLevels.max)
        }

    //Control variables
    var zoom = initialZoom
        internal set
    var angleDegrees = initialRotation
        internal set
    var mapPosition = mapProperties.mapSource.toCanvasPosition(initialPosition) //TODO make if projection
        internal set
    var canvasSize = Offset.Zero
        internal set

    //Derivative variables
    val zoomLevel
        get() = zoom.toIntFloor()
    val magnifierScale
        get() = zoom - zoomLevel + 1F
    internal val positionOffset
        get() = with(motionController) {
            mapPosition.toCanvasDrawReference()
        }

    //Controllers
    val tileCanvasState = TileCanvasState(::redraw, zoomLevel)
    val motionController = MotionController(this, coroutineScope)

    //Map state variable for recomposition
    internal var state by mutableStateOf(false)

    fun updateState() {
        tileCanvasState.onStateChange(
            getBoundingBox(),
            zoomLevel,
            mapProperties.mapSource.mapCoordinatesRange,
            mapProperties.outsideTiles
        )
        redraw()
    }

    private fun redraw() {
        state = !state
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
        density = density
    )
}