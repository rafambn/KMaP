package com.rafambn.kmap.geometry.angle

import com.rafambn.kmap.geometry.plane.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun Degrees.toRadians(): Radians = Radians(value * PI / 180)

fun Radians.toDegrees(): Degrees = Degrees(value * 180 / PI)

fun ScreenOffset.rotate(radians: Radians): ScreenOffset = rotate(this, radians, ::ScreenOffset)
fun TilePoint.rotate(radians: Radians): TilePoint = rotate(this, radians, ::TilePoint)
fun Coordinates.rotate(radians: Radians): Coordinates = rotate(this, radians, ::Coordinates)
fun ProjectedCoordinates.rotate(radians: Radians): ProjectedCoordinates = rotate(this, radians, ::ProjectedCoordinates)
fun DifferentialScreenOffset.rotate(radians: Radians): DifferentialScreenOffset =
    rotate(this, radians, ::DifferentialScreenOffset)

fun CanvasDrawReference.rotate(radians: Radians): CanvasDrawReference =
    rotate(this, radians, ::CanvasDrawReference)

fun ScreenOffset.rotateCentered(center: ScreenOffset, radians: Radians): ScreenOffset =
    rotateCentered(this, center, radians, ::ScreenOffset)

fun TilePoint.rotateCentered(center: TilePoint, radians: Radians): TilePoint =
    rotateCentered(this, center, radians, ::TilePoint)

fun Coordinates.rotateCentered(center: Coordinates, radians: Radians): Coordinates =
    rotateCentered(this, center, radians, ::Coordinates)

fun ProjectedCoordinates.rotateCentered(
    center: ProjectedCoordinates,
    radians: Radians
): ProjectedCoordinates = rotateCentered(this, center, radians, ::ProjectedCoordinates)

fun DifferentialScreenOffset.rotateCentered(
    center: DifferentialScreenOffset,
    radians: Radians
): DifferentialScreenOffset = rotateCentered(this, center, radians, ::DifferentialScreenOffset)

fun CanvasDrawReference.rotateCentered(
    center: CanvasDrawReference,
    radians: Radians
): CanvasDrawReference = rotateCentered(this, center, radians, ::CanvasDrawReference)

private fun <T : Reference> rotate(
    reference: Reference,
    radians: Radians,
    create: (x: Double, y: Double) -> T
): T {
    val cosRadians = cos(radians.value)
    val sinRadians = sin(radians.value)
    val newX = reference.x * cosRadians - reference.y * sinRadians
    val newY = reference.x * sinRadians + reference.y * cosRadians
    return create(newX, newY)
}

private fun <T : Reference> rotateCentered(
    reference: Reference,
    center: Reference,
    radians: Radians,
    create: (x: Double, y: Double) -> T
): T {
    val cosRadians = cos(radians.value)
    val sinRadians = sin(radians.value)

    val translatedX = reference.x - center.x
    val translatedY = reference.y - center.y

    val rotatedX = translatedX * cosRadians - translatedY * sinRadians
    val rotatedY = translatedX * sinRadians + translatedY * cosRadians

    val newX = center.x + rotatedX
    val newY = center.y + rotatedY

    return create(newX, newY)
}
