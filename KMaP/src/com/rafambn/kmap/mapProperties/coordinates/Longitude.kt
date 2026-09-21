package com.rafambn.kmap.mapProperties.coordinates

class Longitude(val east: Double, val west: Double) : CardinalRange {
    override val start: Double get() = west
    override val end: Double get() = east
}
