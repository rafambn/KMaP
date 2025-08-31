@file:OptIn(ExperimentalSerializationApi::class)

package com.rafambn.kmap.utils.vectorTile

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
enum class GeomType(val value: Int) {
    UNKNOWN(0),
    POINT(1),
    LINESTRING(2),
    POLYGON(3)
}

@Serializable
data class Value(
    @ProtoNumber(1) val string_value: String? = null,
    @ProtoNumber(2) val float_value: Float? = null,
    @ProtoNumber(3) val double_value: Double? = null,
    @ProtoNumber(4) val int_value: Long? = null,
    @ProtoNumber(5) val uint_value: Long? = null,
    @ProtoNumber(6) val sint_value: Long? = null,
    @ProtoNumber(7) val bool_value: Boolean? = null
)

@Serializable
data class Feature(
    @ProtoNumber(1) val id: Long = 0L,
    @ProtoNumber(2) val tags: List<Int> = emptyList(),
    @ProtoNumber(3) val type: GeomType = GeomType.UNKNOWN,
    @ProtoNumber(4) val geometry: List<Int> = emptyList()
)

@Serializable
data class Layer(
    @ProtoNumber(15) val version: Int = 1,
    @ProtoNumber(1) val name: String,
    @ProtoNumber(2) val features: List<Feature> = emptyList(),
    @ProtoNumber(3) val keys: List<String> = emptyList(),
    @ProtoNumber(4) val values: List<Value> = emptyList(),
    @ProtoNumber(5) val extent: Int = 4096,
)

@Serializable
data class MVTile(
    @ProtoNumber(3) val layers: List<Layer> = emptyList()
)
