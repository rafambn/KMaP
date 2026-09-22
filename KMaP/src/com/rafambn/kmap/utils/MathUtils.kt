package com.rafambn.kmap.utils

import com.rafambn.kmap.mapProperties.coordinates.CardinalRange
import kotlin.math.floor

fun Double.loopInRange(coordinatesRange: CardinalRange): Double =
    (this - coordinatesRange.min).mod(coordinatesRange.span) + coordinatesRange.min

fun Double.loopInRange(tileConstraints: Double): Double =
    this.mod(tileConstraints)

fun Int.loopInZoom(zoomLevel: Int): Int {
    require(zoomLevel in 0..30) { "Supported zoom levels are 0..30" }
    return mod(1 shl zoomLevel)
}

fun lerp(start: Double, end: Double, value: Double): Double = start + (end - start) * value

fun Float.toIntFloor(): Int = floor(this).toInt()

fun Double.toIntFloor(): Int = floor(this).toInt()
