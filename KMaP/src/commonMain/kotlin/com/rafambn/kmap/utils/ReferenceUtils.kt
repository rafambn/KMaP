package com.rafambn.kmap.utils

import androidx.compose.ui.geometry.Offset

interface Reference

data class ScreenOffset(val x: Float, val y: Float) : Reference {
    operator fun plus(reference: ScreenOffset) = ScreenOffset(x + reference.x, y + reference.y)
    operator fun unaryMinus() = ScreenOffset(-x, -y)
    operator fun minus(reference: ScreenOffset) = ScreenOffset(x - reference.x, y - reference.y)
    operator fun times(value: Number) = ScreenOffset(x * value.toFloat(), y * value.toFloat())
    operator fun div(value: Number) = ScreenOffset(x / value.toFloat(), y / value.toFloat())

    companion object {
        val Zero: ScreenOffset = ScreenOffset(0F, 0F)
    }
}

data class CanvasPosition(val horizontal: Double, val vertical: Double) : Reference {
    operator fun plus(reference: CanvasPosition) = CanvasPosition(horizontal + reference.horizontal, vertical + reference.vertical)
    operator fun unaryMinus() = CanvasPosition(-horizontal, -vertical)
    operator fun minus(reference: CanvasPosition) = CanvasPosition(horizontal - reference.horizontal, vertical - reference.vertical)
    operator fun times(value: Number) = CanvasPosition(horizontal * value.toDouble(), vertical * value.toDouble())
    operator fun div(value: Number) = CanvasPosition(horizontal / value.toDouble(), vertical / value.toDouble())

    companion object {
        val Zero: CanvasPosition = CanvasPosition(0.0, 0.0)
    }
}

data class ProjectedCoordinates(val longitude: Double, val latitude: Double) : Reference {
    operator fun plus(reference: ProjectedCoordinates) = ProjectedCoordinates(longitude + reference.longitude, latitude + reference.latitude)
    operator fun unaryMinus() = ProjectedCoordinates(-longitude, -latitude)
    operator fun minus(reference: ProjectedCoordinates) = ProjectedCoordinates(longitude - reference.longitude, latitude - reference.latitude)
    operator fun times(value: Number) = ProjectedCoordinates(longitude * value.toDouble(), latitude * value.toDouble())
    operator fun div(value: Number) = ProjectedCoordinates(longitude / value.toDouble(), latitude / value.toDouble())
}

data class CanvasDrawReference(val horizontal: Double, val vertical: Double) : Reference {
    fun plus(other: CanvasDrawReference) = CanvasDrawReference(horizontal + other.horizontal, vertical + other.vertical)
    fun unaryMinus() = CanvasDrawReference(-horizontal, -vertical)
    fun minus(other: CanvasDrawReference) = CanvasDrawReference(horizontal - other.horizontal, vertical - other.vertical)
    fun times(operand: Double) = CanvasDrawReference(horizontal * operand, vertical * operand)
    fun div(operand: Double) = CanvasDrawReference(horizontal / operand, vertical / operand)
}

data class DifferentialScreenOffset(val x: Float, val y: Float) : Reference {
    operator fun plus(reference: DifferentialScreenOffset) = DifferentialScreenOffset(x + reference.x, y + reference.y)
    operator fun unaryMinus() = DifferentialScreenOffset(-x, -y)
    operator fun minus(reference: DifferentialScreenOffset) = DifferentialScreenOffset(x - reference.x, y - reference.y)
    operator fun times(value: Number) = DifferentialScreenOffset(x * value.toFloat(), y * value.toFloat())
    operator fun div(value: Number) = DifferentialScreenOffset(x / value.toFloat(), y / value.toFloat())
}

fun CanvasPosition.asScreenOffset() = ScreenOffset(this.horizontal.toFloat(), this.vertical.toFloat())
fun Offset.asScreenOffset() = ScreenOffset(this.x, this.y)

fun DifferentialScreenOffset.asCanvasPosition() = CanvasPosition(this.x.toDouble(), this.y.toDouble())

fun ScreenOffset.asOffset() = Offset(this.x, this.y)

fun Offset.asDifferentialScreenOffset() = DifferentialScreenOffset(this.x, this.y)
fun ScreenOffset.asDifferentialScreenOffset() = DifferentialScreenOffset(this.x, this.y)