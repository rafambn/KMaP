package com.rafambn.kmap.mapProperties.coordinates

class Latitude(val north: Double, val south: Double) : CardinalRange {
    override val start: Double get() = north
    override val end: Double get() = south
}
