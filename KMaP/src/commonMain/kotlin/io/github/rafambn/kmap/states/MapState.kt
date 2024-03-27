package io.github.rafambn.kmap.states

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.drawscope.scale
import io.github.rafambn.kmap.degreesToRadian
import io.github.rafambn.kmap.enums.MapBorderType
import io.github.rafambn.kmap.model.Position
import io.github.rafambn.kmap.rotateVector
import io.github.rafambn.kmap.toCanvasReference
import io.github.rafambn.kmap.toMapReference
import io.github.rafambn.kmap.toOffset
import kotlin.math.floor
import kotlin.math.pow

class MapState(
    initialPosition: Position = Position.Zero,
    initialZoom: Float = 0F,
    initialRotation: Float = 0F,
    maxZoom: Int = 19,
    minZoom: Int = 0,
    val mapProperties: MapProperties = MapProperties(zoomLevels =  OSMZoomlevelsRange, mapCoordinatesRange =  OSMCoordinatesRange)
) {
    private var maxZoom by mutableStateOf(maxZoom.coerceIn(mapProperties.zoomLevels.min, mapProperties.zoomLevels.max))
    private var minZoom by mutableStateOf(minZoom.coerceIn(mapProperties.zoomLevels.min, mapProperties.zoomLevels.max))

    internal var zoom by mutableStateOf(initialZoom)
    internal val zoomLevel by derivedStateOf { floor(zoom).toInt() }
    internal val magnifierScale by derivedStateOf { zoom - zoomLevel + 1.0 }

    internal var angleDegrees by mutableStateOf(initialRotation)

    internal var canvasSize by mutableStateOf(Position.Zero)
    internal var mapSize = Position(TileCanvasState.MAP_SIZE, TileCanvasState.MAP_SIZE)

    internal var mapPosition by mutableStateOf(initialPosition)
    internal val topLeftCanvas by derivedStateOf {
        mapPosition.toCanvasReference(magnifierScale, zoomLevel, angleDegrees) + canvasSize / 2.0
    }
    internal val matrix by derivedStateOf {
        val matrix = Matrix()
        matrix.translate(topLeftCanvas.horizontal.toFloat(), topLeftCanvas.vertical.toFloat(), 0F)
        matrix.rotateZ(angleDegrees)
        matrix.scale(magnifierScale.toFloat(), magnifierScale.toFloat(), 0F)
        matrix
    }

    fun move(position: Position) {
        println(position.toMapReference(magnifierScale, zoomLevel, angleDegrees))
        mapPosition = (position.toMapReference(magnifierScale, zoomLevel, angleDegrees) + mapPosition).coerceInMap()
    }

    fun scale(position: Position, scale: Float) {
        if (scale != 0F) {
            val previousMagnifierScale = magnifierScale
            val previousZoomLevel = zoomLevel
            zoom = (scale + zoom).coerceIn(minZoom.toFloat(), maxZoom.toFloat())
            move((position - canvasSize / 2.0) * (1 - ((magnifierScale / previousMagnifierScale) * (2.0.pow(zoomLevel - previousZoomLevel)))))
        }
    }

    fun rotate(position: Position, angle: Float) {
        if (position != Position.Zero) {
            angleDegrees += angle
            move((position - (canvasSize / 2.0) + ((canvasSize / 2.0) - position).rotateVector(angle.degreesToRadian())))
        }
    }

    private fun Position.coerceInMap(): Position {
        val x = if (mapProperties.boundMap.horizontal == MapBorderType.BOUND) this.horizontal.coerceIn(-mapSize.horizontal, 0.0)
        else (this.horizontal - mapSize.horizontal).rem(mapSize.horizontal)
        val y = if (mapProperties.boundMap.vertical == MapBorderType.BOUND) this.vertical.coerceIn(-mapSize.vertical, 0.0)
        else (this.horizontal - mapSize.vertical).rem(mapSize.vertical)
        return Position(x, y)
    }
}

@Composable
inline fun rememberCameraState(
    crossinline init: MapState.() -> Unit = {}
): MapState = remember {
    MapState().apply(init)
}