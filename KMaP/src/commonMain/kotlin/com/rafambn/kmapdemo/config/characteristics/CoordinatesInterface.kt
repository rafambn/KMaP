package com.rafambn.kmapdemo.config.characteristics

interface CoordinatesInterface {
    val span: Double

    val orientation: Int

    fun getMax(): Double

    fun getMin(): Double

    operator fun contains(value: Double): Boolean
}