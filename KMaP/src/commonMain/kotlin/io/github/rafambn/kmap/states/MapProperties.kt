package io.github.rafambn.kmap.states

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import io.github.rafambn.kmap.enums.MapBorderType

class MapProperties(
    boundHorizontal: MapBorderType = MapBorderType.NONE,
    boundVertical: MapBorderType = MapBorderType.NONE,
    maxZoom: Float = 10F,
    minZoom: Float = 1F
) {
    private val _boundHorizontal = mutableStateOf(boundHorizontal)
    private val _boundVertical = mutableStateOf(boundVertical)
    private val _maxZoom = mutableStateOf(maxZoom)
    private val _minZoom = mutableStateOf(minZoom)

    var boundHorizontal: MapBorderType
        get() = _boundHorizontal.value
        set(value) {
            _boundHorizontal.value = value
        }

    var boundVertical: MapBorderType
        get() = _boundVertical.value
        set(value) {
            _boundVertical.value = value
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