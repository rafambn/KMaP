@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.rafambn.kmap.source.preparation

import com.rafambn.kmap.mvttile.RawMVTGeomType
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoPacked

@Serializable
data class PreparedFeature(
    val type: RawMVTGeomType,
    val extent: Int,
    @ProtoPacked val coordinates: IntArray,
    /** Offsets count points, including the final end offset. */
    @ProtoPacked val parts: IntArray,
    /** Decimal uint64 text avoids JavaScript Number precision loss. */
    val id: String? = null,
    val properties: Map<String, PreparedValue> = emptyMap(),
)
