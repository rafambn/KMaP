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
typealias CanvasPosition = Position
typealias ProjectedCoordinates = Position

fun Offset.toPosition(): Position = Position(this.x.toDouble(), this.y.toDouble())


fun CanvasPosition.toCanvasDrawReference(zoomLevel: Int, mapCoordinatesRange: MapCoordinatesRange, tileSize: Int): ScreenOffset =
    this.applyOrientation(mapCoordinatesRange)
        .moveToTrueCoordinates(mapCoordinatesRange)
        .scaleToZoom((tileSize * (1 shl zoomLevel)).toFloat())
        .scaleToMap(1 / mapCoordinatesRange.longitute.span, 1 / mapCoordinatesRange.latitude.span).toOffset()

fun ProjectedCoordinates.toCanvasPosition(): CanvasPosition = CanvasPosition( //TODO move this to mapProperties
    this.horizontal,
    ln(tan(PI / 4 + (PI * this.vertical) / 360)) / (PI / 85.051129)
)

fun CanvasPosition.toScreenOffset(
    mapPosition: CanvasPosition,
    canvasSize: Offset,
    magnifierScale: Float,
    zoomLevel: Int,
    angle: Degrees,
    mapCoordinatesRange: MapCoordinatesRange,
    density: Density,
    tileSize: Int
): ScreenOffset = -(this - mapPosition)
    .applyOrientation(mapCoordinatesRange)
    .scaleToMap(1 / mapCoordinatesRange.longitute.span, 1 / mapCoordinatesRange.latitude.span)
    .rotate(angle.toRadians())
    .scaleToZoom(tileSize * magnifierScale * (1 shl zoomLevel))
    .times(density.density.toDouble()).toOffset()
    .minus(canvasSize / 2F)

fun ScreenOffset.toCanvasPosition(
    mapPosition: CanvasPosition,
    canvasSize: Offset,
    magnifierScale: Float,
    zoomLevel: Int,
    angle: Degrees,
    mapCoordinatesRange: MapCoordinatesRange,
    density: Density,
    tileSize: Int
): CanvasPosition {
    return this.toCanvasPositionFromScreenCenter(
        canvasSize,
        magnifierScale,
        zoomLevel,
        angle,
        mapCoordinatesRange,
        density,
        tileSize
    ) + mapPosition
}

fun DifferentialScreenOffset.toCanvasPositionFromScreenCenter(
    canvasSize: Offset,
    magnifierScale: Float,
    zoomLevel: Int,
    angle: Degrees,
    mapCoordinatesRange: MapCoordinatesRange,
    density: Density,
    tileSize: Int
): CanvasPosition {
    return (canvasSize / 2F - this).toCanvasPosition(
        magnifierScale,
        zoomLevel,
        angle,
        mapCoordinatesRange,
        density,
        tileSize
    )
}

fun DifferentialScreenOffset.toCanvasPosition(
    magnifierScale: Float,
    zoomLevel: Int,
    angle: Degrees,
    mapCoordinatesRange: MapCoordinatesRange,
    density: Density,
    tileSize: Int
): CanvasPosition = (this.toPosition() / density.density.toDouble())
    .scaleToZoom(1 / (tileSize * magnifierScale * (1 shl zoomLevel)))
    .rotate(-angle.toRadians())
    .scaleToMap(mapCoordinatesRange.longitute.span, mapCoordinatesRange.latitude.span)
    .applyOrientation(mapCoordinatesRange)


//Transformation functions
fun Position.scaleToZoom(zoomScale: Float): Position {
    return Position(horizontal * zoomScale, vertical * zoomScale)
}

fun Position.moveToTrueCoordinates(mapCoordinatesRange: MapCoordinatesRange): Position {
    return Position(horizontal - mapCoordinatesRange.longitute.span / 2, vertical - mapCoordinatesRange.latitude.span / 2)
}

fun Position.scaleToMap(horizontal: Double, vertical: Double): Position {
    return Position(this.horizontal * horizontal, this.vertical * vertical)
}

fun Position.applyOrientation(mapCoordinatesRange: MapCoordinatesRange): Position {
    return Position(horizontal * mapCoordinatesRange.longitute.orientation, vertical * mapCoordinatesRange.latitude.orientation)
}
//Other Functions

fun Double.loopInRange(coordinatesRange: CoordinatesInterface): Double {
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
fun lerp(start: Position, end: Position, value: Double): Position {
    return Position(lerp(start.horizontal, end.horizontal, value), lerp(start.vertical, end.vertical, value))
}