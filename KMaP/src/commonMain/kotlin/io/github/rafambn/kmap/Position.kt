package io.github.rafambn.kmap

class Position(val horizontal: Double, val vertical: Double) {

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
        return "Position(horizontal=$horizontal, vertical=$vertical)"
    }

    companion object {
        val Zero = Position(0.0, 0.0)
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
}
