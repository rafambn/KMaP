package com.rafambn.kmapdemo.model

import com.rafambn.kmapdemo.utils.offsets.CanvasPosition

data class BoundingBox(
    val topLeft: CanvasPosition,
    val topRight: CanvasPosition,
    val bottomRight: CanvasPosition,
    val bottomLeft: CanvasPosition
) //TODO(5) add methods to it