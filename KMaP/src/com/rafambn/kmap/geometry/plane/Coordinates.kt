package com.rafambn.kmap.geometry.plane

data class Coordinates(override val x: Double, override val y: Double) : Reference {
    operator fun plus(other: Coordinates) = Coordinates(x + other.x, y + other.y)
    operator fun minus(other: Coordinates) = Coordinates(x - other.x, y - other.y)
    operator fun unaryMinus() = Coordinates(-x, -y)
    operator fun times(value: Double) = Coordinates(x * value, y * value)
    operator fun div(value: Double) = Coordinates(x / value, y / value)

    companion object {
        val Zero = Coordinates(0.0, 0.0)
    }
}
