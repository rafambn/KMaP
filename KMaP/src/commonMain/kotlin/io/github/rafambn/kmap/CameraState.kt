package io.github.rafambn.kmap

import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow


class CameraState(
    startPosition: Offset = Offset.Zero,
    zoom: Float = 0F,
    rotation: Float = 0F
) {
    private val _isMoving = MutableStateFlow(false)
    val isMoving = _isMoving.asStateFlow()

    private var _rawPosition = MutableStateFlow(startPosition)
    val rawPosition = _rawPosition.asStateFlow()

    private var _rawZoom = MutableStateFlow(zoom)
    val rawZoom = _rawZoom.asStateFlow()

    private var _rawRotation = MutableStateFlow(rotation)
    val rawRotation = _rawRotation.asStateFlow()

    var position: Offset
        get() = _rawPosition.value
        set(value) {
            _rawPosition.value = value
        }

    var zoom: Float
        get() = _rawZoom.value
        set(value) {
            _rawZoom.value = value.coerceAtLeast(1F)
        }

    var rotation: Float
        get() = _rawRotation.value
        set(value) {
            _rawRotation.value = value
        }
}