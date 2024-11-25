package com.rafambn.kmap.gestures


/**
 * [GestureChangeState] is use to indicate what type of change happened from the last input to the new.
 */
enum class GestureChangeState {
    PRESS,
    RELEASE,
    TWO_PRESS,
    TWO_RELEASE,
    CTRL_PRESS,
    CTRL_RELEASE,
}

/**
 * [GestureState] is used to indicate what is the current state that the gesture is.
 */
enum class GestureState {
    DRAG,
    CTRL,
    MOBILE,
    TAP_LONG_PRESS,
    TAP_SWIPE,
    HOVER,
    WAITING_UP,
    WAITING_UP_AFTER_TAP,
    WAITING_UP_AFTER_TWO_PRESS,
    WAITING_UP_AFTER_TWO_RELEASE,
    WAITING_UP_AFTER_MOBILE_RELEASE,
    WAITING_DOWN
}