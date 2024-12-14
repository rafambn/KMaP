package com.rafambn.kmap.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import com.rafambn.kmap.mapProperties.MapProperties
import com.rafambn.kmap.mapProperties.border.MapBorderType
import com.rafambn.kmap.mapProperties.CoordinatesRange
import com.rafambn.kmap.utils.CanvasDrawReference
import com.rafambn.kmap.utils.TilePoint
import com.rafambn.kmap.utils.DifferentialScreenOffset
import com.rafambn.kmap.utils.Coordinates
import com.rafambn.kmap.utils.ScreenOffset
import com.rafambn.kmap.utils.asCanvasDrawReference
import com.rafambn.kmap.utils.asCanvasPosition
import com.rafambn.kmap.utils.asDifferentialScreenOffset
import com.rafambn.kmap.utils.asScreenOffset
import com.rafambn.kmap.utils.loopInRange
import com.rafambn.kmap.utils.rotate
import com.rafambn.kmap.utils.toIntFloor
import com.rafambn.kmap.utils.toRadians

@Composable
fun rememberMapState(
    mapProperties: MapProperties
): MapState = remember {
    MapState(mapProperties)
}

class MapState(
    internal val mapProperties: MapProperties
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
    var cameraState by mutableStateOf(
        CameraState(
            tilePoint = TilePoint(mapProperties.coordinatesRange.longitude.mean, mapProperties.coordinatesRange.latitude.mean),
            coordinates = TilePoint(mapProperties.coordinatesRange.longitude.mean, mapProperties.coordinatesRange.latitude.mean).toCoordinates()
        )
    )

    //Derivative variables
    val drawReference
        get() = cameraState.tilePoint.toCanvasDrawReference()
    private val zoomLevel
        get() = cameraState.zoom.toIntFloor()
    private val magnifierScale
        get() = cameraState.zoom - zoomLevel + 1F

    //Utility functions
    val boundingBox
        get() = BoundingBox(
            ScreenOffset.Zero.toTilePoint(),
            ScreenOffset(cameraState.canvasSize.x, 0F).toTilePoint(),
            cameraState.canvasSize.toTilePoint(),
            ScreenOffset(0F, cameraState.canvasSize.y).toTilePoint(),
        )

    val viewPort
        get() = ViewPort(
            ScreenOffset.Zero,
            Size(cameraState.canvasSize.x, cameraState.canvasSize.y)
        )

    private fun TilePoint.coerceInMap(): TilePoint {
        val x = if (mapProperties.boundMap.horizontal == MapBorderType.BOUND)
            horizontal.coerceIn(
                mapProperties.coordinatesRange.longitude.min,
                mapProperties.coordinatesRange.longitude.max
            )
        else
            horizontal.loopInRange(mapProperties.coordinatesRange.longitude)
        val y = if (mapProperties.boundMap.vertical == MapBorderType.BOUND)
            vertical.coerceIn(
                mapProperties.coordinatesRange.latitude.min,
                mapProperties.coordinatesRange.latitude.max
            )
        else
            vertical.loopInRange(mapProperties.coordinatesRange.latitude)
        return TilePoint(x, y)
    }

    private fun Float.coerceZoom(): Float = this.coerceIn(minZoomPreference.toFloat(), maxZoomPreference.toFloat())

    fun centerPositionAtOffset(position: TilePoint, offset: ScreenOffset) {
        setRawPosition(cameraState.tilePoint + position - offset.toTilePoint())
    }

    //Conversion Functions
    fun ScreenOffset.toTilePoint(): TilePoint =
        (cameraState.canvasSize / 2F - this).asDifferentialScreenOffset().toTilePoint() + cameraState.tilePoint

    fun DifferentialScreenOffset.toTilePoint(): TilePoint =
        (this.asCanvasPosition() / density.density.toDouble())
            .scaleToZoom(
                (1 / (mapProperties.tileSize.width * magnifierScale * (1 shl zoomLevel))).toDouble(),
                (1 / (mapProperties.tileSize.height * magnifierScale * (1 shl zoomLevel))).toDouble()
            )
            .rotate(-cameraState.angleDegrees.toRadians())
            .scaleToMap(
                mapProperties.coordinatesRange.longitude.span,
                mapProperties.coordinatesRange.latitude.span
            )
            .applyOrientation(mapProperties.coordinatesRange)

    fun TilePoint.toScreenOffset(): ScreenOffset = -(this - cameraState.tilePoint)
        .applyOrientation(mapProperties.coordinatesRange)
        .scaleToMap(
            1 / mapProperties.coordinatesRange.longitude.span,
            1 / mapProperties.coordinatesRange.latitude.span
        )
        .rotate(cameraState.angleDegrees.toRadians())
        .scaleToZoom(
            (mapProperties.tileSize.width * magnifierScale * (1 shl zoomLevel)).toDouble(),
            (mapProperties.tileSize.height * magnifierScale * (1 shl zoomLevel)).toDouble()
        )
        .times(density.density.toDouble()).asScreenOffset()
        .minus(cameraState.canvasSize / 2F)

    private fun TilePoint.toCanvasDrawReference(): CanvasDrawReference = this.applyOrientation(mapProperties.coordinatesRange)
        .moveToTrueCoordinates(mapProperties.coordinatesRange)
        .scaleToZoom(
            (mapProperties.tileSize.width * (1 shl zoomLevel)).toDouble(),
            (mapProperties.tileSize.height * (1 shl zoomLevel)).toDouble()
        )
        .scaleToMap(
            1 / mapProperties.coordinatesRange.longitude.span,
            1 / mapProperties.coordinatesRange.latitude.span
        )
        .asCanvasDrawReference()

    //Transformation functions
    private fun TilePoint.scaleToZoom(horizontal: Double, vertical: Double): TilePoint =
        TilePoint(this.horizontal * horizontal, this.vertical * vertical)

    private fun TilePoint.moveToTrueCoordinates(coordinatesRange: CoordinatesRange): TilePoint = TilePoint(
        horizontal - coordinatesRange.longitude.span / 2,
        vertical - coordinatesRange.latitude.span / 2
    )

    private fun TilePoint.scaleToMap(horizontal: Double, vertical: Double): TilePoint =
        TilePoint(this.horizontal * horizontal, this.vertical * vertical)

    private fun TilePoint.applyOrientation(coordinatesRange: CoordinatesRange): TilePoint = TilePoint(
        horizontal * coordinatesRange.longitude.orientation,
        vertical * coordinatesRange.latitude.orientation
    )

    fun Coordinates.toTilePoint(): TilePoint = mapProperties.toTilePoint(this@toTilePoint)

    fun TilePoint.toCoordinates(): Coordinates = mapProperties.toCoordinates(this@toCoordinates)

    internal fun setDensity(density: Density) {
        this.density = density
    }

    fun setCanvasSize(offset: Offset) {
        cameraState = cameraState.copy(canvasSize = offset.asScreenOffset())
    }

    fun setZoom(zoom: Float) {
        cameraState = cameraState.copy(zoom = zoom.coerceZoom())
    }

    fun setAngle(angle: Double) {
        cameraState = cameraState.copy(angleDegrees = angle)
    }

    fun setRawPosition(position: TilePoint) {
        cameraState = cameraState.copy(tilePoint = position.coerceInMap())
        println(cameraState.tilePoint)
    }
}