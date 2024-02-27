package io.github.rafambn.kmap.states

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntSize
import io.github.rafambn.kmap.enums.MapBorderType
import kotlin.math.PI
import kotlin.math.cos
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
    internal var mapViewSize by mutableStateOf(IntSize.Zero)
    internal var angleDegrees by mutableStateOf(initialRotation)
    internal var zoom by mutableStateOf(initialZoom)
    internal var rawPosition by mutableStateOf(initialPosition)
    var boundHorizontal by mutableStateOf(boundHorizontal)
    var boundVertical by mutableStateOf(boundVertical)
    var maxZoom by mutableStateOf(maxZoom)
    var minZoom by mutableStateOf(minZoom)

    private var angleRadian = 0F
    private var gridSize = Size(256 * 3F, 256 * 3F)

    fun move(offset: Offset) {
        rawPosition += offset
    }

    fun scale(offset: Offset, scale: Float) {
        val previousZoom = zoom
        zoom = (scale + zoom).coerceIn(minZoom, maxZoom)
        rawPosition = offset + ((rawPosition - offset) * zoom / previousZoom)
    }

    fun rotate(offset: Offset, angle: Float) {
        if (offset != Offset.Zero) {
            val tempRadianAngle = (angle * PI / 180).toFloat()
            angleDegrees += angle
            angleRadian = (angleDegrees * PI / 180).toFloat()
            rawPosition = rotateVector(rawPosition - offset, tempRadianAngle) + offset
        }
    }

    private fun rotateVector(offset: Offset, angle: Float): Offset {
        return Offset(
            (offset.x * cos(angle) - offset.y * sin(angle)),
            (offset.x * sin(angle) + offset.y * cos(angle))
        )
    }
}

@Composable
inline fun rememberCameraState(
    crossinline init: MapState.() -> Unit = {}
): MapState = remember {
    MapState().apply(init)
}