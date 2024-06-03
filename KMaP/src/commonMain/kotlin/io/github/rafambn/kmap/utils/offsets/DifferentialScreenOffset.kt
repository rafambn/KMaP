package io.github.rafambn.kmap.utils.offsets

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import io.github.rafambn.kmap.config.MapSource
import io.github.rafambn.kmap.utils.Degrees
import io.github.rafambn.kmap.utils.rotate
import io.github.rafambn.kmap.utils.toRadians

typealias DifferentialScreenOffset = Offset

fun DifferentialScreenOffset.toCanvasPositionFromScreenCenter(
    canvasSize: Offset,
    magnifierScale: Float,
    zoomLevel: Int,
    angle: Degrees,
    density: Density,
    mapSource: MapSource
): CanvasPosition {
    return (canvasSize / 2F - this).toCanvasPosition(
        magnifierScale,
        zoomLevel,
        angle,
        density,
        mapSource
    )
}

fun DifferentialScreenOffset.toCanvasPosition(
    magnifierScale: Float,
    zoomLevel: Int,
    angle: Degrees,
    density: Density,
    mapSource: MapSource
): CanvasPosition = (this.toPosition() / density.density.toDouble())
    .scaleToZoom(1 / (mapSource.tileSize * magnifierScale * (1 shl zoomLevel)))
    .rotate(-angle.toRadians())
    .scaleToMap(mapSource.mapCoordinatesRange.longitute.span, mapSource.mapCoordinatesRange.latitude.span)
    .applyOrientation(mapSource.mapCoordinatesRange)