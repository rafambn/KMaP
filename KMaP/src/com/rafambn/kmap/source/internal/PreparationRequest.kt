package com.rafambn.kmap.source.internal

import com.rafambn.kmap.source.TileSpecs
import com.rafambn.kmap.source.preparation.PreparationStyle

internal data class PreparationRequest(
    val requestId: Long,
    val canvasGeneration: Long,
    val specs: TileSpecs,
    val styleRevision: Long,
    val style: PreparationStyle,
    val bytes: ByteArray,
)
