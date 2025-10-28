package com.rafambn.kmap.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.rafambn.kmap.mapProperties.MapProperties
import com.rafambn.kmap.mapProperties.ZoomLevelRange
import com.rafambn.kmap.mapProperties.border.MapBorderType
import com.rafambn.kmap.mapSource.tiled.CanvasKernel
import com.rafambn.kmap.mapSource.tiled.TileLayers
import com.rafambn.kmap.utils.CanvasDrawReference
import com.rafambn.kmap.utils.TilePoint
import com.rafambn.kmap.utils.DifferentialScreenOffset
import com.rafambn.kmap.utils.Coordinates
import com.rafambn.kmap.utils.ProjectedCoordinates
import com.rafambn.kmap.utils.ScreenOffset
import com.rafambn.kmap.utils.asCanvasDrawReference
import com.rafambn.kmap.utils.asCanvasPosition
import com.rafambn.kmap.utils.asDifferentialScreenOffset
import com.rafambn.kmap.utils.asOffset
import com.rafambn.kmap.utils.asScreenOffset
import com.rafambn.kmap.utils.div
import com.rafambn.kmap.utils.loopInRange
import com.rafambn.kmap.utils.minus
import com.rafambn.kmap.utils.plus
import com.rafambn.kmap.utils.rotate
import com.rafambn.kmap.utils.times
import com.rafambn.kmap.utils.toIntFloor
import com.rafambn.kmap.utils.toRadians
import com.rafambn.kmap.utils.transformReference
import com.rafambn.kmap.utils.unaryMinus
import kotlinx.coroutines.CoroutineScope
import kotlin.math.pow
import kotlin.reflect.KProperty

@Composable
fun rememberMapState(
    mapProperties: MapProperties,
    zoomLevelPreference: ZoomLevelRange? = null,
    density: Density = LocalDensity.current,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
): MapState = rememberSaveable(
    saver = MapState.saver(mapProperties, coroutineScope),
    init = {
        MapState(
            mapProperties = mapProperties,
            zoomLevelPreference = zoomLevelPreference,
            density = density,
            initialCameraState = null,
            coroutineScope
        )
    }
)

