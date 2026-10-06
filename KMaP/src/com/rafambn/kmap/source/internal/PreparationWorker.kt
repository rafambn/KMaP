package com.rafambn.kmap.source.internal

import com.rafambn.kmap.source.preparation.PreparedTile

/** Owns a real thread/Worker. One request at a time; close terminates pending waits and owned resources. */
internal interface PreparationWorker {
    suspend fun prepare(request: PreparationRequest): PreparedTile
    fun close()
}

internal expect fun createPreparationWorker(): PreparationWorker
