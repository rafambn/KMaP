package io.github.rafambn.kmap

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize

class CameraState(
    private val initialPosition: Offset = Offset.Zero,
    zoom: Float = 1F,
    rotation: Float = 0F
) {
    var mapSize: IntSize = IntSize.Zero
        set(value) {
            _tileSize.value = minOf(value.height, value.width).toFloat()
            if (field == IntSize.Zero)
                _rawPosition.value = initialPosition.toRaw(value)
            else
                _rawPosition.value = position.toRaw(value)
            field = value
        }

    internal val _tileSize = mutableStateOf(minOf(mapSize.height, mapSize.width).toFloat())
    internal val _zoom = mutableStateOf(zoom)
    internal val _rotation = mutableStateOf(rotation)
    internal val _rawPosition = mutableStateOf(initialPosition)

    var position
        get() = _rawPosition.value.toLatLong(mapSize)
        set(value) {
            _rawPosition.value = value.toRaw(mapSize)
        }
    var zoom
        get() = _zoom.value
        set(value) {
            _zoom.value = value.coerceAtLeast(1F)
        }
    var rotation
        get() = _rotation.value
        set(value) {
            _rotation.value = value
        }

    private fun Offset.toRaw(mapIntSize: IntSize): Offset {
        return this - Offset(_tileSize.value * zoom / 2, _tileSize.value * zoom / 2) + Offset(
            (mapIntSize.width / 2).toFloat(),
            (mapIntSize.height / 2).toFloat()
        )
    }

    private fun Offset.toLatLong(mapIntSize: IntSize): Offset {
        return this + Offset(_tileSize.value * zoom / 2, _tileSize.value * zoom / 2) - Offset(
            (mapIntSize.width / 2).toFloat(),
            (mapIntSize.height / 2).toFloat()
        )
    }
}


@Composable
inline fun rememberCameraState(
    crossinline init: CameraState.() -> Unit = {}
): CameraState = remember {
    CameraState().apply(init)
}
