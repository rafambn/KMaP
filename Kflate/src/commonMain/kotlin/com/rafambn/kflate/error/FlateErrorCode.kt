package com.rafambn.kflate.error

enum class FlateErrorCode(val code: Int, val message: String) {
    UNEXPECTED_EOF(0, "unexpected EOF"),
    INVALID_BLOCK_TYPE(1, "invalid block type"),
    INVALID_LENGTH_LITERAL(2, "invalid length/literal"),
    INVALID_DISTANCE(3, "invalid distance"),
    INVALID_HEADER(4, "invalid header data"),
}

