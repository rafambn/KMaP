@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class, kotlin.native.concurrent.ObsoleteWorkersApi::class)

package com.rafambn.kmap.source.internal

import com.rafambn.kmap.source.preparation.PreparedTile
import com.rafambn.kmap.source.preparation.TilePreparation
import kotlinx.coroutines.CompletableDeferred
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.native.concurrent.ThreadLocal

internal actual fun createPreparationWorker(): PreparationWorker = NativePreparationWorker()

@ThreadLocal
private var nativeStyleRevision: Long? = null
@ThreadLocal
private var nativePreparation: TilePreparation? = null

private class NativePreparationWorker : PreparationWorker {
    private val worker = Worker.start(name = "KMaP-tile-preparation")
    private var pending: CompletableDeferred<PreparedTile>? = null

    override suspend fun prepare(request: PreparationRequest): PreparedTile {
        val result = CompletableDeferred<PreparedTile>()
        pending = result
        worker.execute(TransferMode.SAFE, { request to result }) { (input, reply) ->
            try {
                if (nativeStyleRevision != input.styleRevision) {
                    nativePreparation = TilePreparation(input.style)
                    nativeStyleRevision = input.styleRevision
                }
                reply.complete(nativePreparation!!.prepare(input.bytes, input.specs.zoom, input.specs.row, input.specs.col))
            } catch (error: Exception) {
                reply.completeExceptionally(error)
            }
        }
        try {
            return result.await()
        } finally {
            pending = null
        }
    }

    override fun close() {
        pending?.completeExceptionally(WorkerFailure("Native preparation worker closed"))
        worker.requestTermination(processScheduledJobs = false)
    }
}
