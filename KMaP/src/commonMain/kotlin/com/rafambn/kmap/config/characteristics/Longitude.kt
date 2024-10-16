package com.rafambn.kmap.config.characteristics

import kotlin.math.abs

class Longitude(val east: Double, val west: Double, override val orientation: Int) : CoordinatesInterface {
    override val span = abs(east - west)

    override fun getMax(): Double = maxOf(east, west)

    override fun getMin(): Double = minOf(east, west)

    override operator fun contains(value: Double): Boolean = value in west..east
}