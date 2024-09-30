package com.rafambn.kmapdemo.config.characteristics

import kotlin.math.abs

class Latitude(val north: Double, val south: Double, override val orientation: Int) : CoordinatesInterface {
    override val span = abs(north - south)

    override fun getMax(): Double = maxOf(north, south)

    override fun getMin(): Double = minOf(north, south)

    override operator fun contains(value: Double): Boolean = value in south..north
}