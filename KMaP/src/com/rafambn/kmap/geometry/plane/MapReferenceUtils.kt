package com.rafambn.kmap.geometry.plane

import androidx.compose.ui.geometry.Offset
import com.rafambn.kmap.MapState
import com.rafambn.kmap.geometry.angle.rotate
import com.rafambn.kmap.geometry.angle.toRadians
import com.rafambn.kmap.utils.toIntFloor
import kotlin.math.pow

fun TilePoint.asScreenOffset() = ScreenOffset(x, y)
fun TilePoint.asCanvasDrawReference() = CanvasDrawReference(x, y)

fun ScreenOffset.asOffset() = Offset(x.toFloat(), y.toFloat())
fun ScreenOffset.asDifferentialScreenOffset() = DifferentialScreenOffset(x, y)

fun Offset.asScreenOffset() = ScreenOffset(x.toDouble(), y.toDouble())
fun Offset.asDifferentialScreenOffset() = DifferentialScreenOffset(x.toDouble(), y.toDouble())

fun DifferentialScreenOffset.asCanvasPosition() = TilePoint(x, y)
fun DifferentialScreenOffset.asTilePoint() = TilePoint(x, y)

context(mapState: MapState)
fun ScreenOffset.toTilePoint(): TilePoint =
    (mapState.cameraState.canvasSize / 2.0 - this)
        .asDifferentialScreenOffset()
        .toTilePoint() + mapState.cameraState.tilePoint

context(mapState: MapState)
fun DifferentialScreenOffset.toTilePoint(): TilePoint {
    val tileWidth = with(mapState) { mapProperties.tileSize.width.toPx() }
    val tileHeight = with(mapState) { mapProperties.tileSize.height.toPx() }
    val zoomScale = 2F.pow(mapState.cameraState.zoom)

    return asCanvasPosition()
        .scale(
            tileWidth.toDouble() / (tileWidth * zoomScale),
            tileHeight.toDouble() / (tileHeight * zoomScale)
        )
        .rotate(-mapState.cameraState.angleDegrees.toRadians())
        .unaryMinus()
}

context(mapState: MapState)
fun TilePoint.toScreenOffset(): ScreenOffset {
    val tileWidth = with(mapState) { mapProperties.tileSize.width.toPx() }
    val tileHeight = with(mapState) { mapProperties.tileSize.height.toPx() }
    val zoomScale = 2F.pow(mapState.cameraState.zoom)

    return (this - mapState.cameraState.tilePoint)
        .unaryMinus()
        .rotate(mapState.cameraState.angleDegrees.toRadians())
        .scale(
            tileWidth * zoomScale / tileWidth.toDouble(),
            tileHeight * zoomScale / tileHeight.toDouble()
        )
        .asScreenOffset()
        .minus(mapState.cameraState.canvasSize / 2.0)
        .unaryMinus()
}

context(mapState: MapState)
internal fun TilePoint.toCanvasDrawReference(): CanvasDrawReference {
    val tileWidth = with(mapState) { mapProperties.tileSize.width.toPx() }
    val tileHeight = with(mapState) { mapProperties.tileSize.height.toPx() }
    val zoomLevel = mapState.cameraState.zoom.toIntFloor()

    return scale(
        tileWidth * (1 shl zoomLevel) / tileWidth.toDouble(),
        tileHeight * (1 shl zoomLevel) / tileHeight.toDouble()
    )
        .unaryMinus()
        .asCanvasDrawReference()
}

context(mapState: MapState)
fun Coordinates.toTilePoint(): TilePoint {
    val mapProperties = mapState.mapProperties
    val projectedCoordinates = mapProperties.toProjectedCoordinates(this)
    return projectedCoordinates.toTilePoint()
}

context(mapState: MapState)
fun ProjectedCoordinates.toTilePoint(): TilePoint {
    val mapProperties = mapState.mapProperties
    val tileWidth = with(mapState) { mapProperties.tileSize.width.toPx().toDouble() }
    val tileHeight = with(mapState) { mapProperties.tileSize.height.toPx().toDouble() }
    val scaledTilePoint = transformReference(
        x,
        y,
        Pair(mapProperties.coordinatesRange.longitude.west, mapProperties.coordinatesRange.longitude.east),
        Pair(mapProperties.coordinatesRange.latitude.north, mapProperties.coordinatesRange.latitude.south),
        Pair(0.0, tileWidth),
        Pair(0.0, tileHeight),
    )
    return TilePoint(scaledTilePoint.first, scaledTilePoint.second)
}

context(mapState: MapState)
fun TilePoint.toCoordinates(): Coordinates {
    val mapProperties = mapState.mapProperties
    val tileWidth = with(mapState) { mapProperties.tileSize.width.toPx().toDouble() }
    val tileHeight = with(mapState) { mapProperties.tileSize.height.toPx().toDouble() }
    val scaledTileCoordinates = transformReference(
        x,
        y,
        Pair(0.0, tileWidth),
        Pair(0.0, tileHeight),
        Pair(mapProperties.coordinatesRange.longitude.west, mapProperties.coordinatesRange.longitude.east),
        Pair(mapProperties.coordinatesRange.latitude.north, mapProperties.coordinatesRange.latitude.south),
    )
    return mapProperties.toCoordinates(ProjectedCoordinates(scaledTileCoordinates.first, scaledTileCoordinates.second))
}

private fun TilePoint.scale(horizontal: Double, vertical: Double): TilePoint =
    TilePoint(x * horizontal, y * vertical)

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
