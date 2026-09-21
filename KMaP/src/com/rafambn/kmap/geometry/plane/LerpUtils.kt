package com.rafambn.kmap.geometry.plane

import com.rafambn.kmap.utils.lerp as lerpDouble

fun lerp(start: ScreenOffset, end: ScreenOffset, value: Double): ScreenOffset =
    lerp(start, end, value, ::ScreenOffset)

fun lerp(start: TilePoint, end: TilePoint, value: Double): TilePoint =
    lerp(start, end, value, ::TilePoint)

fun lerp(start: Coordinates, end: Coordinates, value: Double): Coordinates =
    lerp(start, end, value, ::Coordinates)

fun lerp(
    start: ProjectedCoordinates,
    end: ProjectedCoordinates,
    value: Double
): ProjectedCoordinates = lerp(start, end, value, ::ProjectedCoordinates)

fun lerp(
    start: DifferentialScreenOffset,
    end: DifferentialScreenOffset,
    value: Double
): DifferentialScreenOffset = lerp(start, end, value, ::DifferentialScreenOffset)

fun lerp(
    start: CanvasDrawReference,
    end: CanvasDrawReference,
    value: Double
): CanvasDrawReference = lerp(start, end, value, ::CanvasDrawReference)

private fun <T : Reference> lerp(
    start: Reference,
    end: Reference,
    value: Double,
    create: (x: Double, y: Double) -> T
): T {
    val newX = lerpDouble(start.x, end.x, value)
    val newY = lerpDouble(start.y, end.y, value)
    return create(newX, newY)
}
