package com.rafambn.kmap.utils

import com.rafambn.kmap.mapProperties.CardinalRange
import kotlin.math.floor

fun Double.loopInRange(coordinatesRange: CardinalRange): Double =
    (this - coordinatesRange.min).mod(coordinatesRange.span) + coordinatesRange.min

fun Double.loopInRange(tileConstraints: Double): Double =
    this.mod(tileConstraints)

fun Int.loopInZoom(zoomLevel: Int): Int = this.mod(1 shl zoomLevel)

fun lerp(start: Double, end: Double, value: Double): Double = start + (end - start) * value

inline fun <reified T : Reference> lerp(start: T, end: T, value: Double): T {
    val newX = lerp(start.x, end.x, value)
    val newY = lerp(start.y, end.y, value)

    return when (T::class) {
        ScreenOffset::class -> ScreenOffset(newX, newY) as T
        TilePoint::class -> TilePoint(newX, newY) as T
        Coordinates::class -> Coordinates(newX, newY) as T
        ProjectedCoordinates::class -> ProjectedCoordinates(newX, newY) as T
        DifferentialScreenOffset::class -> DifferentialScreenOffset(newX, newY) as T
        CanvasDrawReference::class -> CanvasDrawReference(newX, newY) as T
        else -> throw IllegalArgumentException("Unsupported type for lerp: ${T::class.simpleName}")
    }
}

fun Float.toIntFloor(): Int = floor(this).toInt()

fun Double.toIntFloor(): Int = floor(this).toInt()
