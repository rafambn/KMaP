package com.rafambn.kmap.utils.offsets

import androidx.compose.ui.geometry.Offset

interface Position {
    val horizontal: Double
    val vertical: Double

    fun toOffset(): Offset = Offset(horizontal.toFloat(), vertical.toFloat())

    operator fun plus(other: Position): Position
    operator fun unaryMinus(): Position
    operator fun minus(other: Position): Position
    operator fun times(operand: Double): Position
    operator fun div(operand: Double): Position
}