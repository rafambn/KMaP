package io.github.rafambn.kmap.utils

import io.github.rafambn.kmap.config.characteristics.CoordinatesInterface
import io.github.rafambn.kmap.utils.offsets.CanvasPosition

fun Double.loopInRange(coordinatesRange: CoordinatesInterface): Double =
    (this - coordinatesRange.getMin()).mod(coordinatesRange.span) + coordinatesRange.getMin()

fun Int.loopInZoom(zoomLevel: Int): Int = this.mod(1 shl zoomLevel)

fun lerp(start: Double, end: Double, value: Double): Double = start + (end - start) * value

fun lerp(start: CanvasPosition, end: CanvasPosition, value: Double): CanvasPosition =
    CanvasPosition(lerp(start.horizontal, end.horizontal, value), lerp(start.vertical, end.vertical, value))
