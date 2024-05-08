package io.github.rafambn.kmap.utils

import androidx.compose.ui.geometry.Offset
import io.github.rafambn.kmap.Position
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

fun Position.rotate(radians: Radians): Position {
    return Position(
        horizontal * cos(radians) - vertical * sin(radians),
        horizontal * sin(radians) + vertical * cos(radians)
    )
}

fun Offset.rotateCentered(centerOffset: Offset, radians: Radians): Offset {
    return Offset(
        (centerOffset.x + (x - centerOffset.x) * cos(radians) - (y - centerOffset.y) * sin(radians)).toFloat(),
        (centerOffset.y + (x - centerOffset.x) * sin(radians) + (-centerOffset.y) * cos(radians)).toFloat()
    )
}

fun Position.rotateCentered(centerPosition: Position, radians: Radians): Position {
    return Position(
        centerPosition.horizontal + (horizontal - centerPosition.horizontal) * cos(radians) - (vertical - centerPosition.vertical) * sin(radians),
        centerPosition.vertical + (horizontal - centerPosition.horizontal) * sin(radians) + (vertical -centerPosition.vertical) * cos(radians)
    )
}