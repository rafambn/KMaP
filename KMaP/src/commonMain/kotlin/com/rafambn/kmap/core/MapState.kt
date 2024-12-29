package com.rafambn.kmap.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.rafambn.kmap.mapProperties.MapProperties
import com.rafambn.kmap.mapProperties.ZoomLevelRange
import com.rafambn.kmap.mapProperties.border.MapBorderType
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
import com.rafambn.kmap.utils.transformReference
import kotlin.math.pow

@Composable
fun rememberMapState(
    //TODO add saveable
    mapProperties: MapProperties,
    zoomLevelPreference: ZoomLevelRange? = null,
    density: Density = LocalDensity.current,
): MapState = remember {
    MapState(
        mapProperties = mapProperties,
        zoomLevelPreference = zoomLevelPreference,
        density = density,
    )
}

class MapState(
    internal val mapProperties: MapProperties,
    zoomLevelPreference: ZoomLevelRange? = null,
    var density: Density = Density(1F),
) {
    val motionController = MotionController(this)

    //User define min/max zoom
    var zoomLevelPreference = zoomLevelPreference ?: mapProperties.zoomLevels
        set(value) {
            if (value.max > mapProperties.zoomLevels.max || value.min < mapProperties.zoomLevels.min)
                throw IllegalArgumentException("Zoom level is out of bounds")
            field = value
        }

    //State variables
    var cameraState by mutableStateOf(
        CameraState(
            tilePoint = TilePoint(mapProperties.tileSize.width / 2.0, mapProperties.tileSize.height / 2.0),
            coordinates = TilePoint(mapProperties.tileSize.width / 2.0, mapProperties.tileSize.height / 2.0).toCoordinates()
        )
    )

    //Derivative variables
    val drawReference
        get() = cameraState.tilePoint.toCanvasDrawReference()
    private val zoomLevel
        get() = cameraState.zoom.toIntFloor()

    //Utility functions
    val viewPort
        get() = {
            val topLeft = ScreenOffset.Zero.toTilePoint()
            val topRight = ScreenOffset(cameraState.canvasSize.x, 0F).toTilePoint()
            val bottomLeft = ScreenOffset(0F, cameraState.canvasSize.y).toTilePoint()
            val bottomRight = cameraState.canvasSize.toTilePoint()
            Rect(
                minOf(topLeft.horizontal, topRight.horizontal, bottomLeft.horizontal, bottomRight.horizontal).toFloat(),
                minOf(topLeft.vertical, topRight.vertical, bottomLeft.vertical, bottomRight.vertical).toFloat(),
                maxOf(topLeft.horizontal, topRight.horizontal, bottomLeft.horizontal, bottomRight.horizontal).toFloat(),
                maxOf(topLeft.vertical, topRight.vertical, bottomLeft.vertical, bottomRight.vertical).toFloat()
            )
        }.invoke()

    private fun TilePoint.coerceInMap(): TilePoint {
        val x = if (mapProperties.boundMap.horizontal == MapBorderType.BOUND)
            horizontal.coerceIn(0.0, mapProperties.tileSize.width.toDouble())
        else
            horizontal.loopInRange(mapProperties.tileSize.width.toDouble())
        val y = if (mapProperties.boundMap.vertical == MapBorderType.BOUND)
            vertical.coerceIn(0.0, mapProperties.tileSize.height.toDouble())
        else
            vertical.loopInRange(mapProperties.tileSize.height.toDouble())
        return TilePoint(x, y)
    }

    private fun Float.coerceZoom(): Float = this.coerceIn(zoomLevelPreference.min.toFloat(), zoomLevelPreference.max.toFloat())

    fun centerPointAtOffset(tilePoint: TilePoint, offset: ScreenOffset) {
        setPosition(cameraState.tilePoint + tilePoint - offset.toTilePoint())
    }

    fun ScreenOffset.toTilePoint(): TilePoint =
        (cameraState.canvasSize / 2F - this).asDifferentialScreenOffset().toTilePoint() + cameraState.tilePoint

    fun DifferentialScreenOffset.toTilePoint(): TilePoint = this
        .asCanvasPosition()
        .div(density.density.toDouble())
        .scale(
            (mapProperties.tileSize.width.toDouble() / (mapProperties.tileSize.width * 2F.pow(cameraState.zoom))).toDouble(),
            (mapProperties.tileSize.height.toDouble() / (mapProperties.tileSize.height * 2F.pow(cameraState.zoom))).toDouble()
        )
        .rotate(-cameraState.angleDegrees.toRadians())
        .unaryMinus()

    fun TilePoint.toScreenOffset(): ScreenOffset = (this - cameraState.tilePoint)
        .unaryMinus()
        .rotate(cameraState.angleDegrees.toRadians())
        .scale(
            mapProperties.tileSize.width * 2F.pow(cameraState.zoom) / mapProperties.tileSize.width.toDouble(),
            mapProperties.tileSize.height * 2F.pow(cameraState.zoom) / mapProperties.tileSize.height.toDouble()
        )
        .times(density.density.toDouble())
        .asScreenOffset()
        .minus(cameraState.canvasSize / 2F)
        .unaryMinus()

    private fun TilePoint.toCanvasDrawReference(): CanvasDrawReference = this
        .scale(
            mapProperties.tileSize.width * (1 shl zoomLevel) / mapProperties.tileSize.width.toDouble(),
            mapProperties.tileSize.height * (1 shl zoomLevel) / mapProperties.tileSize.height.toDouble()
        )
        .unaryMinus()
        .asCanvasDrawReference()

    private fun TilePoint.scale(horizontal: Double, vertical: Double): TilePoint =
        TilePoint(this.horizontal * horizontal, this.vertical * vertical)

    fun Coordinates.toTilePoint(): TilePoint {
        val unscaledTilePoint = mapProperties.toTilePoint(this@toTilePoint)
        val scaledTilePoint = transformReference(
            unscaledTilePoint.horizontal,
            unscaledTilePoint.vertical,
            Pair(mapProperties.coordinatesRange.longitude.west, mapProperties.coordinatesRange.longitude.east),
            Pair(mapProperties.coordinatesRange.latitude.north, mapProperties.coordinatesRange.latitude.south),
            Pair(0.0, mapProperties.tileSize.width.toDouble()),
            Pair(0.0, mapProperties.tileSize.height.toDouble()),
        )
        return TilePoint(scaledTilePoint.first, scaledTilePoint.second)
    }

    fun TilePoint.toCoordinates(): Coordinates {
        val unscaledCoordinates = mapProperties.toCoordinates(this@toCoordinates)
        val scaledTileCoordinates = transformReference(
            unscaledCoordinates.longitude,
            unscaledCoordinates.latitude,
            Pair(0.0, mapProperties.tileSize.width.toDouble()),
            Pair(0.0, mapProperties.tileSize.height.toDouble()),
            Pair(mapProperties.coordinatesRange.longitude.west, mapProperties.coordinatesRange.longitude.east),
            Pair(mapProperties.coordinatesRange.latitude.north, mapProperties.coordinatesRange.latitude.south),
        )
        return Coordinates(scaledTileCoordinates.first, scaledTileCoordinates.second)
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

    fun setPosition(position: TilePoint) {
        val coercedPoint = position.coerceInMap()
        cameraState = cameraState.copy(
            tilePoint = coercedPoint,
            coordinates = coercedPoint.toCoordinates()
        )
    }
}