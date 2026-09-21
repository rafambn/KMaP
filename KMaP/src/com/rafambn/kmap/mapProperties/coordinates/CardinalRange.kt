package com.rafambn.kmap.mapProperties.coordinates

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

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
