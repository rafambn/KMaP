package com.rafambn.kmap.mapProperties.coordinates

import com.rafambn.kmap.utils.getSign
import kotlin.math.abs

class Latitude(val north: Double, val south: Double) : CoordinatesInterface {
    override val span = abs(north - south)

    override fun getMax(): Double = maxOf(north, south)

    override fun getMin(): Double = minOf(north, south)

    override fun getOrientation(): Int = getSign(south - north)

    override operator fun contains(value: Double): Boolean = value in south..north
}