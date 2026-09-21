package com.rafambn.kmap.geometry.angle

import kotlin.jvm.JvmInline

@JvmInline
value class Radians(val value: Double) {
    operator fun unaryMinus(): Radians = Radians(-value)
}
