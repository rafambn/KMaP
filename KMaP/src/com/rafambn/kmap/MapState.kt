package com.rafambn.kmap

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import com.rafambn.kmap.camera.CameraState
import com.rafambn.kmap.camera.InternalCameraState
import com.rafambn.kmap.camera.MotionController
import com.rafambn.kmap.components.ViewPort
import com.rafambn.kmap.geometry.angle.Degrees
import com.rafambn.kmap.geometry.plane.*
import com.rafambn.kmap.mapProperties.MapProperties
import com.rafambn.kmap.mapProperties.ZoomLevelRange
import com.rafambn.kmap.mapProperties.border.MapBorderType
import com.rafambn.kmap.source.internal.CanvasKernel
import com.rafambn.kmap.utils.loopInRange
import com.rafambn.kmap.utils.toIntFloor
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
    private var currentDensity by mutableStateOf(density)

    override val density
        get() = currentDensity.density

    override val fontScale
        get() = currentDensity.fontScale

    val motionController = MotionController(this)
    internal var viewportSize = IntSize.Zero
        private set

    var zoomLevelPreference = zoomLevelPreference ?: mapProperties.zoomLevels
        set(value) {
            if (value.max > mapProperties.zoomLevels.max || value.min < mapProperties.zoomLevels.min)
                throw IllegalArgumentException("Zoom level is out of bounds")
            field = value
        }

    internal var internalCameraState by mutableStateOf(
        initialCameraState?.let {
            InternalCameraState(
                tilePoint = it.coordinates.toTilePoint(),
                zoom = it.zoom,
                angleDegrees = it.angleDegrees,
            )
        } ?: InternalCameraState(
            tilePoint = TilePoint(
                mapProperties.tileSize.width.value / 2.0,
                mapProperties.tileSize.height.value / 2.0,
            )
        )
    )
        private set

    val cameraState: CameraState by derivedStateOf {
        CameraState(
            zoom = internalCameraState.zoom,
            angleDegrees = internalCameraState.angleDegrees,
            coordinates = internalCameraState.tilePoint.toCoordinates(),
        )
    }

    private operator fun MutableState<InternalCameraState>.setValue(
        thisObj: Any?,
        property: KProperty<*>,
        value: InternalCameraState,
    ) {
        this.value = value
        resolveVisibleTiles()
    }

    internal fun resolveVisibleTiles() {
        if (viewportSize.width == 0 || viewportSize.height == 0) return

        val screenSize = viewportSize.asScreenOffset()
        val topLeft = ScreenOffset.Zero.toTilePoint()
        val topRight = ScreenOffset(screenSize.x, 0.0).toTilePoint()
        val bottomLeft = ScreenOffset(0.0, screenSize.y).toTilePoint()
        val bottomRight = screenSize.toTilePoint()
        val viewPort = ViewPort(
            Rect(
                minOf(topLeft.x, topRight.x, bottomLeft.x, bottomRight.x).toFloat(),
                minOf(topLeft.y, topRight.y, bottomLeft.y, bottomRight.y).toFloat(),
                maxOf(topLeft.x, topRight.x, bottomLeft.x, bottomRight.x).toFloat(),
                maxOf(topLeft.y, topRight.y, bottomLeft.y, bottomRight.y).toFloat()
            )
        )
        canvasKernel.resolveVisibleTiles(viewPort, internalCameraState.zoom.toIntFloor(), mapProperties)
    }

    val drawMagScale = { internalCameraState.zoom - internalCameraState.zoom.toIntFloor() }
    val drawReference = { internalCameraState.tilePoint.toCanvasDrawReference() }
    val drawTileSize = { mapProperties.tileSize }
    val drawRotationDegrees = { internalCameraState.angleDegrees.toFloat() }

    val canvasKernel = CanvasKernel(coroutineScope, this)

    private fun TilePoint.coerceInMap(): TilePoint {
        val x = if (mapProperties.boundMap.horizontal == MapBorderType.BOUND)
            x.coerceIn(0.0, mapProperties.tileSize.width.value.toDouble())
        else
            x.loopInRange(mapProperties.tileSize.width.value.toDouble())
        val y = if (mapProperties.boundMap.vertical == MapBorderType.BOUND)
            y.coerceIn(0.0, mapProperties.tileSize.height.value.toDouble())
        else
            y.loopInRange(mapProperties.tileSize.height.value.toDouble())
        return TilePoint(x, y)
    }

    private fun Float.coerceZoom(): Float =
        this.coerceIn(zoomLevelPreference.min.toFloat(), zoomLevelPreference.max.toFloat())

    fun centerPointAtOffset(tilePoint: TilePoint, offset: ScreenOffset) {
        setPosition(internalCameraState.tilePoint + tilePoint - offset.toTilePoint())
    }

    internal fun setViewportSize(size: IntSize) {
        if (size == viewportSize) return

        viewportSize = size
        resolveVisibleTiles()
    }

    internal fun updateDensity(newDensity: Density) {
        val densityChanged = density != newDensity.density
        if (!densityChanged && fontScale == newDensity.fontScale) return

        currentDensity = newDensity
        if (densityChanged) resolveVisibleTiles()
    }

    fun setZoom(zoom: Float) {
        internalCameraState = internalCameraState.copy(zoom = zoom.coerceZoom())
    }

    fun setAngle(angle: Degrees) {
        internalCameraState = internalCameraState.copy(angleDegrees = angle)
    }

    fun setPosition(position: TilePoint) {
        internalCameraState = internalCameraState.copy(tilePoint = position.coerceInMap())
    }

    companion object {
        fun saver(
            mapProperties: MapProperties,
            coroutineScope: CoroutineScope,
        ) = mapSaver(
            save = { mapState ->
                mapOf(
                    "zoomLevelPreference" to Pair(mapState.zoomLevelPreference.min, mapState.zoomLevelPreference.max),
                    "zoom" to mapState.internalCameraState.zoom,
                    "angleDegrees" to mapState.internalCameraState.angleDegrees.value,
                    "tilePoint" to Pair(
                        mapState.internalCameraState.tilePoint.x,
                        mapState.internalCameraState.tilePoint.y,
                    ),
                )
            },
            restore = { map ->
                val tilePoint = (map["tilePoint"] as Pair<*, *>).let {
                    TilePoint(
                        it.first as Double,
                        it.second as Double,
                    )
                }
                MapState(
                    mapProperties = mapProperties,
                    zoomLevelPreference = (map["zoomLevelPreference"] as Pair<*, *>).let {
                        object : ZoomLevelRange {
                            override val max: Int = it.second as Int
                            override val min: Int = it.first as Int
                        }
                    },
                    coroutineScope = coroutineScope
                ).apply {
                    internalCameraState = InternalCameraState(
                        zoom = map["zoom"] as Float,
                        angleDegrees = Degrees(map["angleDegrees"] as Double),
                        tilePoint = tilePoint,
                    )
                }
            }
        )
    }
}
