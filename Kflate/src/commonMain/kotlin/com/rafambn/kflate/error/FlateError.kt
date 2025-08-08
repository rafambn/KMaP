package com.rafambn.kflate.error

class FlateError(code: FlateErrorCode) : Exception(code.message)

fun createFlateError(errorCode: FlateErrorCode): Nothing {
    throw FlateError(errorCode)
}
