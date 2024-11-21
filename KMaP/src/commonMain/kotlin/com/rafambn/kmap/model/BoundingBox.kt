package com.rafambn.kmap.model

import com.rafambn.kmap.utils.CanvasPosition

data class BoundingBox(
    val topLeft: CanvasPosition,
    val topRight: CanvasPosition,
    val bottomRight: CanvasPosition,
    val bottomLeft: CanvasPosition
)