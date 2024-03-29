package io.github.rafambn.kmap

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import io.github.rafambn.kmap.model.Position
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
    return Position(
        this.horizontal * (TileCanvasState.TILE_SIZE * (2.0.pow(zoomLevel))),
        this.vertical * (TileCanvasState.TILE_SIZE * (2.0.pow(zoomLevel)))
    ).scaleToMap(1 / mapCoordinatesRange.longitute.span, 1 / mapCoordinatesRange.latitude.span)
}

fun Position.toMapReference(magnifierScale: Float, zoomLevel: Int, angleDegrees: Float, mapCoordinatesRange: MapCoordinatesRange): Position {
    return Position(
        this.horizontal / (TileCanvasState.TILE_SIZE * magnifierScale * 2.0.pow(zoomLevel)),
        this.vertical / (TileCanvasState.TILE_SIZE * magnifierScale * 2.0.pow(zoomLevel))
    ).rotateVector(-angleDegrees.degreesToRadian()).scaleToMap(mapCoordinatesRange.longitute.span, mapCoordinatesRange.latitude.span)
}

fun Position.scaleToMap(horizontal: Double, vertical: Double): Position {
    return Position(this.horizontal * horizontal, this.vertical * vertical)
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