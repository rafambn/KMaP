package com.rafambn.kmap.utils

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

typealias Degrees = Double
typealias Radians = Double

fun Degrees.toRadians(): Radians = this * PI / 180

fun Radians.toDegrees(): Degrees = this * 180 / PI

fun Degrees.modulo(): Degrees = this.mod(180.0)

inline fun <reified T : Reference> T.rotate(radians: Radians): T {
    val cosRadians = cos(radians)
    val sinRadians = sin(radians)
    val newX = this.x * cosRadians - this.y * sinRadians
    val newY = this.x * sinRadians + this.y * cosRadians

    return when (T::class) {
        ScreenOffset::class -> ScreenOffset(newX, newY) as T
        TilePoint::class -> TilePoint(newX, newY) as T
        Coordinates::class -> Coordinates(newX, newY) as T
        ProjectedCoordinates::class -> ProjectedCoordinates(newX, newY) as T
        DifferentialScreenOffset::class -> DifferentialScreenOffset(newX, newY) as T
        CanvasDrawReference::class -> CanvasDrawReference(newX, newY) as T
        else -> throw IllegalArgumentException("Unsupported type for rotation: ${T::class.simpleName}")
    }
}

inline fun <reified T : Reference> T.rotateCentered(center: T, radians: Radians): T {
    val cosRadians = cos(radians)
    val sinRadians = sin(radians)

    val translatedX = this.x - center.x
    val translatedY = this.y - center.y

    val rotatedX = translatedX * cosRadians - translatedY * sinRadians
    val rotatedY = translatedX * sinRadians + translatedY * cosRadians

    val newX = center.x + rotatedX
    val newY = center.y + rotatedY

    return when (T::class) {
        ScreenOffset::class -> ScreenOffset(newX, newY) as T
        TilePoint::class -> TilePoint(newX, newY) as T
        Coordinates::class -> Coordinates(newX, newY) as T
        ProjectedCoordinates::class -> ProjectedCoordinates(newX, newY) as T
        DifferentialScreenOffset::class -> DifferentialScreenOffset(newX, newY) as T
        CanvasDrawReference::class -> CanvasDrawReference(newX, newY) as T
        else -> throw IllegalArgumentException("Unsupported type for centered rotation: ${T::class.simpleName}")
    }
}
