package io.github.rafambn.kmap.gestures


/**
 * [GestureChangeState] is use to indicate what type of change happened from the last input to the new.
 */
enum class GestureChangeState { //TODO Verify if its possible to use only the parameters of PointerEvent to represent this states
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
    TAP_MAP,
    HOVER,
    WAITING_UP,
    WAITING_UP_AFTER_TAP,
    WAITING_UP_AFTER_TWO_PRESS,
    WAITING_DOWN
}