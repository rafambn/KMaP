package io.github.rafambn.kmap.states

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import io.github.rafambn.kmap.enums.MapBorderType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sin

class MapState(
    initialPosition: Offset = Offset.Zero,
    initialZoom: Float = 1F,
    initialRotation: Float = 0F,
    boundHorizontal: MapBorderType = MapBorderType.BOUND,
    boundVertical: MapBorderType = MapBorderType.BOUND,
    maxZoom: Float = 10F,
    minZoom: Float = 1F
) {
    internal var tileCanvasSize by mutableStateOf(Offset.Zero)
    internal var tileMapSize = Offset(256 * 2F, 256 * 2F)

    internal var angleDegrees by mutableStateOf(initialRotation)
    internal var zoom by mutableStateOf(initialZoom)
    internal val zoomLevel by derivedStateOf { floor(zoom).toInt() }
    internal val magnifierScale by derivedStateOf { zoom - zoomLevel + 1F }
    internal var rawPosition by mutableStateOf(initialPosition)
    internal val mapViewCenter by derivedStateOf { rawPosition + (tileCanvasSize / 2F) }

    private var boundHorizontal by mutableStateOf(boundHorizontal)
    private var boundVertical by mutableStateOf(boundVertical)
    private var maxZoom by mutableStateOf(maxZoom)
    private var minZoom by mutableStateOf(minZoom)

    fun move(offset: Offset) {
        rawPosition =
            (offset + rawPosition).coerceInCanvas(
                tileMapSize * 2F.pow(zoomLevel - 1) * magnifierScale,
                angleDegrees.degreesToRadian(),
                boundHorizontal,
                boundVertical
            )
    }

    fun scale(offset: Offset, scale: Float) {
        val previousMagnifierScale = magnifierScale
        zoom = (scale + zoom).coerceIn(minZoom, maxZoom)
        move(offset - mapViewCenter + ((mapViewCenter - offset) * magnifierScale / previousMagnifierScale))
    }

    fun rotate(offset: Offset, angle: Float) {
        if (offset != Offset.Zero) {
            angleDegrees += angle
            move(rotateVector(mapViewCenter - offset, angle.degreesToRadian()) + offset - mapViewCenter)
        }
    }

    private fun rotateVector(offset: Offset, angle: Float): Offset {
        return Offset(
            (offset.x * cos(angle) - offset.y * sin(angle)),
            (offset.x * sin(angle) + offset.y * cos(angle))
        )
    }

    private fun Float.degreesToRadian(): Float {
        return (this * PI / 180).toFloat()
    }

    private fun Offset.coerceInCanvas(gridSizeOnCanvas: Offset, angleRadian: Float, boundHorizontal: MapBorderType, boundVertical: MapBorderType): Offset {
        val unRotatedPosition = rotateVector(this, -angleRadian)
        val x = if (boundHorizontal == MapBorderType.BOUND) unRotatedPosition.x.coerceIn(
            -gridSizeOnCanvas.x,
            0F
        ) else (unRotatedPosition.x - gridSizeOnCanvas.x).rem(gridSizeOnCanvas.x)
        val y =
            if (boundVertical == MapBorderType.BOUND) unRotatedPosition.y.coerceIn(-gridSizeOnCanvas.y, 0F) else (unRotatedPosition.y - gridSizeOnCanvas.y).rem(
                gridSizeOnCanvas.y
            )
        return rotateVector(Offset(x, y), angleRadian)
    }
}

private fun Offset.pow(zoomLevel: Float): Offset {
    return Offset(this.x.pow(zoomLevel), this.y.pow(zoomLevel))
}

@Composable
inline fun rememberCameraState(
    crossinline init: MapState.() -> Unit = {}
): MapState = remember {
    MapState().apply(init)
}