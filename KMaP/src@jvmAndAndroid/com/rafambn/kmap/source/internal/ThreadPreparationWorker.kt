package com.rafambn.kmap.source.internal

import com.rafambn.kmap.source.preparation.PreparedTile
import com.rafambn.kmap.source.preparation.TilePreparation
import kotlinx.coroutines.CompletableDeferred
import java.util.concurrent.Executors

internal actual fun createPreparationWorker(): PreparationWorker = ThreadPreparationWorker()

private class ThreadPreparationWorker : PreparationWorker {
    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "KMaP-tile-preparation").apply { isDaemon = true }
    }
    private var pending: CompletableDeferred<PreparedTile>? = null
    private var styleRevision: Long? = null
    private var preparation: TilePreparation? = null

    override suspend fun prepare(request: PreparationRequest): PreparedTile {
        val result = CompletableDeferred<PreparedTile>()
        pending = result
        executor.execute {
            try {
                if (styleRevision != request.styleRevision) {
                    preparation = TilePreparation(request.style)
                    styleRevision = request.styleRevision
                }
                result.complete(preparation!!.prepare(request.bytes, request.specs.zoom, request.specs.row, request.specs.col))
            } catch (error: Exception) {
                result.completeExceptionally(error)
            }
        }
        try {
            return result.await()
        } finally {
            pending = null
        }
    }

    override fun close() {
        pending?.completeExceptionally(WorkerFailure("Preparation executor closed"))
        executor.shutdownNow()
    }
}
