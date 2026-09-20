package com.rafambn.kmap.geometry.angle

import com.rafambn.kmap.utils.CanvasDrawReference
import com.rafambn.kmap.utils.Coordinates
import com.rafambn.kmap.utils.DifferentialScreenOffset
import com.rafambn.kmap.utils.ProjectedCoordinates
import com.rafambn.kmap.utils.Reference
import com.rafambn.kmap.utils.ScreenOffset
import com.rafambn.kmap.utils.TilePoint
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun Degrees.toRadians(): Radians = Radians(value * PI / 180)

fun Radians.toDegrees(): Degrees = Degrees(value * 180 / PI)

inline fun <reified T : Reference> T.rotate(radians: Radians): T {
    val cosRadians = cos(radians.value)
    val sinRadians = sin(radians.value)
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
    val cosRadians = cos(radians.value)
    val sinRadians = sin(radians.value)

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
