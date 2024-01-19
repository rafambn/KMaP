package io.github.rafambn.kmap

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

class MapProperties(
    boundHorizontal: Boolean = false,
    boundVertical: Boolean = true,
    loopHorizontal: Boolean = true,
    loopVertical: Boolean = true,
    maxZoom: Float = 10F,
    minZoom: Float = 1F
) {
    private val _boundHorizontal = mutableStateOf(boundHorizontal)
    private val _boundVertical = mutableStateOf(boundVertical)
    private val _loopHorizontal = mutableStateOf(loopHorizontal)
    private val _loopVertical = mutableStateOf(loopVertical)
    private val _maxZoom = mutableStateOf(maxZoom)
    private val _minZoom = mutableStateOf(minZoom)

    var boundHorizontal: Boolean
        get() = _boundHorizontal.value
        set(value) {
            _boundHorizontal.value = value
        }

    var boundVertical: Boolean
        get() = _boundVertical.value
        set(value) {
            _boundVertical.value = value
        }

    var loopHorizontal: Boolean
        get() = _loopHorizontal.value
        set(value) {
            _loopHorizontal.value = value
        }

    var loopVertical: Boolean
        get() = _loopVertical.value
        set(value) {
            _loopVertical.value = value
        }

    var maxZoom: Float
        get() = _maxZoom.value
        set(value) {
            _maxZoom.value = value
        }

    var minZoom: Float
        get() = _minZoom.value
        set(value) {
            _minZoom.value = value
        }
}



@Composable
inline fun rememberMapProperties(
    crossinline init: MapProperties.() -> Unit = {}
): MapProperties = remember {
    MapProperties().apply(init)
}