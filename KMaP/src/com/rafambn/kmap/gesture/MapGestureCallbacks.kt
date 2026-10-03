package com.rafambn.kmap.gesture

import com.rafambn.kmap.geometry.angle.Degrees
import com.rafambn.kmap.geometry.plane.DifferentialScreenOffset
import com.rafambn.kmap.geometry.plane.ScreenOffset

/**
 * Optional handlers invoked synchronously by the canvas's pointer input detector.
 * Positions and pan deltas use local canvas pixels, with positive axes pointing right and down.
 * Rotation deltas are in degrees, positive clockwise. Zoom factors multiply the current scale:
 * `1f` leaves it unchanged, `2f` doubles it, and `0.5f` halves it.
 *
 * Null leaves a callback unregistered; registration affects gesture recognition. The detector
 * observes input without consuming pointer changes, and handlers must explicitly update the camera.
 *
 * @property onTap Called on release, unless a second-tap handler is registered. In that case,
 * recognition waits for the double-tap timeout or movement beyond touch slop; a recognized
 * double tap, tap-long-press, or tap-swipe replaces the single tap.
 * @property onDoubleTap Called when a second press starts within the double-tap timeout and
 * releases before the long-press timeout.
 * @property onLongPress Called when the initial press reaches the long-press timeout.
 * @property onTapLongPress Called after a tap followed by a second press held to the long-press timeout.
 * @property onTapSwipe Called while dragging the second press after a tap. Zoom is the ratio of
 * current to previous distance from the canvas center; rotation is also around that center.
 * Zoom is `1f` when either distance is zero.
 * @property onTransform Called during pan, pinch, or rotation. The centroid is the previous
 * position of pointers present in both events. Apply zoom and rotation around it, then pan.
 * Zoom is `1f` for a single pointer or when either pinch radius is zero.
 * @property onTwoFingerTap Reports the position of the final released pointer.
 * @property onHover Reports pointer movement while the detector is in its hover state.
 * @property onScroll Reports the vertical Compose scroll delta without normalization. Positive
 * values mean scrolling down. Units depend on the platform and input device, not canvas pixels;
 * horizontal scrolling is ignored.
 */
data class MapGestureCallbacks(
    // common use
    val onTap: ((position: ScreenOffset) -> Unit)? = null,
    val onDoubleTap: ((position: ScreenOffset) -> Unit)? = null,
    val onLongPress: ((position: ScreenOffset) -> Unit)? = null,
    val onTapLongPress: ((position: ScreenOffset) -> Unit)? = null,
    val onTapSwipe: ((zoomFactor: Float, rotationDelta: Degrees) -> Unit)? = null,
    val onTransform: ((
        centroid: ScreenOffset,
        panDelta: DifferentialScreenOffset,
        zoomFactor: Float,
        rotationDelta: Degrees,
    ) -> Unit)? = null,

    // mobile use
    val onTwoFingerTap: ((position: ScreenOffset) -> Unit)? = null,

    // jvm/web use
    val onHover: ((position: ScreenOffset) -> Unit)? = null,
    val onScroll: ((position: ScreenOffset, scrollDeltaY: Float) -> Unit)? = null,
)
