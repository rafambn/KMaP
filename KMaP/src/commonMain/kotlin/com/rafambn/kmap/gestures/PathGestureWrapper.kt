package com.rafambn.kmap.gestures

import com.rafambn.kmap.utils.ProjectedCoordinates

data class PathGestureWrapper(
    val onTap: ((ProjectedCoordinates) -> Unit)? = null,
    val onDoubleTap: ((ProjectedCoordinates) -> Unit)? = null,
    val onLongPress: ((ProjectedCoordinates) -> Unit)? = null,
    val onHover: ((ProjectedCoordinates) -> Unit)? = null,
)
