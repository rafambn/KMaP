package io.github.rafambn.kmap.states

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Matrix
import io.github.rafambn.kmap.enums.MapBorderType
import io.github.rafambn.kmap.utils.invertFisrt
import io.github.rafambn.kmap.utils.invertSecond
import io.github.rafambn.kmap.utils.loopInRange
import io.github.rafambn.kmap.model.Position
import io.github.rafambn.kmap.model.VeiwPort
import io.github.rafambn.kmap.ranges.MapCoordinatesRange
import io.github.rafambn.kmap.utils.rotate
import io.github.rafambn.kmap.utils.toCanvasReference
import io.github.rafambn.kmap.utils.toMapReference
import io.github.rafambn.kmap.utils.toPosition
import io.github.rafambn.kmap.utils.toRadians
import io.github.rafambn.kmap.utils.toViewportReference
import kotlin.math.floor
import kotlin.math.pow

class MapState(
    initialPosition: Position = Position.Zero,
    initialZoom: Float = 0F,
    initialRotation: Float = 0F,
    maxZoom: Int = 19,
    minZoom: Int = 0,
    val mapProperties: MapProperties = MapProperties(zoomLevels = OSMZoomlevelsRange, mapCoordinatesRange = OSMCoordinatesRange)
) {
    private var maxZoom by mutableStateOf(maxZoom.coerceIn(mapProperties.zoomLevels.min, mapProperties.zoomLevels.max))
    private var minZoom by mutableStateOf(minZoom.coerceIn(mapProperties.zoomLevels.min, mapProperties.zoomLevels.max))

    internal var zoom by mutableStateOf(initialZoom)
    internal var angleDegrees by mutableStateOf(initialRotation)
    internal var mapPosition by mutableStateOf(initialPosition)

    internal val zoomLevel
        get() = floor(zoom).toInt()
    private val magnifierScale
        get() = zoom - zoomLevel + 1F
    private var canvasSize by mutableStateOf(Offset.Zero)

    internal val positionOffset by derivedStateOf {
        mapPosition.toCanvasReference(zoomLevel, mapProperties.mapCoordinatesRange)
    }

    internal val viewPort by derivedStateOf {
        canvasSize.toPosition().toViewportReference(magnifierScale, zoomLevel, angleDegrees.toDouble(), OSMCoordinatesRange, mapPosition)
    }

    internal val matrix by derivedStateOf {
        val matrix = Matrix()
        matrix.translate(canvasSize.x / 2, canvasSize.y / 2, 0F)
        matrix.rotateZ(angleDegrees)
        matrix.scale(magnifierScale, magnifierScale, 0F)
        matrix
    }

    fun move(position: Position) { //TODO add user interface
        mapPosition =
            (position.toMapReference(magnifierScale, zoomLevel, angleDegrees.toDouble(), mapProperties.mapCoordinatesRange) + mapPosition).coerceInMap()
    }

    fun scale(position: Position, scale: Float) {
        if (scale != 0F) {
            val previousMagnifierScale = magnifierScale
            val previousZoomLevel = zoomLevel
            zoom = (scale + zoom).coerceIn(minZoom.toFloat(), maxZoom.toFloat())
            move((position - canvasSize.toPosition() / 2.0) * (1 - ((magnifierScale / previousMagnifierScale) * (2.0.pow(zoomLevel - previousZoomLevel)))))
        }
    }

    fun rotate(position: Position, angle: Float) {
        if (position != Position.Zero) {
            angleDegrees += angle
            move(position - (canvasSize.toPosition() / 2.0) + ((canvasSize.toPosition() / 2.0) - position).rotate(angle.toDouble().toRadians()))
        }
    }

    private fun Position.coerceInMap(): Position {
        val x = if (mapProperties.boundMap.horizontal == MapBorderType.BOUND)
            horizontal.coerceIn(
                mapProperties.mapCoordinatesRange.longitute.west,
                mapProperties.mapCoordinatesRange.longitute.east
            )
        else
            horizontal.loopInRange(mapProperties.mapCoordinatesRange.longitute)
        val y = if (mapProperties.boundMap.vertical == MapBorderType.BOUND)
            vertical.coerceIn(
                mapProperties.mapCoordinatesRange.latitude.south,
                mapProperties.mapCoordinatesRange.latitude.north
            )
        else
            vertical.loopInRange(mapProperties.mapCoordinatesRange.latitude)
        return Position(x, y)
    }

    fun updateCanvasSize(canvasSize: Offset) {
        this.canvasSize = canvasSize
    }
}

@Composable
inline fun rememberCameraState(
    crossinline init: MapState.() -> Unit = {}
): MapState = remember {
    MapState().apply(init)
}