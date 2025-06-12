package com.rafambn.kmap.gestures


/**
 * [MapGestureState] is used to indicate what is the current state that the map gesture is.
 */
enum class MapGestureState {
    GESTURE,
    TAP_SWIPE,
    HOVER,
    WAITING_UP,
    WAITING_UP_AFTER_TAP,
    WAITING_UP_AFTER_TWO_PRESS,
    WAITING_DOWN
}
/**
 * [PathGestureState] is used to indicate what is the current state that the path gesture is.
 */
enum class PathGestureState {
    WAITING_UP,
    WAITING_DOWN,
    WAITING_DOWN_AFTER_TAP,
    WAITING_UP_AFTER_TAP,
}
