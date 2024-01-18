package io.github.rafambn.kmap

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize

class CameraState(
    position: Offset = Offset.Zero,
    zoom: Float = 1F,
    rotation: Float = 0F
) {
    var mapSize: IntSize = IntSize.Zero
        set(value) {
            _rawPosition.value = latLongToRaw(position, value)
            field = value
        }

    internal val _rawPosition = mutableStateOf(latLongToRaw(position, mapSize))
    internal val _zoom = mutableStateOf(zoom)
    internal val _rotation = mutableStateOf(rotation)

    val tileSize = 256F

    var position
        get() = rawToLatLong(_rawPosition.value, mapSize)
        set(value) {
            _rawPosition.value = latLongToRaw(value, mapSize)
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
}

fun latLongToRaw(latLong: Offset, mapIntSize: IntSize): Offset {
    return latLong - Offset(256 * 5 / 2F, 256 * 5 / 2F) + Offset((mapIntSize.width / 2).toFloat(), (mapIntSize.height / 2).toFloat())
}

fun rawToLatLong(raw: Offset, mapIntSize: IntSize): Offset {
    return raw + Offset(256 * 5 / 2F, 256 * 5 / 2F) - Offset((mapIntSize.width / 2).toFloat(), (mapIntSize.height / 2).toFloat())
}

@Composable
inline fun rememberCameraState(
    crossinline init: CameraState.() -> Unit = {}
): CameraState = remember {
    CameraState().apply(init)
}
