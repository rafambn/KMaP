package io.github.rafambn.kmap.utils

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import io.github.rafambn.kmap.Position
import io.github.rafambn.kmap.BoundingBox
import io.github.rafambn.kmap.CoordinatesInterface
import io.github.rafambn.kmap.MapCoordinatesRange
import io.github.rafambn.kmap.TileCanvasState

fun Offset.toPosition(): Position {
    return Position(this.x.toDouble(), this.y.toDouble())
}

fun Position.toOffset(): Offset {
    return Offset(this.horizontal.toFloat(), this.vertical.toFloat())
}

fun Position.toCanvasReference(zoomLevel: Int, mapCoordinatesRange: MapCoordinatesRange): Position {
    return this.applyOrientation(mapCoordinatesRange)
        .moveToTrueCoordinates(mapCoordinatesRange)
        .scaleToZoom((TileCanvasState.TILE_SIZE * (1 shl zoomLevel)).toDouble())
        .scaleToMap(1 / mapCoordinatesRange.longitute.span, 1 / mapCoordinatesRange.latitude.span)
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

fun Position.transform(
    magnifierScale: Float,
    zoomLevel: Int,
    angle: Degrees,
    mapCoordinatesRange: MapCoordinatesRange,
    invertFirst: Boolean = false,
    invertSecond: Boolean = false
): Position {
    val scale = (1 / (TileCanvasState.TILE_SIZE * magnifierScale * (1 shl (zoomLevel + 1)))).toDouble()
    var position = this
    if (invertFirst) position = position.invertFisrt()
    if (invertSecond) position = position.invertSecond()
    return position
        .scaleToZoom(scale)
        .applyOrientation(mapCoordinatesRange)
        .rotate(angle.toRadians())
        .scaleToMap(mapCoordinatesRange.longitute.span, mapCoordinatesRange.latitude.span)
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