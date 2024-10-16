package com.rafambn.kmap.utils.offsets

data class CanvasDrawReference(override val horizontal: Double, override val vertical: Double) : Position {
    override fun plus(other: Position) = CanvasDrawReference(horizontal + other.horizontal, vertical + other.vertical)
    override fun unaryMinus() = CanvasDrawReference(-horizontal, -vertical)
    override fun minus(other: Position) = CanvasDrawReference(horizontal - other.horizontal, vertical - other.vertical)
    override fun times(operand: Double) = CanvasDrawReference(horizontal * operand, vertical * operand)
    override fun div(operand: Double) = CanvasDrawReference(horizontal / operand, vertical / operand)
}