class MapState(
    val mapProperties: MapProperties,
    zoomLevelPreference: ZoomLevelRange? = null,
    var density: Density = Density(1F),
    initialCameraState: CameraState? = null,
    coroutineScope: CoroutineScope
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
        initialCameraState ?: CameraState(
            tilePoint = TilePoint(mapProperties.tileSize.width / 2.0, mapProperties.tileSize.height / 2.0),
            coordinates = TilePoint(mapProperties.tileSize.width / 2.0, mapProperties.tileSize.height / 2.0).toCoordinates()
        )
    )

    operator fun MutableState<CameraState>.setValue(thisObj: Any?, property: KProperty<*>, value: CameraState) {
        this.value = value
        val topLeft = ScreenOffset.Zero.toTilePoint()
        val topRight = ScreenOffset(value.canvasSize.xFloat, 0F).toTilePoint()
        val bottomLeft = ScreenOffset(0F, value.canvasSize.yFloat).toTilePoint()
        val bottomRight = value.canvasSize.toTilePoint()
        val viewPort = Rect(
            minOf(topLeft.x, topRight.x, bottomLeft.x, bottomRight.x).toFloat(),
            minOf(topLeft.y, topRight.y, bottomLeft.y, bottomRight.y).toFloat(),
            maxOf(topLeft.x, topRight.x, bottomLeft.x, bottomRight.x).toFloat(),
            maxOf(topLeft.y, topRight.y, bottomLeft.y, bottomRight.y).toFloat()
        )
        canvasKernel.resolveVisibleTiles(viewPort, value.zoom.toIntFloor(), mapProperties)
    }

    //Draw variables
    val drawMagScale = { cameraState.zoom - cameraState.zoom.toIntFloor() }
    val drawReference = { cameraState.tilePoint.toCanvasDrawReference() }
    val drawTileSize = { mapProperties.tileSize }
    val drawRotationDegrees = { cameraState.angleDegrees.toFloat() }
    val drawTranslation = { cameraState.canvasSize.asOffset() / 2F }

    //Derivative variables
    private val zoomLevel
        get() = cameraState.zoom.toIntFloor()

    //Canvas Kernel
    val canvasKernel = CanvasKernel(coroutineScope)

    //Utility functions
    private fun TilePoint.coerceInMap(): TilePoint {
        val x = if (mapProperties.boundMap.horizontal == MapBorderType.BOUND)
            x.coerceIn(0.0, mapProperties.tileSize.width.toDouble())
        else
            x.loopInRange(mapProperties.tileSize.width.toDouble())
        val y = if (mapProperties.boundMap.vertical == MapBorderType.BOUND)
            y.coerceIn(0.0, mapProperties.tileSize.height.toDouble())
        else
            y.loopInRange(mapProperties.tileSize.height.toDouble())
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
            (mapProperties.tileSize.width.toDouble() / (mapProperties.tileSize.width * 2F.pow(cameraState.zoom))),
            (mapProperties.tileSize.height.toDouble() / (mapProperties.tileSize.height * 2F.pow(cameraState.zoom)))
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
        TilePoint(this.x * horizontal, this.y * vertical)

    fun Coordinates.toTilePoint(): TilePoint {
        val projectedCoordinates = mapProperties.toProjectedCoordinates(this@toTilePoint)
        val scaledTilePoint = transformReference(
            projectedCoordinates.x,
            projectedCoordinates.y,
            Pair(mapProperties.coordinatesRange.longitude.west, mapProperties.coordinatesRange.longitude.east),
            Pair(mapProperties.coordinatesRange.latitude.north, mapProperties.coordinatesRange.latitude.south),
            Pair(0.0, mapProperties.tileSize.width.toDouble()),
            Pair(0.0, mapProperties.tileSize.height.toDouble()),
        )
        return TilePoint(scaledTilePoint.first, scaledTilePoint.second)
    }

    fun ProjectedCoordinates.toTilePoint(): TilePoint {
        val scaledTilePoint = transformReference(
            this.x,
            this.y,
            Pair(mapProperties.coordinatesRange.longitude.west, mapProperties.coordinatesRange.longitude.east),
            Pair(mapProperties.coordinatesRange.latitude.north, mapProperties.coordinatesRange.latitude.south),
            Pair(0.0, mapProperties.tileSize.width.toDouble()),
            Pair(0.0, mapProperties.tileSize.height.toDouble()),
        )
        return TilePoint(scaledTilePoint.first, scaledTilePoint.second)
    }

    fun TilePoint.toCoordinates(): Coordinates {
        val scaledTileCoordinates = transformReference(
            this.x,
            this.y,
            Pair(0.0, mapProperties.tileSize.width.toDouble()),
            Pair(0.0, mapProperties.tileSize.height.toDouble()),
            Pair(mapProperties.coordinatesRange.longitude.west, mapProperties.coordinatesRange.longitude.east),
            Pair(mapProperties.coordinatesRange.latitude.north, mapProperties.coordinatesRange.latitude.south),
        )
        return mapProperties.toCoordinates(ProjectedCoordinates(scaledTileCoordinates.first, scaledTileCoordinates.second))
    }

    fun setCanvasSize(offset: Offset) {
        if (offset.asScreenOffset() == cameraState.canvasSize)
            return
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

    companion object {
        fun saver(mapProperties: MapProperties, coroutineScope: CoroutineScope) = mapSaver(
            save = { mapState ->
                mapOf(
                    "zoomLevelPreference" to Pair(mapState.zoomLevelPreference.min, mapState.zoomLevelPreference.max),
                    "density" to mapState.density.density,
                    "canvasSize" to Pair(mapState.cameraState.canvasSize.x, mapState.cameraState.canvasSize.y),
                    "zoom" to mapState.cameraState.zoom,
                    "angleDegrees" to mapState.cameraState.angleDegrees,
                    "coordinates" to Pair(mapState.cameraState.coordinates.x, mapState.cameraState.coordinates.y),
                    "tilePoint" to Pair(mapState.cameraState.tilePoint.x, mapState.cameraState.tilePoint.y),
                )
            },
            restore = { map ->
                MapState(
                    mapProperties = mapProperties,
                    zoomLevelPreference = (map["zoomLevelPreference"] as Pair<*, *>).let {
                        object : ZoomLevelRange {
                            override val max: Int = it.second as Int
                            override val min: Int = it.first as Int
                        }
                    },
                    density = Density(map["density"] as Float),
                    initialCameraState = CameraState(
                        canvasSize = (map["canvasSize"] as Pair<*, *>).let { ScreenOffset(it.first as Float, it.second as Float) },
                        zoom = map["zoom"] as Float,
                        angleDegrees = map["angleDegrees"] as Double,
                        coordinates = (map["coordinates"] as Pair<*, *>).let { Coordinates(it.first as Double, it.second as Double) },
                        tilePoint = (map["tilePoint"] as Pair<*, *>).let { TilePoint(it.first as Double, it.second as Double) },
                    ),
                    coroutineScope = coroutineScope
                )
            }
        )
    }
}
