package com.rafambn.kmap.model

import com.rafambn.kmap.utils.offsets.CanvasPosition

data class BoundingBox(
    val topLeft: CanvasPosition,
    val topRight: CanvasPosition,
    val bottomRight: CanvasPosition,
    val bottomLeft: CanvasPosition
) //TODO(5) add methods to it