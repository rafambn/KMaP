package io.github.rafambn.kmap

import io.github.rafambn.kmap.utils.CanvasPosition

data class BoundingBox(val topLeft: CanvasPosition, val topRight: CanvasPosition, val bottomRight: CanvasPosition, val bottomLeft: CanvasPosition)
