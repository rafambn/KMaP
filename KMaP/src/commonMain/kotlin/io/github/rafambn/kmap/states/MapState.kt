package io.github.rafambn.kmap.states

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import io.github.rafambn.kmap.enums.MapBorderType
import io.github.rafambn.kmap.enums.OutsideBorderType
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
    outsideBorderType: OutsideBorderType = OutsideBorderType.NONE,
    maxZoom: Float = 10F,
    minZoom: Float = 1F
) {
    internal var tileCanvasSize by mutableStateOf(Offset.Zero)
    internal var tileMapSize = Offset(TileCanvasState.tileSize * 2F.pow(maxZoom), TileCanvasState.tileSize * 2F.pow(maxZoom))

    internal var angleDegrees by mutableStateOf(initialRotation)

    internal var zoom by mutableStateOf(initialZoom)
    internal val zoomLevel by derivedStateOf { floor(zoom).toInt() }
    internal val magnifierScale by derivedStateOf { zoom - zoomLevel + 1F }

    internal var rawPosition by mutableStateOf(initialPosition)
    internal val scaledPosition by derivedStateOf { rawPosition * magnifierScale / 2F.pow(maxZoom - zoomLevel) }
    internal val gridCenterOffset by derivedStateOf { scaledPosition.rotateVector(angleDegrees.degreesToRadian()) + (tileCanvasSize / 2F) }

    private var boundHorizontal by mutableStateOf(boundHorizontal)
    private var boundVertical by mutableStateOf(boundVertical)
    private var maxZoom by mutableStateOf(maxZoom)
    private var minZoom by mutableStateOf(minZoom)

    fun move(offset: Offset) {
        rawPosition =
            ((offset * 2F.pow(maxZoom - zoomLevel) / magnifierScale).rotateVector(-angleDegrees.degreesToRadian()) + rawPosition).coerceInCanvas(
                tileMapSize, boundHorizontal, boundVertical
            )
    }

    fun scale(offset: Offset, scale: Float) {
        val previousMagnifierScale = magnifierScale
        zoom = (scale + zoom).coerceIn(minZoom, maxZoom)
//        move((offset - gridCenterOffset + ((gridCenterOffset - offset) * magnifierScale / previousMagnifierScale)) * magnifierScale / 2F.pow(maxZoom - zoomLevel))
    }

    fun rotate(offset: Offset, angle: Float) {
        if (offset != Offset.Zero) {
            angleDegrees += angle
            move((offset - (tileCanvasSize / 2F) + ((tileCanvasSize / 2F) - offset).rotateVector(angle.degreesToRadian())))
        }
    }

    private fun Offset.coerceInCanvas(
        gridSizeOnCanvas: Offset, boundHorizontal: MapBorderType, boundVertical: MapBorderType
    ): Offset {
        val x = if (boundHorizontal == MapBorderType.BOUND) this.x.coerceIn(-gridSizeOnCanvas.x, 0F)
        else (this.x - gridSizeOnCanvas.x).rem(gridSizeOnCanvas.x)
        val y = if (boundVertical == MapBorderType.BOUND) this.y.coerceIn(-gridSizeOnCanvas.y, 0F)
        else (this.y - gridSizeOnCanvas.y).rem(gridSizeOnCanvas.y)
        return Offset(x, y)
    }
}

@Composable
inline fun rememberCameraState(
    crossinline init: MapState.() -> Unit = {}
): MapState = remember {
    MapState().apply(init)
}

fun Float.degreesToRadian(): Float {
    return (this * PI / 180).toFloat()
}

fun Offset.rotateVector(angleRadians: Float): Offset {
    return Offset(
        (this.x * cos(angleRadians) - this.y * sin(angleRadians)), (this.x * sin(angleRadians) + this.y * cos(angleRadians))
    )
}