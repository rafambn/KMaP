package com.rafambn.kmap.geometry.angle

import kotlin.jvm.JvmInline

@JvmInline
value class Radians(val value: Double) {
    init {
        require(value.isFinite()) { "Radians must be finite" }
    }

    operator fun unaryMinus(): Radians = Radians(-value)
}
