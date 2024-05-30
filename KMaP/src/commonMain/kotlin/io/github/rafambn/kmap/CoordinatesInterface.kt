package io.github.rafambn.kmap

import kotlin.math.abs

interface MapCoordinatesRange {
    val latitude: Latitude
    val longitute: Longitude
}

interface CoordinatesInterface {
    val span: Float

    val orientation: Int

    fun getMax(): Float

    fun getMin(): Float

    operator fun contains(value: Float): Boolean
}

class Latitude(val north: Float, val south: Float, override val orientation: Int) : CoordinatesInterface {
    override val span = abs(north - south)

    override fun getMax(): Float = maxOf(north, south)

    override fun getMin(): Float = minOf(north, south)

    override operator fun contains(value: Float): Boolean = value in south..north
}

class Longitude(val east: Float, val west: Float, override val orientation: Int) : CoordinatesInterface {
    override val span = abs(east - west)

    override fun getMax(): Float = maxOf(east, west)

    override fun getMin(): Float = minOf(east, west)

    override operator fun contains(value: Float): Boolean = value in west..east
}

open class MapZoomlevelsRange(val max: Int, val min: Int) {
    operator fun contains(value: Int): Boolean = value in min..max
}
