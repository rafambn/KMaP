package io.github.rafambn.kmap

enum class GestureChangeState {
    PRESS,
    RELEASE,
    TWO_PRESS,
    TWO_RELEASE,
    CTRL_PRESS,
    CTRL_RELEASE,
}

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
    WAITING_DOWN
}