package io.github.rafambn.kmap.utils

import androidx.compose.ui.geometry.Offset

class Position(val horizontal: Double, val vertical: Double) {

    fun Position.toOffset(): Offset = Offset(horizontal.toFloat(), vertical.toFloat())

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Position) return false

        if (horizontal != other.horizontal) return false
        if (vertical != other.vertical) return false

        return true
    }

    override fun hashCode(): Int {
        var result = horizontal.hashCode()
        result = 31 * result + vertical.hashCode()
        return result
    }

    override fun toString(): String {
        return "${this::class.simpleName}(horizontal=$horizontal, vertical=$vertical)"
    }

    operator fun plus(other: Position): Position {
        return Position(this.horizontal + other.horizontal, this.vertical + other.vertical)
    }

    operator fun unaryMinus(): Position = Position(-horizontal, -vertical)

    operator fun minus(other: Position): Position {
        return Position(this.horizontal - other.horizontal, this.vertical - other.vertical)
    }

    operator fun times(operand: Double): Position {
        return Position(this.horizontal * operand, this.vertical * operand)
    }

    operator fun div(operand: Double): Position {
        return Position(this.horizontal / operand, this.vertical / operand)
    }

    companion object {
        val Zero = Position(0.0, 0.0)
    }
}