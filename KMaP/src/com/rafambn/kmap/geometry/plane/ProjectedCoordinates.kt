package com.rafambn.kmap.geometry.plane

data class ProjectedCoordinates(override val x: Double, override val y: Double) : Reference {
    operator fun plus(other: ProjectedCoordinates) = ProjectedCoordinates(x + other.x, y + other.y)
    operator fun minus(other: ProjectedCoordinates) = ProjectedCoordinates(x - other.x, y - other.y)
    operator fun unaryMinus() = ProjectedCoordinates(-x, -y)
    operator fun times(value: Double) = ProjectedCoordinates(x * value, y * value)
    operator fun div(value: Double) = ProjectedCoordinates(x / value, y / value)

    companion object {
        val Zero = ProjectedCoordinates(0.0, 0.0)
    }
}
