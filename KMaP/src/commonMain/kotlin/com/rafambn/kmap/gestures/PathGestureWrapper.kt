package com.rafambn.kmap.gestures

import com.rafambn.kmap.utils.ScreenOffset

data class PathGestureWrapper(
    val onTap: ((ScreenOffset) -> Unit)? = null,
    val onDoubleTap: ((ScreenOffset) -> Unit)? = null,
    val onLongPress: ((ScreenOffset) -> Unit)? = null,
)
