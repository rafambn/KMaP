package com.rafambn.kmap.geometry.plane

data class DifferentialScreenOffset(override val x: Double, override val y: Double) : Reference {
    operator fun plus(other: DifferentialScreenOffset) = DifferentialScreenOffset(x + other.x, y + other.y)
    operator fun minus(other: DifferentialScreenOffset) = DifferentialScreenOffset(x - other.x, y - other.y)
    operator fun unaryMinus() = DifferentialScreenOffset(-x, -y)
    operator fun times(value: Double) = DifferentialScreenOffset(x * value, y * value)
    operator fun div(value: Double) = DifferentialScreenOffset(x / value, y / value)

    companion object {
        val Zero = DifferentialScreenOffset(0.0, 0.0)
    }
}
