package io.github.rafambn.kmap.utils

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import io.github.rafambn.kmap.CoordinatesInterface
import io.github.rafambn.kmap.MapCoordinatesRange
import io.github.rafambn.kmap.TileCanvasState
import kotlin.math.PI
import kotlin.math.ln
import kotlin.math.tan

typealias ScreenOffset = Offset
typealias DifferentialScreenOffset = Offset
typealias CanvasPosition = Offset
typealias ProjectedCoordinates = Offset //TODO Go back to using double due to precision problems

fun ProjectedCoordinates.toCanvasPosition(): CanvasPosition{
    return Offset(
        this.x,
        (ln(tan(PI / 4 + (PI * this.y) / 360)) / (PI/85.051129)).toFloat()
    )
}

fun CanvasPosition.toCanvasReference(zoomLevel: Int, mapCoordinatesRange: MapCoordinatesRange): ScreenOffset {
    return this.applyOrientation(mapCoordinatesRange)
        .moveToTrueCoordinates(mapCoordinatesRange)
        .scaleToZoom((TileCanvasState.TILE_SIZE * (1 shl zoomLevel)).toFloat())
        .scaleToMap(1 / mapCoordinatesRange.longitute.span, 1 / mapCoordinatesRange.latitude.span)
}

fun CanvasPosition.toScreenOffset(
    mapPosition: CanvasPosition,
    canvasSize: Offset,
    magnifierScale: Float,
    zoomLevel: Int,
    angle: Degrees,
    mapCoordinatesRange: MapCoordinatesRange,
    density: Density
): ScreenOffset {
    return -(this - mapPosition)
        .applyOrientation(mapCoordinatesRange)
        .scaleToMap(1 / mapCoordinatesRange.longitute.span, 1 / mapCoordinatesRange.latitude.span)
        .rotate(angle.toRadians())
        .scaleToZoom(TileCanvasState.TILE_SIZE * magnifierScale * (1 shl zoomLevel))
        .times(density.density)
        .minus(Offset(canvasSize.x / 2F, canvasSize.y / 2F))
}

fun DifferentialScreenOffset.toCanvasPosition(
    magnifierScale: Float,
    zoomLevel: Int,
    angle: Degrees,
    mapCoordinatesRange: MapCoordinatesRange,
    density: Density
): CanvasPosition {
    return (this / density.density)
        .scaleToZoom(1 / (TileCanvasState.TILE_SIZE * magnifierScale * (1 shl zoomLevel)))
        .rotate(-angle.toRadians())
        .scaleToMap(mapCoordinatesRange.longitute.span, mapCoordinatesRange.latitude.span)
        .applyOrientation(mapCoordinatesRange)
}

fun ScreenOffset.toCanvasPosition(
    mapPosition: CanvasPosition,
    canvasSize: Offset,
    magnifierScale: Float,
    zoomLevel: Int,
    angle: Degrees,
    mapCoordinatesRange: MapCoordinatesRange,
    density: Density
): CanvasPosition {
    return this.toCanvasPositionFromScreenCenter(
        canvasSize,
        magnifierScale,
        zoomLevel,
        angle,
        mapCoordinatesRange,
        density
    ) + mapPosition
}

fun DifferentialScreenOffset.toCanvasPositionFromScreenCenter(
    canvasSize: Offset,
    magnifierScale: Float,
    zoomLevel: Int,
    angle: Degrees,
    mapCoordinatesRange: MapCoordinatesRange,
    density: Density
): CanvasPosition {
    return (canvasSize / 2F - this).toCanvasPosition(
        magnifierScale,
        zoomLevel,
        angle,
        mapCoordinatesRange,
        density
    )
}

//Transformation functions
fun Offset.scaleToZoom(zoomScale: Float): Offset {
    return Offset(x * zoomScale, y * zoomScale)
}

fun Offset.moveToTrueCoordinates(mapCoordinatesRange: MapCoordinatesRange): Offset {
    return Offset(x - mapCoordinatesRange.longitute.span / 2, y - mapCoordinatesRange.latitude.span / 2)
}

fun Offset.scaleToMap(horizontal: Float, vertical: Float): Offset {
    return Offset(this.x * horizontal, this.y * vertical)
}

fun Offset.applyOrientation(mapCoordinatesRange: MapCoordinatesRange): Offset {
    return Offset(x * mapCoordinatesRange.longitute.orientation, y * mapCoordinatesRange.latitude.orientation)
}
//Other Functions

fun Float.loopInRange(coordinatesRange: CoordinatesInterface): Float {
    return (this - coordinatesRange.getMin()).mod(coordinatesRange.span) + coordinatesRange.getMin()
}

fun Int.loopInZoom(zoomLevel: Int): Int {
    return this.mod(1 shl zoomLevel)
}

fun lerp(start: Float, end: Float, value: Double): Float {
    return start + (end - start) * value.toFloat()
}

fun lerp(start: Double, end: Double, value: Double): Double {
    return start + (end - start) * value
}

fun lerp(start: Offset, end: Offset, value: Double): Offset {
    return Offset(lerp(start.x, end.x, value), lerp(start.y, end.y, value))
}