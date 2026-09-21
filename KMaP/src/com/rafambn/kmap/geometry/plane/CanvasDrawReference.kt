package com.rafambn.kmap.geometry.plane

data class CanvasDrawReference(override val x: Double, override val y: Double) : Reference {
    operator fun plus(other: CanvasDrawReference) = CanvasDrawReference(x + other.x, y + other.y)
    operator fun minus(other: CanvasDrawReference) = CanvasDrawReference(x - other.x, y - other.y)
    operator fun unaryMinus() = CanvasDrawReference(-x, -y)
    operator fun times(value: Double) = CanvasDrawReference(x * value, y * value)
    operator fun div(value: Double) = CanvasDrawReference(x / value, y / value)

    companion object {
        val Zero = CanvasDrawReference(0.0, 0.0)
    }
}
