package com.rafambn.kmap

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.rafambn.kmap.camera.CameraState
import com.rafambn.kmap.camera.MotionController
import com.rafambn.kmap.geometry.angle.Degrees
import com.rafambn.kmap.mapProperties.MapProperties
import com.rafambn.kmap.mapProperties.ZoomLevelRange
import com.rafambn.kmap.mapProperties.border.MapBorderType
import com.rafambn.kmap.mapSource.tiled.engine.CanvasKernel
import com.rafambn.kmap.utils.*
import kotlinx.coroutines.CoroutineScope
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
            coroutineScope = coroutineScope,
        )
    }
)

class MapState(
    val mapProperties: MapProperties,
    zoomLevelPreference: ZoomLevelRange? = null,
    initialCameraState: CameraState? = null,
    coroutineScope: CoroutineScope,
    density: Density = Density(1f, 1f),
) : Density {
    override var density = density.density
    override val fontScale = density.fontScale

    val motionController = MotionController(this)

    var zoomLevelPreference = zoomLevelPreference ?: mapProperties.zoomLevels
        set(value) {
            if (value.max > mapProperties.zoomLevels.max || value.min < mapProperties.zoomLevels.min)
                throw IllegalArgumentException("Zoom level is out of bounds")
            field = value
        }

    var cameraState by mutableStateOf(
        initialCameraState ?: CameraState(
            tilePoint = TilePoint(mapProperties.tileSize.width.toPx() / 2.0, mapProperties.tileSize.height.toPx() / 2.0),
            coordinates = TilePoint(mapProperties.tileSize.width.toPx() / 2.0, mapProperties.tileSize.height.toPx() / 2.0).toCoordinates()
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

    val drawMagScale = { cameraState.zoom - cameraState.zoom.toIntFloor() }
    val drawReference = { cameraState.tilePoint.toCanvasDrawReference() }
    val drawTileSize = { mapProperties.tileSize }
    val drawRotationDegrees = { cameraState.angleDegrees.toFloat() }
    val drawTranslation = { cameraState.canvasSize.asOffset() / 2F }

    private val zoomLevel
        get() = cameraState.zoom.toIntFloor()

    val canvasKernel = CanvasKernel(coroutineScope, this)

    private fun TilePoint.coerceInMap(): TilePoint {
        val x = if (mapProperties.boundMap.horizontal == MapBorderType.BOUND)
            x.coerceIn(0.0, mapProperties.tileSize.width.toPx().toDouble())
        else
            x.loopInRange(mapProperties.tileSize.width.toPx().toDouble())
        val y = if (mapProperties.boundMap.vertical == MapBorderType.BOUND)
            y.coerceIn(0.0, mapProperties.tileSize.height.toPx().toDouble())
        else
            y.loopInRange(mapProperties.tileSize.height.toPx().toDouble())
        return TilePoint(x, y)
    }

    private fun Float.coerceZoom(): Float = this.coerceIn(zoomLevelPreference.min.toFloat(), zoomLevelPreference.max.toFloat())

    fun centerPointAtOffset(tilePoint: TilePoint, offset: ScreenOffset) {
        setPosition(cameraState.tilePoint + tilePoint - offset.toTilePoint())
    }

    fun setCanvasSize(offset: Offset) {
        if (offset.asScreenOffset() == cameraState.canvasSize)
            return
        cameraState = cameraState.copy(canvasSize = offset.asScreenOffset())
    }

    fun setZoom(zoom: Float) {
        cameraState = cameraState.copy(zoom = zoom.coerceZoom())
    }

    fun setAngle(angle: Degrees) {
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
                    "density" to mapState.density,
                    "fontScale" to mapState.fontScale,
                    "canvasSize" to Pair(mapState.cameraState.canvasSize.x, mapState.cameraState.canvasSize.y),
                    "zoom" to mapState.cameraState.zoom,
                    "angleDegrees" to mapState.cameraState.angleDegrees.value,
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
                    density = Density(map["density"] as Float, map["fontScale"] as Float),
                    initialCameraState = CameraState(
                        canvasSize = (map["canvasSize"] as Pair<*, *>).let { ScreenOffset(it.first as Float, it.second as Float) },
                        zoom = map["zoom"] as Float,
                        angleDegrees = Degrees(map["angleDegrees"] as Double),
                        coordinates = (map["coordinates"] as Pair<*, *>).let { Coordinates(it.first as Double, it.second as Double) },
                        tilePoint = (map["tilePoint"] as Pair<*, *>).let { TilePoint(it.first as Double, it.second as Double) },
                    ),
                    coroutineScope = coroutineScope
                )
            }
        )
    }
}
