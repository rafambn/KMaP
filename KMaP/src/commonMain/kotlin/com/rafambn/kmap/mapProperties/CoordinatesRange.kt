package com.rafambn.kmap.mapProperties

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

interface CoordinatesRange {
    val latitude: Latitude
    val longitude: Longitude
}

interface CardinalRange {
    val start: Double
    val end: Double

    val span: Double get() = abs(start - end)
    val max: Double get() = max(start, end)
    val min: Double get() = min(start, end)
    val mean: Double get() = (start + end) / 2
    val orientation: Int get() = end.compareTo(start)
    operator fun contains(value: Double): Boolean = value in min..max
}

class Longitude(val east: Double, val west: Double) : CardinalRange {
    override val start: Double get() = west
    override val end: Double get() = east
}

class Latitude(val north: Double, val south: Double) : CardinalRange {
    override val start: Double get() = north
    override val end: Double get() = south
}
