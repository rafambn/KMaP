package com.rafambn.kmap.source.preparation

import kotlinx.serialization.Serializable

/** Numeric tags preserve MVT signed, unsigned and floating-point property semantics. */
@Serializable
data class PreparedValue(val type: String, val value: String) {
    fun toValue(): Any = when (type) {
        "string" -> value
        "boolean" -> value.toBooleanStrict()
        "int" -> value.toInt()
        "long" -> value.toLong()
        "ulong" -> value.toULong()
        "float" -> value.toFloat()
        "double" -> value.toDouble()
        else -> error("Unsupported property type: $type")
    }
}

internal fun preparedValue(value: Any) = PreparedValue(when (value) {
    is String -> "string"
    is Boolean -> "boolean"
    is Int -> "int"
    is Long -> "long"
    is ULong -> "ulong"
    is Float -> "float"
    is Double -> "double"
    else -> error("Unsupported vector property: ${value::class}")
}, value.toString())
