package com.rafambn.kmap.utils

import androidx.compose.ui.geometry.Offset

sealed interface Reference

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

data class TilePoint(val horizontal: Double, val vertical: Double) : Reference {
    operator fun plus(reference: TilePoint) = TilePoint(horizontal + reference.horizontal, vertical + reference.vertical)
    operator fun unaryMinus() = TilePoint(-horizontal, -vertical)
    operator fun minus(reference: TilePoint) = TilePoint(horizontal - reference.horizontal, vertical - reference.vertical)
    operator fun times(value: Number) = TilePoint(horizontal * value.toDouble(), vertical * value.toDouble())
    operator fun div(value: Number) = TilePoint(horizontal / value.toDouble(), vertical / value.toDouble())

    companion object {
        val Zero: TilePoint = TilePoint(0.0, 0.0)
    }
}

data class Coordinates(val longitude: Double, val latitude: Double) : Reference {
    operator fun plus(reference: Coordinates) = Coordinates(longitude + reference.longitude, latitude + reference.latitude)
    operator fun unaryMinus() = Coordinates(-longitude, -latitude)
    operator fun minus(reference: Coordinates) = Coordinates(longitude - reference.longitude, latitude - reference.latitude)
    operator fun times(value: Number) = Coordinates(longitude * value.toDouble(), latitude * value.toDouble())
    operator fun div(value: Number) = Coordinates(longitude / value.toDouble(), latitude / value.toDouble())

    companion object {
        val Zero: Coordinates = Coordinates(0.0, 0.0)
    }
}

data class DifferentialScreenOffset(val x: Float, val y: Float) : Reference {
    operator fun plus(reference: DifferentialScreenOffset) = DifferentialScreenOffset(x + reference.x, y + reference.y)
    operator fun unaryMinus() = DifferentialScreenOffset(-x, -y)
    operator fun minus(reference: DifferentialScreenOffset) = DifferentialScreenOffset(x - reference.x, y - reference.y)
    operator fun times(value: Number) = DifferentialScreenOffset(x * value.toFloat(), y * value.toFloat())
    operator fun div(value: Number) = DifferentialScreenOffset(x / value.toFloat(), y / value.toFloat())
    companion object {
        val Zero: DifferentialScreenOffset = DifferentialScreenOffset(0F, 0F)
    }
}

data class CanvasDrawReference(val horizontal: Double, val vertical: Double) {
    fun plus(other: CanvasDrawReference) = CanvasDrawReference(horizontal + other.horizontal, vertical + other.vertical)
    fun unaryMinus() = CanvasDrawReference(-horizontal, -vertical)
    fun minus(other: CanvasDrawReference) = CanvasDrawReference(horizontal - other.horizontal, vertical - other.vertical)
    fun times(operand: Double) = CanvasDrawReference(horizontal * operand, vertical * operand)
    fun div(operand: Double) = CanvasDrawReference(horizontal / operand, vertical / operand)
}

fun TilePoint.asScreenOffset() = ScreenOffset(this.horizontal.toFloat(), this.vertical.toFloat())
fun TilePoint.asCanvasDrawReference() = CanvasDrawReference(this.horizontal, this.vertical)

fun Offset.asScreenOffset() = ScreenOffset(this.x, this.y)
fun Offset.asDifferentialScreenOffset() = DifferentialScreenOffset(this.x, this.y)

fun DifferentialScreenOffset.asCanvasPosition() = TilePoint(this.x.toDouble(), this.y.toDouble())

fun ScreenOffset.asOffset() = Offset(this.x, this.y)
fun ScreenOffset.asDifferentialScreenOffset() = DifferentialScreenOffset(this.x, this.y)

fun transformReference(
    pointX: Double,
    pointY: Double,
    sourceRangeX: Pair<Double, Double>,
    sourceRangeY: Pair<Double, Double>,
    targetRangeX: Pair<Double, Double>,
    targetRangeY: Pair<Double, Double>
): Pair<Double, Double> {

    val sourceWidth = sourceRangeX.second - sourceRangeX.first
    val sourceHeight = sourceRangeY.second - sourceRangeY.first
    val targetWidth = targetRangeX.second - targetRangeX.first
    val targetHeight = targetRangeY.second - targetRangeY.first

    val normalizedX = (pointX - sourceRangeX.first) / sourceWidth
    val normalizedY = (pointY - sourceRangeY.first) / sourceHeight

    val transformedX = normalizedX * targetWidth + targetRangeX.first
    val transformedY = normalizedY * targetHeight + targetRangeY.first

    return Pair(transformedX, transformedY)
}