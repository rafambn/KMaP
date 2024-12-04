package com.rafambn.kmap.mapProperties

interface MapZoomLevelsRange {
    val max: Int
    val min: Int

    operator fun contains(value: Int): Boolean = value in min..max
}
