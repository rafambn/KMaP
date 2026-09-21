package com.rafambn.kmap.geometry.plane

data class ScreenOffset(override val x: Double, override val y: Double) : Reference {
    operator fun plus(other: ScreenOffset) = ScreenOffset(x + other.x, y + other.y)
    operator fun minus(other: ScreenOffset) = ScreenOffset(x - other.x, y - other.y)
    operator fun unaryMinus() = ScreenOffset(-x, -y)
    operator fun times(value: Double) = ScreenOffset(x * value, y * value)
    operator fun div(value: Double) = ScreenOffset(x / value, y / value)

    companion object {
        val Zero = ScreenOffset(0.0, 0.0)
    }
}
