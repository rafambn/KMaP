package io.github.rafambn.kmap.utils

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import io.github.rafambn.kmap.Position
import io.github.rafambn.kmap.BoundingBox
import io.github.rafambn.kmap.CoordinatesInterface
import io.github.rafambn.kmap.MapCoordinatesRange
import io.github.rafambn.kmap.TileCanvasState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.exp

fun Offset.toPosition(): Position {
    return Position(this.x.toDouble(), this.y.toDouble())
}

fun Position.toOffset(): Offset {
    return Offset(this.horizontal.toFloat(), this.vertical.toFloat())
}

fun Offset.differentialOffsetToMapReference(
    magnifierScale: Float,
    zoomLevel: Int,
    angle: Degrees,
    mapCoordinatesRange: MapCoordinatesRange,
    density: Density
): Position {
    return this.toPosition().toMapReference(
        magnifierScale,
        zoomLevel,
        angle,
        mapCoordinatesRange,
        density
    )
}

fun Offset.differentialOffsetFromScreenCenterToMapReference(
    canvasSize: Offset,
    magnifierScale: Float,
    zoomLevel: Int,
    angle: Degrees,
    mapCoordinatesRange: MapCoordinatesRange,
    density: Density
): Position {
    return (canvasSize / 2F - this).toPosition().toMapReference(
        magnifierScale,
        zoomLevel,
        angle,
        mapCoordinatesRange,
        density
    )
}

fun Offset.offsetToMapReference(
    mapPosition: Position,
    canvasSize: Offset,
    magnifierScale: Float,
    zoomLevel: Int,
    angle: Degrees,
    mapCoordinatesRange: MapCoordinatesRange,
    density: Density
): Position {
    return this.differentialOffsetFromScreenCenterToMapReference(
        canvasSize,
        magnifierScale,
        zoomLevel,
        angle,
        mapCoordinatesRange,
        density
    ) + mapPosition
}

fun Position.toMapReference(
    magnifierScale: Float,
    zoomLevel: Int,
    angle: Degrees,
    mapCoordinatesRange: MapCoordinatesRange,
    density: Density
): Position {
    return (this / density.density.toDouble())
        .scaleToZoom((1 / (TileCanvasState.TILE_SIZE * magnifierScale * (1 shl zoomLevel))).toDouble())
        .rotate(-angle.toRadians())
        .scaleToMap(mapCoordinatesRange.longitute.span, mapCoordinatesRange.latitude.span)
        .applyOrientation(mapCoordinatesRange)
}

fun Position.toCanvasDrawReference(zoomLevel: Int, mapCoordinatesRange: MapCoordinatesRange): Position {
    return this.applyOrientation(mapCoordinatesRange)
        .moveToTrueCoordinates(mapCoordinatesRange)
        .scaleToZoom((TileCanvasState.TILE_SIZE * (1 shl zoomLevel)).toDouble())
        .scaleToMap(1 / mapCoordinatesRange.longitute.span, 1 / mapCoordinatesRange.latitude.span)
}

fun Position.positionToCanvasOffset(
    mapPosition: Position,
    magnifierScale: Float,
    zoomLevel: Int,
    mapCoordinatesRange: MapCoordinatesRange,
    angle: Degrees,
    density: Density,
    canvasSize: Offset
): Offset {
    return -(this - mapPosition)
        .applyOrientation(mapCoordinatesRange)
        .scaleToMap(1 / mapCoordinatesRange.longitute.span, 1 / mapCoordinatesRange.latitude.span)
        .rotate(angle.toRadians())
        .scaleToZoom((TileCanvasState.TILE_SIZE * magnifierScale * (1 shl zoomLevel)).toDouble())
        .times(density.density.toDouble())
        .minus(Position(canvasSize.x / 2.0, canvasSize.y / 2.0))
        .toOffset()
}

fun Position.transform(
    magnifierScale: Float,
    zoomLevel: Int,
    angle: Degrees,
    mapCoordinatesRange: MapCoordinatesRange,
    invertFirst: Boolean = false,
    invertSecond: Boolean = false
): Position {
    var position = this
    if (invertFirst) position = position.invertFisrt()
    if (invertSecond) position = position.invertSecond()
    return position
        .scaleToZoom((1 / (TileCanvasState.TILE_SIZE * magnifierScale * (1 shl (zoomLevel + 1)))).toDouble())
        .applyOrientation(mapCoordinatesRange)
        .rotate(angle.toRadians())
        .scaleToMap(mapCoordinatesRange.longitute.span, mapCoordinatesRange.latitude.span)
}

fun Position.toViewportReference(
    magnifierScale: Float,
    zoomLevel: Int,
    angle: Degrees,
    mapCoordinatesRange: MapCoordinatesRange,
    mapPosition: Position
): BoundingBox {
    return BoundingBox(
        mapPosition + this.transform(magnifierScale, zoomLevel, angle, mapCoordinatesRange),
        mapPosition + this.transform(magnifierScale, zoomLevel, angle, mapCoordinatesRange, invertFirst = true),
        mapPosition + this.transform(magnifierScale, zoomLevel, angle, mapCoordinatesRange, invertFirst = true, invertSecond = true),
        mapPosition + this.transform(magnifierScale, zoomLevel, angle, mapCoordinatesRange, invertSecond = true)
    )
}

fun Position.invertFisrt(): Position {
    return Position(-horizontal, vertical)
}

fun Position.invertSecond(): Position {
    return Position(horizontal, -vertical)
}

fun Position.scaleToZoom(zoomScale: Double): Position {
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

fun Double.loopInRange(coordinatesRange: CoordinatesInterface): Double {
    return (this - coordinatesRange.getMin()).mod(coordinatesRange.span) + coordinatesRange.getMin()
}

fun Int.loopInZoom(zoomLevel: Int): Int {
    return this.mod(1 shl zoomLevel)
}

fun lerp(start: Double, end: Double, value: Double): Double {
    return start + (end - start) * value
}

fun lerp(start: Position, end: Position, value: Double): Position {
    return Position(lerp(start.horizontal, end.horizontal, value), lerp(start.vertical, end.vertical, value))
}

fun decayValue(coroutineScope: CoroutineScope, decayRate: Double, function: (value: Double) -> Unit) =
    coroutineScope.launch {
        val steps = 200
        val timeStep = 5L
        for (i in 0 until steps) {
            val x = i.toDouble() / steps
            function((1 - exp(decayRate * (x - 1.0))) / (1 - exp(-decayRate)))
            delay(timeStep)
        }
    }