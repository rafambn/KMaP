package com.rafambn.kmap.mapProperties.coordinates

interface CoordinatesInterface {
    val span: Double

    fun getMax(): Double

    fun getMin(): Double

    fun getOrientation(): Int

    operator fun contains(value: Double): Boolean
}