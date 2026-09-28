package com.rafambn.kmap.style

import com.rafambn.kmap.style.expression.parseColor
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull

internal enum class StylePropertyType(val description: String) {
    COLOR("a color"),
    NUMBER("a number"),
    BOOLEAN("a boolean"),
    STRING("a string"),
    NUMBER_ARRAY("an array of numbers"),
    STRING_ARRAY("an array of strings");

    fun acceptsLiteral(value: JsonElement): Boolean = when (this) {
        COLOR -> value is JsonPrimitive && value.isString && parseColor(value.content) != null
        NUMBER -> value.isNumber()
        BOOLEAN -> value is JsonPrimitive && !value.isString && value.booleanOrNull != null
        STRING -> value is JsonPrimitive && value.isString
        NUMBER_ARRAY -> value is JsonArray && value.all { it.isNumber() }
        STRING_ARRAY -> value is JsonArray && value.all { it is JsonPrimitive && it.isString }
    }
}

private fun JsonElement.isNumber(): Boolean =
    this is JsonPrimitive && !isString && doubleOrNull?.isFinite() == true
