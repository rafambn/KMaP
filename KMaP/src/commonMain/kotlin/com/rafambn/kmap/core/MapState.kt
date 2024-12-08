package com.rafambn.kmap.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import com.rafambn.kmap.mapProperties.MapProperties
import com.rafambn.kmap.mapProperties.border.MapBorderType
import com.rafambn.kmap.mapProperties.coordinates.MapCoordinatesRange
import com.rafambn.kmap.utils.CanvasDrawReference
import com.rafambn.kmap.utils.CanvasPosition
import com.rafambn.kmap.utils.DifferentialScreenOffset
import com.rafambn.kmap.utils.ProjectedCoordinates
import com.rafambn.kmap.utils.ScreenOffset
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
    var cameraState by mutableStateOf(CameraState())

    //Derivative variables
    var projection: ProjectedCoordinates
        get() {
            return cameraState.rawPosition.toProjectedCoordinates()
        }
        set(value) {
            setRawPosition(value.toCanvasPosition())
        }
    val drawReference
        get() = cameraState.rawPosition.toCanvasDrawReference()
    private val zoomLevel
        get() = cameraState.zoom.toIntFloor()
    private val magnifierScale
        get() = cameraState.zoom - zoomLevel + 1F

    //Utility functions
    fun getBoundingBox(): BoundingBox {
        return BoundingBox(
            ScreenOffset.Zero.toCanvasPosition(),
            ScreenOffset(cameraState.canvasSize.x, 0F).toCanvasPosition(),
            cameraState.canvasSize.toCanvasPosition(),
            ScreenOffset(0F, cameraState.canvasSize.y).toCanvasPosition(),
        )
    }

    private fun CanvasPosition.coerceInMap(): CanvasPosition {
        val x = if (mapProperties.boundMap.horizontal == MapBorderType.BOUND)
            horizontal.coerceIn(
                mapProperties.mapCoordinatesRange.longitude.west * mapProperties.mapCoordinatesRange.longitude.getOrientation(),
                mapProperties.mapCoordinatesRange.longitude.east * mapProperties.mapCoordinatesRange.longitude.getOrientation()
            )
        else
            horizontal.loopInRange(mapProperties.mapCoordinatesRange.longitude)
        val y = if (mapProperties.boundMap.vertical == MapBorderType.BOUND)
            vertical.coerceIn(
                mapProperties.mapCoordinatesRange.latitude.north * mapProperties.mapCoordinatesRange.latitude.getOrientation(),
                mapProperties.mapCoordinatesRange.latitude.south * mapProperties.mapCoordinatesRange.latitude.getOrientation()
            )
        else
            vertical.loopInRange(mapProperties.mapCoordinatesRange.latitude)
        return CanvasPosition(x, y)
    }

    private fun Float.coerceZoom(): Float {
        return this.coerceIn(minZoomPreference.toFloat(), maxZoomPreference.toFloat())
    }

    fun centerPositionAtOffset(position: CanvasPosition, offset: ScreenOffset) {
        setRawPosition(cameraState.rawPosition + position - offset.toCanvasPosition())
    }

    //Conversion Functions
    fun ScreenOffset.toCanvasPosition(): CanvasPosition =
        (cameraState.canvasSize / 2F - this).asDifferentialScreenOffset().toCanvasPosition() + cameraState.rawPosition

    fun DifferentialScreenOffset.toCanvasPosition(): CanvasPosition =
        (this.asCanvasPosition() / density.density.toDouble())
            .scaleToZoom(1 / (mapProperties.tileSize * magnifierScale * (1 shl zoomLevel)))
            .rotate(-cameraState.angleDegrees.toRadians())
            .scaleToMap(
                mapProperties.mapCoordinatesRange.longitude.span,
                mapProperties.mapCoordinatesRange.latitude.span
            )
            .applyOrientation(mapProperties.mapCoordinatesRange)

    fun CanvasPosition.toScreenOffset(): ScreenOffset = -(this - cameraState.rawPosition)
        .applyOrientation(mapProperties.mapCoordinatesRange)
        .scaleToMap(
            1 / mapProperties.mapCoordinatesRange.longitude.span,
            1 / mapProperties.mapCoordinatesRange.latitude.span
        )
        .rotate(cameraState.angleDegrees.toRadians())
        .scaleToZoom(mapProperties.tileSize * magnifierScale * (1 shl zoomLevel))
        .times(density.density.toDouble()).asScreenOffset()
        .minus(cameraState.canvasSize / 2F)

    private fun CanvasPosition.toCanvasDrawReference(): CanvasDrawReference {
        val canvasDrawReference = this.applyOrientation(mapProperties.mapCoordinatesRange)
            .moveToTrueCoordinates(mapProperties.mapCoordinatesRange)
            .scaleToZoom((mapProperties.tileSize * (1 shl zoomLevel)).toFloat())
            .scaleToMap(
                1 / mapProperties.mapCoordinatesRange.longitude.span,
                1 / mapProperties.mapCoordinatesRange.latitude.span
            )
        return CanvasDrawReference(canvasDrawReference.horizontal, canvasDrawReference.vertical)
    }

    //Transformation functions
    private fun CanvasPosition.scaleToZoom(zoomScale: Float): CanvasPosition {
        return CanvasPosition(horizontal * zoomScale, vertical * zoomScale)
    }

    private fun CanvasPosition.moveToTrueCoordinates(mapCoordinatesRange: MapCoordinatesRange): CanvasPosition {
        return CanvasPosition(
            horizontal - mapCoordinatesRange.longitude.span / 2,
            vertical - mapCoordinatesRange.latitude.span / 2
        )
    }

    private fun CanvasPosition.scaleToMap(horizontal: Double, vertical: Double): CanvasPosition {
        return CanvasPosition(this.horizontal * horizontal, this.vertical * vertical)
    }

    private fun CanvasPosition.applyOrientation(mapCoordinatesRange: MapCoordinatesRange): CanvasPosition {
        return CanvasPosition(
            horizontal * mapCoordinatesRange.longitude.getOrientation(),
            vertical * mapCoordinatesRange.latitude.getOrientation()
        )
    }

    fun ProjectedCoordinates.toCanvasPosition(): CanvasPosition = mapProperties.toCanvasPosition(this@toCanvasPosition)

    fun CanvasPosition.toProjectedCoordinates(): ProjectedCoordinates = mapProperties.toProjectedCoordinates(this@toProjectedCoordinates)

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

    fun setRawPosition(position: CanvasPosition) {
        cameraState = cameraState.copy(rawPosition = position.coerceInMap())
    }
}