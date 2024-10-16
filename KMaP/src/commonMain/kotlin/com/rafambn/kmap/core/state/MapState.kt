package com.rafambn.kmap.core.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import com.rafambn.kmap.config.MapProperties
import com.rafambn.kmap.config.border.MapBorderType
import com.rafambn.kmap.config.border.OutsideTilesType
import com.rafambn.kmap.config.characteristics.MapCoordinatesRange
import com.rafambn.kmap.model.BoundingBox
import com.rafambn.kmap.model.TileSpecs
import com.rafambn.kmap.utils.loopInRange
import com.rafambn.kmap.utils.offsets.CanvasDrawReference
import com.rafambn.kmap.utils.offsets.CanvasPosition
import com.rafambn.kmap.utils.offsets.DifferentialScreenOffset
import com.rafambn.kmap.utils.offsets.ProjectedCoordinates
import com.rafambn.kmap.utils.offsets.ScreenOffset
import com.rafambn.kmap.utils.offsets.toPosition
import com.rafambn.kmap.utils.rotate
import com.rafambn.kmap.utils.toIntFloor
import com.rafambn.kmap.utils.toRadians
import kotlin.math.pow
import kotlin.reflect.KProperty

@Composable
fun rememberMapState(
    mapProperties: MapProperties
): MapState = remember {
    MapState(mapProperties)
}

class MapState(
    internal val mapProperties: MapProperties //TODO(3) add source future -- online, db, cache or mapFile
) {
    //Map controllers
    private var density: Density = Density(1F)

    //User define min/max zoom
    var maxZoomPreference = mapProperties.zoomLevels.max
        set(value) {
            field = value.coerceIn(mapProperties.zoomLevels.min, mapProperties.zoomLevels.max)
        }
    var minZoomPreference = mapProperties.zoomLevels.min
        set(value) {
            field = value.coerceIn(mapProperties.zoomLevels.min, mapProperties.zoomLevels.max)
        }

    //State variables
    var canvasSize by mutableStateOf(Offset.Zero)
        internal set
    var zoom by ZoomDelegate(mutableStateOf(0F))

    var angleDegrees by mutableStateOf(0.0)
        internal set
    var rawPosition by RawPositionDelegate(mutableStateOf(CanvasPosition.Companion.Zero))

    var projection: ProjectedCoordinates
        get() {
            return rawPosition.toProjectedCoordinates()
        }
        set(value) {
            rawPosition = value.toCanvasPosition()
        }
    val drawReference
        get() = rawPosition.toCanvasDrawReference()

    //Derivative variables
    val zoomLevel
        get() = zoom.toIntFloor()
    val magnifierScale
        get() = zoom - zoomLevel + 1F

    val visibleTiles by derivedStateOf {
        getVisibleTilesForLevel(
            getBoundingBox(),
            zoomLevel,
            mapProperties.outsideTiles,
            mapProperties.mapCoordinatesRange
        )
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
            .scaleToZoom(1 / (mapProperties.tileSize * magnifierScale * (1 shl zoomLevel)))
            .rotate(-angleDegrees.toRadians())
            .scaleToMap(
                mapProperties.mapCoordinatesRange.longitute.span,
                mapProperties.mapCoordinatesRange.latitude.span
            )
            .applyOrientation(mapProperties.mapCoordinatesRange)

    fun CanvasPosition.toCanvasDrawReference(): CanvasDrawReference {
        val canvasDrawReference = this.applyOrientation(mapProperties.mapCoordinatesRange)
            .moveToTrueCoordinates(mapProperties.mapCoordinatesRange)
            .scaleToZoom((mapProperties.tileSize * (1 shl zoomLevel)).toFloat())
            .scaleToMap(
                1 / mapProperties.mapCoordinatesRange.longitute.span,
                1 / mapProperties.mapCoordinatesRange.latitude.span
            )
        return CanvasDrawReference(canvasDrawReference.horizontal, canvasDrawReference.vertical)
    }

    fun CanvasPosition.toScreenOffset(): ScreenOffset = -(this - rawPosition)
        .applyOrientation(mapProperties.mapCoordinatesRange)
        .scaleToMap(
            1 / mapProperties.mapCoordinatesRange.longitute.span,
            1 / mapProperties.mapCoordinatesRange.latitude.span
        )
        .rotate(angleDegrees.toRadians())
        .scaleToZoom(mapProperties.tileSize * magnifierScale * (1 shl zoomLevel))
        .times(density.density.toDouble()).toOffset()
        .minus(canvasSize / 2F)


    fun CanvasPosition.scaleToZoom(zoomScale: Float): CanvasPosition {
        return CanvasPosition(horizontal * zoomScale, vertical * zoomScale)
    }

    fun CanvasPosition.moveToTrueCoordinates(mapCoordinatesRange: MapCoordinatesRange): CanvasPosition {
        return CanvasPosition(
            horizontal - mapCoordinatesRange.longitute.span / 2,
            vertical - mapCoordinatesRange.latitude.span / 2
        )
    }

    fun CanvasPosition.scaleToMap(horizontal: Double, vertical: Double): CanvasPosition {
        return CanvasPosition(this.horizontal * horizontal, this.vertical * vertical)
    }

    fun CanvasPosition.applyOrientation(mapCoordinatesRange: MapCoordinatesRange): CanvasPosition {
        return CanvasPosition(
            horizontal * mapCoordinatesRange.longitute.orientation,
            vertical * mapCoordinatesRange.latitude.orientation
        )
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

    internal fun ProjectedCoordinates.toCanvasPosition(): CanvasPosition = with(mapProperties) {
        toCanvasPosition(this@toCanvasPosition)
    }

    internal fun CanvasPosition.toProjectedCoordinates(): ProjectedCoordinates = with(mapProperties) {
        toProjectedCoordinates(this@toProjectedCoordinates)
    }

   private inner class ZoomDelegate(private var zoom: MutableState<Float>) {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): Float {
            return zoom.value
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Float) {
            zoom.value = value.coerceZoom()
        }
    }

   private inner class RawPositionDelegate(private var zoom: MutableState<CanvasPosition>) {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): CanvasPosition {
            return zoom.value
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: CanvasPosition) {
            zoom.value = value.coerceInMap()
        }
    }
}