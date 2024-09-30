package com.rafambn.kmapdemo.utils

import com.rafambn.kmapdemo.utils.offsets.CanvasPosition
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

typealias Degrees = Double
typealias Radians = Double

fun Degrees.toRadians(): Radians = this * PI / 180

fun Radians.toDegrees(): Degrees = this * 180 / PI

fun Degrees.modulo(): Degrees = this.mod(180.0)

fun CanvasPosition.rotate(radians: Radians): CanvasPosition = CanvasPosition(
    this.horizontal * cos(radians) - this.vertical * sin(radians),
    this.horizontal * sin(radians) + this.vertical * cos(radians)
)

fun CanvasPosition.rotateCentered(centerOffset: CanvasPosition, radians: Radians): CanvasPosition = CanvasPosition(
    centerOffset.horizontal + (horizontal - centerOffset.horizontal) * cos(radians) - (vertical - centerOffset.vertical) * sin(
        radians
    ),
    centerOffset.vertical + (horizontal - centerOffset.horizontal) * sin(radians) + (vertical - centerOffset.vertical) * cos(
        radians
    )
)
