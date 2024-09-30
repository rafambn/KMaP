package com.rafambn.kmapdemo.utils

import com.rafambn.kmapdemo.config.characteristics.CoordinatesInterface
import com.rafambn.kmapdemo.utils.offsets.CanvasPosition
import com.rafambn.kmapdemo.utils.offsets.ProjectedCoordinates
import kotlin.math.floor

fun Double.loopInRange(coordinatesRange: CoordinatesInterface): Double =
    (this - coordinatesRange.getMin()).mod(coordinatesRange.span) + coordinatesRange.getMin()

fun Int.loopInZoom(zoomLevel: Int): Int = this.mod(1 shl zoomLevel)

fun lerp(start: Double, end: Double, value: Double): Double = start + (end - start) * value

fun lerp(start: CanvasPosition, end: CanvasPosition, value: Double): CanvasPosition =
    CanvasPosition(lerp(start.horizontal, end.horizontal, value), lerp(start.vertical, end.vertical, value))

fun lerp(start: ProjectedCoordinates, end: ProjectedCoordinates, value: Double): ProjectedCoordinates =
    ProjectedCoordinates(lerp(start.horizontal, end.horizontal, value), lerp(start.vertical, end.vertical, value))

fun Float.toIntFloor(): Int = floor(this).toInt()

fun Double.toIntFloor(): Int = floor(this).toInt()