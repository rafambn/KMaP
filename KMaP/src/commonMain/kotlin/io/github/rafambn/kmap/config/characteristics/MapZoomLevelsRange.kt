package io.github.rafambn.kmap.config.characteristics

interface MapZoomLevelsRange {
    val max: Int
    val min: Int

    operator fun contains(value: Int): Boolean = value in min..max
}
