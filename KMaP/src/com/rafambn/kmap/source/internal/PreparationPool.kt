package com.rafambn.kmap.source.internal

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext

internal class PreparationPool(val capacity: Int = 2, private val createWorker: () -> PreparationWorker = ::createPreparationWorker) {
    private val available = Channel<Int>(capacity)
    private val workers = arrayOfNulls<PreparationWorker>(capacity)
    private var closed = false

    init {
        repeat(capacity) { available.trySend(it) }
    }

    suspend fun prepare(request: PreparationRequest) = withContext(NonCancellable) {
        check(!closed) { "Preparation pool is closed" }
        val slot = available.receive()
        try {
            check(!closed) { "Preparation pool is closed" }
            val worker = workers[slot] ?: createWorker().also { workers[slot] = it }
            worker.prepare(request)
        } catch (error: WorkerFailure) {
            workers[slot]?.close()
            workers[slot] = null
            throw error
        } finally {
            // An obsolete CPU job keeps its slot until its terminal reply, even if its canvas was removed.
            available.trySend(slot)
        }
    }

    fun close() {
        if (closed) return
        closed = true
        available.close()
        workers.forEach { it?.close() }
    }
}
