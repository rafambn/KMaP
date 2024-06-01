package io.github.rafambn.kmap.utils

import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

typealias Degrees = Double
typealias Radians = Double

fun Degrees.toRadians(): Radians = this * PI / 180

fun Radians.toDegrees(): Degrees = this * 180 / PI

fun Degrees.modulo(): Degrees = this.mod(180.0)

fun Offset.rotate(radians: Radians): Offset {
    return Offset(
        (this.x * cos(radians) - this.y * sin(radians)).toFloat(),
        (this.x * sin(radians) + this.y * cos(radians)).toFloat()
    )
}

fun Offset.rotateCentered(centerOffset: Offset, radians: Radians): Offset {
    return Offset(
        (centerOffset.x + (x - centerOffset.x) * cos(radians) - (y - centerOffset.y) * sin(radians)).toFloat(),
        (centerOffset.y + (x - centerOffset.x) * sin(radians) + (y - centerOffset.y) * cos(radians)).toFloat()
    )
}