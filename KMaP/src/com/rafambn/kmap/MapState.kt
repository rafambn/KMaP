package com.rafambn.kmap

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import com.rafambn.kmap.camera.CameraState
import com.rafambn.kmap.camera.MotionController
import com.rafambn.kmap.geometry.angle.Degrees
import com.rafambn.kmap.geometry.angle.rotate
import com.rafambn.kmap.geometry.angle.toRadians
import com.rafambn.kmap.geometry.plane.*
import com.rafambn.kmap.mapProperties.MapProperties
import com.rafambn.kmap.mapProperties.ZoomLevelRange
import com.rafambn.kmap.mapProperties.border.MapBorderType
import com.rafambn.kmap.source.internal.CanvasKernel
import com.rafambn.kmap.utils.loopInRange
import com.rafambn.kmap.utils.toIntFloor
import kotlinx.coroutines.CoroutineScope
import kotlin.math.pow

@Composable
fun rememberMapState(
    mapProperties: MapProperties,
    zoomLevelPreference: ZoomLevelRange? = null,
    density: Density = LocalDensity.current,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
): MapState {
    val currentZoomLevelPreference = zoomLevelPreference ?: mapProperties.zoomLevels
    return rememberSaveable(
        saver = MapState.saver(
            mapProperties = mapProperties,
            zoomLevelPreference = currentZoomLevelPreference,
            density = density,
            coroutineScope = coroutineScope,
        ),
        init = {
            MapState(
                mapProperties = mapProperties,
                zoomLevelPreference = currentZoomLevelPreference,
                density = density,
                initialCameraState = null,
                coroutineScope = coroutineScope,
            )
        }
    )
}

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

    var zoomLevelPreference = validateZoomLevelPreference(zoomLevelPreference ?: mapProperties.zoomLevels)
        set(value) {
            field = validateZoomLevelPreference(value)
            updateCamera(zoom = cameraState.zoom)
        }

    var cameraState by mutableStateOf(
        initialCameraState ?: CameraState(
            tilePoint = TilePoint(
                mapProperties.tileSize.width.value / 2.0,
                mapProperties.tileSize.height.value / 2.0,
            ),
            zoom = this.zoomLevelPreference.min.toFloat(),
        )
    )
        private set

    init {
        validateZoom(cameraState.zoom)
    }

    val coordinates: Coordinates
        get() = cameraState.tilePoint.toCoordinates()

    internal fun resolveVisibleTiles() {
        if (viewportSize.width == 0 || viewportSize.height == 0) return

        val screenSize = viewportSize.asScreenOffset()
        val topLeft = ScreenOffset.Zero.toTilePoint()
        val topRight = ScreenOffset(screenSize.x, 0.0).toTilePoint()
        val bottomLeft = ScreenOffset(0.0, screenSize.y).toTilePoint()
        val bottomRight = screenSize.toTilePoint()
        canvasKernel.resolveVisibleTiles(
            TilePoint(
                minOf(topLeft.x, topRight.x, bottomLeft.x, bottomRight.x),
                minOf(topLeft.y, topRight.y, bottomLeft.y, bottomRight.y),
            ),
            TilePoint(
                maxOf(topLeft.x, topRight.x, bottomLeft.x, bottomRight.x),
                maxOf(topLeft.y, topRight.y, bottomLeft.y, bottomRight.y),
            ),
            cameraState.zoom.toIntFloor(),
            mapProperties,
        )
    }

    val drawMagScale = { cameraState.zoom - cameraState.zoom.toIntFloor() }
    val drawReference = { cameraState.tilePoint.toCanvasDrawReference() }
    val drawTileSize = { mapProperties.tileSize }
    val drawRotationDegrees = { cameraState.angleDegrees.toFloat() }

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

    private fun validateZoomLevelPreference(value: ZoomLevelRange): ZoomLevelRange {
        require(mapProperties.zoomLevels.min in 0..30 &&
            mapProperties.zoomLevels.max in mapProperties.zoomLevels.min..30
        ) { "Map zoom levels must be within 0..30" }
        require(value.min <= value.max) { "Minimum zoom level must not exceed maximum zoom level" }
        require(
            value.min >= mapProperties.zoomLevels.min &&
                value.max <= mapProperties.zoomLevels.max
        ) { "Zoom level preference must be within the map zoom levels" }
        return value
    }

    private fun validateZoom(zoom: Float) {
        require(
            zoom >= zoomLevelPreference.min &&
                zoom <= zoomLevelPreference.max
        ) { "Zoom must be within the zoom level preference" }
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

    /**
     * Places [tilePoint] at [centerOffset] with the requested zoom and angle in one update.
     * [centerOffset] is measured in screen pixels from the viewport center, positive right/down.
     * Map borders may prevent the requested placement. Zoom, angle and tile point default to the
     * current camera; the offset defaults to zero and is not stored in the camera state.
     */
    fun updateCamera(
        zoom: Float = cameraState.zoom,
        angle: Degrees = cameraState.angleDegrees,
        tilePoint: TilePoint = cameraState.tilePoint,
        centerOffset: DifferentialScreenOffset = DifferentialScreenOffset.Zero,
    ) {
        val newZoom = zoom.coerceZoom()
        validateZoom(newZoom)
        val inverseScale = 2.0.pow(-newZoom.toDouble()) / density
        val tileOffset = TilePoint(centerOffset.x * inverseScale, centerOffset.y * inverseScale)
            .rotate(-angle.toRadians())
        val updatedCameraState = CameraState(
            zoom = newZoom,
            angleDegrees = angle,
            tilePoint = (tilePoint - tileOffset).coerceInMap(),
        )
        if (updatedCameraState == cameraState) return

        cameraState = updatedCameraState
        resolveVisibleTiles()
    }

    companion object {
        fun saver(
            mapProperties: MapProperties,
            zoomLevelPreference: ZoomLevelRange,
            density: Density,
            coroutineScope: CoroutineScope,
        ) = mapSaver(
            save = { mapState ->
                mapOf(
                    "zoom" to mapState.cameraState.zoom,
                    "angleDegrees" to mapState.cameraState.angleDegrees.value,
                    "tilePoint" to Pair(
                        mapState.cameraState.tilePoint.x,
                        mapState.cameraState.tilePoint.y,
                    ),
                )
            },
            restore = { map ->
                val legacyDensity = (map["density"] as? Float)?.toDouble() ?: 1.0
                val tilePoint = (map["tilePoint"] as Pair<*, *>).let {
                    TilePoint(
                        (it.first as Double) / legacyDensity,
                        (it.second as Double) / legacyDensity,
                    )
                }
                MapState(
                    mapProperties = mapProperties,
                    zoomLevelPreference = zoomLevelPreference,
                    density = density,
                    coroutineScope = coroutineScope,
                ).apply {
                    updateCamera(
                        zoom = map["zoom"] as Float,
                        angle = Degrees(map["angleDegrees"] as Double),
                        tilePoint = tilePoint,
                    )
                }
            }
        )
    }
}
