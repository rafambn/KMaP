package io.github.rafambn.kmap

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import io.github.rafambn.kmap.model.Position
import io.github.rafambn.kmap.ranges.CoordinatesInterface
import io.github.rafambn.kmap.ranges.MapCoordinatesRange
import io.github.rafambn.kmap.states.TileCanvasState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.readResourceBytes
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

fun Float.degreesToRadian(): Float {
    return (this * PI / 180).toFloat()
}

fun Offset.rotateVector(angleRadians: Float): Offset {
    return Offset(
        this.x * cos(angleRadians) - this.y * sin(angleRadians),
        this.x * sin(angleRadians) + this.y * cos(angleRadians)
    )
}

fun Position.rotateVector(angleRadians: Float): Position {
    return Position(
        (this.horizontal * cos(angleRadians) - this.vertical * sin(angleRadians)),
        (this.horizontal * sin(angleRadians) + this.vertical * cos(angleRadians))
    )
}

fun Offset.toPosition(): Position {
    return Position(this.x.toDouble(), this.y.toDouble())
}

fun Position.toOffset(): Offset {
    return Offset(this.horizontal.toFloat(), this.vertical.toFloat())
}

fun Position.toCanvasReference(zoomLevel: Int, mapCoordinatesRange: MapCoordinatesRange): Position {
    return this.invertPosition()
        .moveToTrueCoordinates(mapCoordinatesRange)
        .scaleToZoom((TileCanvasState.TILE_SIZE * (1 shl zoomLevel)).toDouble())
        .scaleToMap(1 / mapCoordinatesRange.longitute.span, 1 / mapCoordinatesRange.latitude.span)
}

fun Position.scaleToZoom(zoomScale: Double): Position {
    return Position(horizontal * zoomScale, vertical * zoomScale)
}

fun Position.moveToTrueCoordinates(mapCoordinatesRange: MapCoordinatesRange): Position {
    return Position(horizontal - mapCoordinatesRange.longitute.span / 2, vertical - mapCoordinatesRange.latitude.span / 2)
}

fun Position.scaleToMap(horizontal: Double, vertical: Double): Position {
    return Position(this.horizontal * horizontal, this.vertical * vertical)
}

fun Position.toMapReference(magnifierScale: Float, zoomLevel: Int, angleDegrees: Float, mapCoordinatesRange: MapCoordinatesRange): Position {
    return this
        .scaleToZoom(1 / (TileCanvasState.TILE_SIZE * magnifierScale * 2.0.pow(zoomLevel)))
        .rotateVector(-angleDegrees.degreesToRadian())
        .scaleToMap(mapCoordinatesRange.longitute.span, mapCoordinatesRange.latitude.span)
        .invertPosition()
}

fun Double.loopInRange(coordinatesRange: CoordinatesInterface): Double {
    return (this - coordinatesRange.getMin()).mod(coordinatesRange.span) + coordinatesRange.getMin()
}

fun Int.loopInZoom(zoomLevel: Int): Int {
    return this.mod(1 shl zoomLevel)
}

fun Position.invertPosition(): Position {
    return Position(-horizontal, vertical)
}
fun Position.invertPosition2(): Position {
    return Position(horizontal, -vertical)
}

@OptIn(InternalResourceApi::class)
inline fun imageBitmapResource(
    res: String,
    crossinline imageBitmap: (ImageBitmap) -> Unit
) {
    val coroutineScope = CoroutineScope(Dispatchers.Default)
    coroutineScope.launch {
        imageBitmap.invoke(readResourceBytes(res).toImageBitmap())
    }
}


expect fun ByteArray.toImageBitmap(): ImageBitmap