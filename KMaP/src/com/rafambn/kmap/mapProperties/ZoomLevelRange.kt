package com.rafambn.kmap.mapProperties

data class ZoomLevelRange(
    val min: Int,
    val max: Int,
) {
    operator fun contains(value: Int): Boolean = value in min..max
}
