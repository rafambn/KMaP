package com.rafambn.kmap.utils

import com.rafambn.kmap.mapProperties.CardinalRange
import kotlin.math.floor

fun Double.loopInRange(coordinatesRange: CardinalRange): Double =
    (this - coordinatesRange.min).mod(coordinatesRange.span) + coordinatesRange.min

fun Int.loopInZoom(zoomLevel: Int): Int = this.mod(1 shl zoomLevel)

fun lerp(start: Double, end: Double, value: Double): Double = start + (end - start) * value

fun lerp(start: TilePoint, end: TilePoint, value: Double): TilePoint =
    TilePoint(lerp(start.horizontal, end.horizontal, value), lerp(start.vertical, end.vertical, value))

fun lerp(start: Coordinates, end: Coordinates, value: Double): Coordinates =
    Coordinates(lerp(start.longitude, end.longitude, value), lerp(start.latitude, end.latitude, value))

fun Float.toIntFloor(): Int = floor(this).toInt()

fun Double.toIntFloor(): Int = floor(this).toInt()