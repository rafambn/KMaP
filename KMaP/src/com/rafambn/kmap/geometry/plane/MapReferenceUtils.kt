package com.rafambn.kmap.geometry.plane

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import com.rafambn.kmap.MapState
import com.rafambn.kmap.geometry.angle.rotate
import com.rafambn.kmap.geometry.angle.toRadians
import com.rafambn.kmap.mapProperties.border.OutsideTilesType
import com.rafambn.kmap.utils.toIntFloor
import kotlin.math.pow

fun TilePoint.asScreenOffset() = ScreenOffset(x, y)
fun TilePoint.asCanvasDrawReference() = CanvasDrawReference(x, y)

fun ScreenOffset.asOffset() = Offset(x.toFloat(), y.toFloat())
fun ScreenOffset.asDifferentialScreenOffset() = DifferentialScreenOffset(x, y)

fun Offset.asScreenOffset() = ScreenOffset(x.toDouble(), y.toDouble())
fun Offset.asDifferentialScreenOffset() = DifferentialScreenOffset(x.toDouble(), y.toDouble())
internal fun IntSize.asScreenOffset() = ScreenOffset(width.toDouble(), height.toDouble())

fun DifferentialScreenOffset.asCanvasPosition() = TilePoint(x, y)
fun DifferentialScreenOffset.asTilePoint() = TilePoint(x, y)

context(mapState: MapState)
fun ScreenOffset.toTilePoint(): TilePoint =
    (mapState.viewportSize.asScreenOffset() / 2.0 - this)
        .asDifferentialScreenOffset()
        .toTilePoint() + mapState.cameraState.tilePoint

context(mapState: MapState)
fun DifferentialScreenOffset.toTilePoint(): TilePoint {
    val inverseScale = 2.0.pow(-mapState.cameraState.zoom.toDouble()) / mapState.density

    return asCanvasPosition()
        .scale(inverseScale, inverseScale)
        .rotate(-mapState.cameraState.angleDegrees.toRadians())
        .unaryMinus()
}

context(mapState: MapState)
fun TilePoint.toScreenOffset(): ScreenOffset {
    val scale = 2.0.pow(mapState.cameraState.zoom.toDouble()) * mapState.density
    val cameraOffset = this - mapState.cameraState.tilePoint

    return cameraOffset
        .unaryMinus()
        .rotate(mapState.cameraState.angleDegrees.toRadians())
        .scale(scale, scale)
        .asScreenOffset()
        .minus(mapState.viewportSize.asScreenOffset() / 2.0)
        .unaryMinus()
}

context(mapState: MapState)
internal fun TilePoint.toNearestScreenOffset(): ScreenOffset {
    if (mapState.mapProperties.outsideTiles != OutsideTilesType.LOOP) return toScreenOffset()

    val (mapWidth, mapHeight) = mapState.mapSize()
    val cameraPoint = mapState.cameraState.tilePoint
    val cameraOffset = this - cameraPoint
    return (cameraPoint + TilePoint(
        cameraOffset.x.nearestLoopOffset(mapWidth),
        cameraOffset.y.nearestLoopOffset(mapHeight),
    )).toScreenOffset()
}

context(mapState: MapState)
internal fun TilePoint.toCanvasDrawReference(): CanvasDrawReference {
    val zoomLevel = mapState.cameraState.zoom.toIntFloor()
    val scale = 2.0.pow(zoomLevel) * mapState.density

    return scale(scale, scale)
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
    val (tileWidth, tileHeight) = mapState.mapSize()
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
    val (tileWidth, tileHeight) = mapState.mapSize()
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

private fun MapState.mapSize(): Pair<Double, Double> {
    val width = mapProperties.tileSize.width.value.toDouble()
    val height = mapProperties.tileSize.height.value.toDouble()

    require(width.isFinite() && width > 0.0) { "Tile width must be finite and greater than zero" }
    require(height.isFinite() && height > 0.0) { "Tile height must be finite and greater than zero" }

    return Pair(width, height)
}

private fun Double.nearestLoopOffset(size: Double): Double =
    (this + size / 2.0).mod(size) - size / 2.0

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

    require(sourceWidth.isFinite() && sourceWidth != 0.0) { "Source X range must have a finite, non-zero span" }
    require(sourceHeight.isFinite() && sourceHeight != 0.0) { "Source Y range must have a finite, non-zero span" }

    val normalizedX = (pointX - sourceRangeX.first) / sourceWidth
    val normalizedY = (pointY - sourceRangeY.first) / sourceHeight

    val transformedX = normalizedX * targetWidth + targetRangeX.first
    val transformedY = normalizedY * targetHeight + targetRangeY.first

    return Pair(transformedX, transformedY)
}
