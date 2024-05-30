package io.github.rafambn.kmap

import io.github.rafambn.kmap.utils.CanvasPosition

enum class MapComponentType {
    CANVAS,
    MARKER,
    PATH
}

data class MapComponent(
    val position: CanvasPosition,
    val zIndex: Float,
    val drawPosition: DrawPosition,
    val mapComponentType: MapComponentType
)