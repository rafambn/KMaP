package com.rafambn.kmap.gesture

import com.rafambn.kmap.geometry.plane.ProjectedCoordinates

data class PathGestureWrapper(
    val onTap: ((ProjectedCoordinates) -> Unit)? = null,
    val onDoubleTap: ((ProjectedCoordinates) -> Unit)? = null,
    val onLongPress: ((ProjectedCoordinates) -> Unit)? = null,
    val onHover: ((ProjectedCoordinates) -> Unit)? = null,
)
