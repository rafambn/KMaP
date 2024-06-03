package io.github.rafambn.kmap.model

import io.github.rafambn.kmap.utils.offsets.CanvasPosition

data class BoundingBox(
    val topLeft: CanvasPosition,
    val topRight: CanvasPosition,
    val bottomRight: CanvasPosition,
    val bottomLeft: CanvasPosition
)