package com.rafambn.kmap.style

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder

object SpriteElementSerializer : KSerializer<JsonElement> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("SpriteElement", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): JsonElement = when (decoder) {
        is JsonDecoder -> decoder.decodeJsonElement()
        else -> Json.parseToJsonElement(decoder.decodeString())
    }

    override fun serialize(encoder: Encoder, value: JsonElement) {
        when (encoder) {
            is JsonEncoder -> encoder.encodeJsonElement(value)
            else -> encoder.encodeString(value.toString())
        }
    }
}
