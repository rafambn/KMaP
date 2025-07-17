@file:OptIn(ExperimentalSerializationApi::class)

package com.rafambn.mvtparser

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
data class Tile(
    @ProtoNumber(3) // Field number from .proto
    val layers: List<Layer> = emptyList()
) {
    @Serializable
    data class Layer(
        @ProtoNumber(1) // Field number from .proto
        val name: String,
        @ProtoNumber(2)
        val features: List<Feature> = emptyList(),
        @ProtoNumber(3)
        val keys: List<String> = emptyList(),
        @ProtoNumber(4)
        val values: List<Value> = emptyList(),
        @ProtoNumber(5)
        val extent: UInt = 4096u, // Default value from .proto
        @ProtoNumber(15)
        val version: UInt = 1u // Default value from .proto
    )

    @Serializable
    data class Feature(
        @ProtoNumber(1)
        val id: ULong? = null,
        @ProtoNumber(2)
        val tags: List<UInt> = emptyList(), // Raw indices
        @ProtoNumber(3)
        val type: GeomType = GeomType.UNKNOWN,
        @ProtoNumber(4)
        val geometry: List<UInt> = emptyList() // Raw commands/parameters
    )

    @Serializable
    data class Value(
        // This will be a "oneof" mapping in KotlinX Serialization, like:
        @ProtoNumber(1) val stringValue: String? = null,
        @ProtoNumber(2) val floatValue: Float? = null,
        @ProtoNumber(3) val doubleValue: Double? = null,
        @ProtoNumber(4) val intValue: Long? = null,
        @ProtoNumber(5) val uintValue: ULong? = null,
        @ProtoNumber(6) val sintValue: Long? = null,
        @ProtoNumber(7) val boolValue: Boolean? = null
    )

    // Enum mapping for GeomType
    @Serializable
    enum class GeomType(val value: Int) {
        UNKNOWN(0),
        POINT(1),
        LINESTRING(2),
        POLYGON(3)
    }
}
