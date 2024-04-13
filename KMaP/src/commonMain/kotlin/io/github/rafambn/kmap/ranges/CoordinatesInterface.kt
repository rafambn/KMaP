package io.github.rafambn.kmap.ranges

import androidx.compose.foundation.gestures.Orientation
import kotlin.math.abs

interface MapCoordinatesRange {
    val latitude: Latitude
    val longitute: Longitude
}

interface CoordinatesInterface {
    val span: Double

    val orientation: Int

    fun getMax(): Double

    fun getMin(): Double

    operator fun contains(value: Double): Boolean
}

class Latitude(val north: Double, val south: Double, override val orientation: Int) : CoordinatesInterface {
    override val span = abs(north - south)

    override fun getMax(): Double = maxOf(north, south)

    override fun getMin(): Double = minOf(north, south)

    override operator fun contains(value: Double): Boolean = value in south..north
}

class Longitude(val east: Double, val west: Double, override val orientation: Int) : CoordinatesInterface {
    override val span = abs(east - west)

    override fun getMax(): Double = maxOf(east, west)

    override fun getMin(): Double = minOf(east, west)

    override operator fun contains(value: Double): Boolean = value in west..east
}

open class MapZoomlevelsRange(val max: Int, val min: Int) {
    operator fun contains(value: Int): Boolean = value in min..max
}
