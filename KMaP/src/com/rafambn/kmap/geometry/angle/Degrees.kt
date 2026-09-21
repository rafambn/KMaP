package com.rafambn.kmap.geometry.angle

import kotlin.jvm.JvmInline

@JvmInline
value class Degrees(val value: Double) {
    operator fun plus(other: Degrees): Degrees = Degrees(value + other.value)

    operator fun minus(other: Degrees): Degrees = Degrees(value - other.value)

    operator fun unaryMinus(): Degrees = Degrees(-value)

    fun toFloat(): Float = value.toFloat()

    companion object {
        val Zero = Degrees(0.0)
    }
}
