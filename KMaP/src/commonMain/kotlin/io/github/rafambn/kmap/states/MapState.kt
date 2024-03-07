package io.github.rafambn.kmap.states

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import io.github.rafambn.kmap.degreesToRadian
import io.github.rafambn.kmap.enums.MapBorderType
import io.github.rafambn.kmap.rotateVector
import kotlin.math.floor
import kotlin.math.pow

class MapState(
    initialPosition: Offset = Offset.Zero,
    initialZoom: Float = 0F,
    initialRotation: Float = 0F,
    maxZoom: Float = 10F,
    minZoom: Float = 0F,
    private val mapProperties: MapProperties = MapProperties()
) {
    private var maxZoom by mutableStateOf(maxZoom.coerceIn(mapProperties.minMapZoom, mapProperties.maxMapZoom))
    private var minZoom by mutableStateOf(minZoom.coerceIn(mapProperties.minMapZoom, mapProperties.maxMapZoom))

    internal var zoom by mutableStateOf(initialZoom)
    internal val zoomLevel by derivedStateOf { floor(zoom).toInt() }
    internal val magnifierScale by derivedStateOf { zoom - zoomLevel + 1F }

    internal var angleDegrees by mutableStateOf(initialRotation)

    internal var canvasSize by mutableStateOf(Offset.Zero)
    internal var mapSize =
        Offset(TileCanvasState.tileSize * 2F.pow(mapProperties.maxMapZoom), TileCanvasState.tileSize * 2F.pow(mapProperties.maxMapZoom))

    internal var mapPosition by mutableStateOf(initialPosition)
    internal val topLeftCanvas by derivedStateOf { mapPosition.toCanvasReference() + canvasSize / 2F }

    fun move(offset: Offset) {
        mapPosition = (offset.toMapReference() + mapPosition).coerceInCanvas()
    }

    fun scale(offset: Offset, scale: Float) {
        if (scale != 0F) {
            val previousMagnifierScale = magnifierScale
            val previousZoomLevel = zoomLevel
            zoom = (scale + zoom).coerceIn(minZoom, maxZoom)
            move((offset - canvasSize / 2F) * (1 - ((magnifierScale / previousMagnifierScale) * (2F.pow(zoomLevel - previousZoomLevel)))))
        }
    }

    fun rotate(offset: Offset, angle: Float) {
        if (offset != Offset.Zero) {
            angleDegrees += angle
            move((offset - (canvasSize / 2F) + ((canvasSize / 2F) - offset).rotateVector(angle.degreesToRadian())))
        }
    }

    private fun Offset.coerceInCanvas(): Offset {
        val x = if (mapProperties.boundMap.horizontal == MapBorderType.BOUND) this.x.coerceIn(-mapSize.x, 0F)
        else (this.x - mapSize.x).rem(mapSize.x)
        val y = if (mapProperties.boundMap.vertical == MapBorderType.BOUND) this.y.coerceIn(-mapSize.y, 0F)
        else (this.y - mapSize.y).rem(mapSize.y)
        return Offset(x, y)
    }

    private fun Offset.toCanvasReference(): Offset {
        return (this * magnifierScale / 2F.pow(maxZoom - zoomLevel)).rotateVector(angleDegrees.degreesToRadian())
    }

    private fun Offset.toMapReference(): Offset {
        return (this * 2F.pow(maxZoom - zoomLevel) / magnifierScale).rotateVector(-angleDegrees.degreesToRadian())
    }
}

@Composable
inline fun rememberCameraState(
    crossinline init: MapState.() -> Unit = {}
): MapState = remember {
    MapState().apply(init)
}