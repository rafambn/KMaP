package io.github.rafambn.kmap.utils.offsets

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import io.github.rafambn.kmap.config.MapSource
import io.github.rafambn.kmap.utils.Degrees

typealias ScreenOffset = Offset

fun ScreenOffset.toPosition(): CanvasPosition = CanvasPosition(x.toDouble(), y.toDouble())

fun ScreenOffset.toCanvasPosition(
    mapPosition: CanvasPosition,
    canvasSize: Offset,
    magnifierScale: Float,
    zoomLevel: Int,
    angle: Degrees,
    density: Density,
    mapSource: MapSource
): CanvasPosition {
    return this.toCanvasPositionFromScreenCenter(
        canvasSize,
        magnifierScale,
        zoomLevel,
        angle,
        density,
        mapSource
    ) + mapPosition
}