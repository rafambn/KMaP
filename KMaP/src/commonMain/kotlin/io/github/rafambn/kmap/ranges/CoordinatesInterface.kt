package io.github.rafambn.kmap.ranges

interface MapCoordinatesRange {
    val latitude: Latitude
    val longitute: Longitude
}

class Latitude(val north: Double, val south: Double) {
    operator fun contains(value: Double): Boolean = value in south..north
}

class Longitude(val east: Double, val west: Double) {
    operator fun contains(value: Double): Boolean = value in west..east
}

open class MapZoomlevelsRange(val max: Int, val min: Int) {
    operator fun contains(value: Int): Boolean = value in min..max
}
