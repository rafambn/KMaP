package com.rafambn.kmap.utils.offsets

data class ProjectedCoordinates(override val horizontal: Double, override val vertical: Double) : Position {
    override fun plus(other: Position) = ProjectedCoordinates(horizontal + other.horizontal, vertical + other.vertical)
    override fun unaryMinus() = ProjectedCoordinates(-horizontal, -vertical)
    override fun minus(other: Position) = ProjectedCoordinates(horizontal - other.horizontal, vertical - other.vertical)
    override fun times(operand: Double) = ProjectedCoordinates(horizontal * operand, vertical * operand)
    override fun div(operand: Double) = ProjectedCoordinates(horizontal / operand, vertical / operand)

    companion object {
        val Zero = ProjectedCoordinates(0.0, 0.0)
    }
}
