package io.github.rafambn.kmap.core.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import io.github.rafambn.kmap.config.DefaultMapProperties
import io.github.rafambn.kmap.config.MapProperties
import io.github.rafambn.kmap.config.border.MapBorderType
import io.github.rafambn.kmap.config.border.OutsideTilesType
import io.github.rafambn.kmap.config.characteristics.MapCoordinatesRange
import io.github.rafambn.kmap.config.characteristics.MapSource
import io.github.rafambn.kmap.config.customSources.OSMMapSource
import io.github.rafambn.kmap.core.CanvasSizeChangeListener
import io.github.rafambn.kmap.model.BoundingBox
import io.github.rafambn.kmap.model.TileCanvasStateModel
import io.github.rafambn.kmap.model.TileSpecs
import io.github.rafambn.kmap.utils.loopInRange
import io.github.rafambn.kmap.utils.offsets.CanvasDrawReference
import io.github.rafambn.kmap.utils.offsets.CanvasPosition
import io.github.rafambn.kmap.utils.offsets.DifferentialScreenOffset
import io.github.rafambn.kmap.utils.offsets.ProjectedCoordinates
import io.github.rafambn.kmap.utils.offsets.ScreenOffset
import io.github.rafambn.kmap.utils.offsets.toPosition
import io.github.rafambn.kmap.utils.rotate
import io.github.rafambn.kmap.utils.toIntFloor
import io.github.rafambn.kmap.utils.toRadians
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.math.pow

@Composable
fun rememberMapState(): MapState = remember { MapState() }

class MapState : CanvasSizeChangeListener {
    //Map controllers
    private var mapProperties: MapProperties = DefaultMapProperties()
    var mapSource: MapSource = OSMMapSource  //TODO(3) add source future -- online, db, cache or mapFile
    private var density: Density = Density(1F)

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
        internal set(value) {
            field = value.coerceZoom()
        }
    var angleDegrees = 0.0
        internal set
    var rawPosition = CanvasPosition.Zero
        internal set(value) {
            field = value.coerceInMap()
        }
    var projection: ProjectedCoordinates
        get() {
            return with(mapSource) {
                toProjectedCoordinates(rawPosition)
            }
        }
        set(value) {
            rawPosition = with(mapSource) {
                toCanvasPosition(value)
            }
        }
    var canvasSize = Offset.Zero
        internal set

    //Derivative variables
    val zoomLevel
        get() = zoom.toIntFloor()
    val magnifierScale
        get() = zoom - zoomLevel + 1F

    //Map state variable for recomposition
    val trigger = mutableStateOf(false) //TODO(3) make some control variables tied to kmap to remove this
    fun updateState() {
        _canvasSharedState.tryEmit(
            TileCanvasStateModel(
                canvasSize / 2F,
                angleDegrees.toFloat(),
                magnifierScale,
                rawPosition.toCanvasDrawReference(),
                mapSource.tileSize,
                getVisibleTilesForLevel(
                    getBoundingBox(),
                    zoomLevel,
                    mapProperties.outsideTiles,
                    mapSource.mapCoordinatesRange
                ),
                zoomLevel
            )
        )
        trigger.value = !trigger.value
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

    private fun CanvasPosition.coerceInMap(): CanvasPosition {
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

    private fun Float.coerceZoom(): Float {
        return this.coerceIn(minZoomPreference.toFloat(), maxZoomPreference.toFloat())
    }

    fun centerPositionAtOffset(position: CanvasPosition, offset: ScreenOffset) {
        rawPosition += position - offset.fromScreenOffsetToCanvasPosition()
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

    private fun getVisibleTilesForLevel(
        viewPort: BoundingBox,
        zoomLevel: Int,
        outsideTilesType: OutsideTilesType,
        coordinatesRange: MapCoordinatesRange
    ): List<TileSpecs> {
        val topLeftTile = getXYTile(
            viewPort.topLeft,
            zoomLevel,
            coordinatesRange
        )
        val topRightTile = getXYTile(
            viewPort.topRight,
            zoomLevel,
            coordinatesRange
        )
        val bottomRightTile = getXYTile(
            viewPort.bottomRight,
            zoomLevel,
            coordinatesRange
        )
        val bottomLeftTile = getXYTile(
            viewPort.bottomLeft,
            zoomLevel,
            coordinatesRange
        )
        val horizontalTileIntRange =
            IntRange(
                minOf(topLeftTile.first, bottomRightTile.first, topRightTile.first, bottomLeftTile.first),
                maxOf(topLeftTile.first, bottomRightTile.first, topRightTile.first, bottomLeftTile.first)
            )
        val verticalTileIntRange =
            IntRange(
                minOf(topLeftTile.second, bottomRightTile.second, topRightTile.second, bottomLeftTile.second),
                maxOf(topLeftTile.second, bottomRightTile.second, topRightTile.second, bottomLeftTile.second)
            )

        val visibleTileSpecs = mutableListOf<TileSpecs>()
        if (outsideTilesType == OutsideTilesType.NONE) {
            for (x in horizontalTileIntRange)
                for (y in verticalTileIntRange) {
                    var xTile: Int
                    if (x < 0 || x > 2F.pow(zoomLevel) - 1)
                        continue
                    else
                        xTile = x
                    var yTile: Int
                    if (y < 0 || y > 2F.pow(zoomLevel) - 1)
                        continue
                    else
                        yTile = y
                    visibleTileSpecs.add(TileSpecs(zoomLevel, xTile, yTile))
                }
        } else {
            for (x in horizontalTileIntRange)
                for (y in verticalTileIntRange)
                    visibleTileSpecs.add(TileSpecs(zoomLevel, x, y))
        }
        return visibleTileSpecs
    }

    private fun getXYTile(position: CanvasPosition, zoomLevel: Int, mapSize: MapCoordinatesRange): Pair<Int, Int> {
        return Pair(
            ((position.horizontal - mapSize.longitute.getMin()) / mapSize.longitute.span * (1 shl zoomLevel)).toIntFloor(),
            ((-position.vertical + mapSize.latitude.getMax()) / mapSize.latitude.span * (1 shl zoomLevel)).toIntFloor()
        )
    }

    companion object {
        private val _canvasSharedState = MutableSharedFlow<TileCanvasStateModel>(onBufferOverflow = BufferOverflow.DROP_LATEST,
            extraBufferCapacity = 1)
        val canvasSharedState = _canvasSharedState.asSharedFlow()
    }
}