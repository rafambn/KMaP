package com.rafambn.kmap.gestures


/**
 * [GestureState] is used to indicate what is the current state that the gesture is.
 */
enum class GestureState {
    DRAG,
    CTRL,
    MOBILE,
    TAP_SWIPE,
    HOVER,
    WAITING_UP,
    WAITING_UP_AFTER_TAP,
    WAITING_UP_AFTER_TWO_PRESS,
    WAITING_DOWN
}