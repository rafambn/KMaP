package io.github.rafambn.kmap

enum class MapComponentType {
    CANVAS,
    MARKER,
    PATH
}

data class MapComponent(
    val position: Position,
    val zIndex: Float,
    val drawPosition: DrawPosition,
    val mapComponentType: MapComponentType
)