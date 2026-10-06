package com.rafambn.kmap.source.internal

import com.rafambn.kmap.source.TileSpecs
import kotlinx.coroutines.Job

internal data class CanvasRequest(
    val id: Long,
    val canvasId: Int,
    val generation: Long,
    val styleRevision: Long,
    val specs: TileSpecs,
    var job: Job? = null,
)
