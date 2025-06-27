package com.rafambn.kmap.gestures

import com.rafambn.kmap.utils.DifferentialScreenOffset
import com.rafambn.kmap.utils.ScreenOffset

data class MapGestureWrapper(
    // common use
    val onTap: ((screenOffset: ScreenOffset) -> Unit)? = null,
    val onDoubleTap: ((screenOffset: ScreenOffset) -> Unit)? = null,
    val onLongPress: ((screenOffset: ScreenOffset) -> Unit)? = null,
    val onTapLongPress: ((screenOffset: ScreenOffset) -> Unit)? = null,
    val onTapSwipe: ((zoomChange: Float, rotationChange: Double) -> Unit)? = null,
    val onGesture: ((screenOffset: ScreenOffset, screenOffsetDiff: DifferentialScreenOffset, zoom: Float, rotation: Float) -> Unit)? = null,

    // mobile use
    val onTwoFingersTap: ((screenOffset: ScreenOffset) -> Unit)? = null,

    // jvm/web use
    val onHover: ((screenOffset: ScreenOffset) -> Unit)? = null,
    val onScroll: ((screenOffset: ScreenOffset, scrollAmount: Float) -> Unit)? = null,
)
