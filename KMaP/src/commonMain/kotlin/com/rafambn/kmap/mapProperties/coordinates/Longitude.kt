package com.rafambn.kmap.mapProperties.coordinates

import com.rafambn.kmap.utils.getSign
import kotlin.math.abs

class Longitude(val east: Double, val west: Double) : CoordinatesInterface {
    override val span = abs(east - west)

    override fun getMax(): Double = maxOf(east, west)

    override fun getMin(): Double = minOf(east, west)

    override fun getOrientation(): Int = getSign(east - west)

    override operator fun contains(value: Double): Boolean = value in west..east
}