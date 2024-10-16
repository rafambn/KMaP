package com.rafambn.kmap.utils.offsets

data class CanvasPosition(override val horizontal: Double, override val vertical: Double) : Position {
    override fun plus(other: Position) = CanvasPosition(horizontal + other.horizontal, vertical + other.vertical)
    override fun unaryMinus() = CanvasPosition(-horizontal, -vertical)
    override fun minus(other: Position) = CanvasPosition(horizontal - other.horizontal, vertical - other.vertical)
    override fun times(operand: Double) = CanvasPosition(horizontal * operand, vertical * operand)
    override fun div(operand: Double) = CanvasPosition(horizontal / operand, vertical / operand)

    companion object {
        val Zero = CanvasPosition(0.0, 0.0)
    }
}
