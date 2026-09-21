package com.rafambn.kmap.geometry.plane

data class TilePoint(override val x: Double, override val y: Double) : Reference {
    operator fun plus(other: TilePoint) = TilePoint(x + other.x, y + other.y)
    operator fun minus(other: TilePoint) = TilePoint(x - other.x, y - other.y)
    operator fun unaryMinus() = TilePoint(-x, -y)
    operator fun times(value: Double) = TilePoint(x * value, y * value)
    operator fun div(value: Double) = TilePoint(x / value, y / value)

    companion object {
        val Zero = TilePoint(0.0, 0.0)
    }
}
