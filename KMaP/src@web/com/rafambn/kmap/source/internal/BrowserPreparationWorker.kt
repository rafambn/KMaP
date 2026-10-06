@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class, kotlinx.serialization.ExperimentalSerializationApi::class)

package com.rafambn.kmap.source.internal

import com.rafambn.kmap.source.preparation.PreparationStyle
import com.rafambn.kmap.source.preparation.PreparedTile
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.js.JsAny

internal actual fun createPreparationWorker(): PreparationWorker = try {
    BrowserPreparationWorker()
} catch (error: Exception) {
    throw WorkerFailure("Could not start tile worker", error)
}

private class BrowserPreparationWorker : PreparationWorker {
    private val ready = CompletableDeferred<Unit>()
    private var styleReady: CompletableDeferred<Unit>? = null
    private var pending: CompletableDeferred<PreparedTile>? = null
    private var requestId: String? = null
    private var styleRevision: String? = null
    private var closed = false
    private var failure: WorkerFailure? = null
    private val worker = openWorker(::onMessage, ::onFailure)

    private fun onMessage(kind: String, id: String, revision: String, payload: JsAny?, message: String) {
        if (closed) return
        when (kind) {
            "ready" -> if (message == "1") ready.complete(Unit) else onFailure("Incompatible tile worker protocol: $message")
            "style-ready" -> if (revision == styleRevision) styleReady?.complete(Unit)
            "prepared" -> if (id == requestId && revision == styleRevision && payload != null) {
                try {
                    val view = bufferView(payload)
                    val bytes = ByteArray(bufferSize(view)) { bufferByte(view, it).toByte() }
                    pending?.complete(ProtoBuf.decodeFromByteArray(PreparedTile.serializer(), bytes))
                } catch (error: Exception) {
                    pending?.completeExceptionally(WorkerFailure("Invalid tile worker reply", error))
                }
            }
            "failed" -> if (id == requestId && revision == styleRevision) pending?.completeExceptionally(IllegalArgumentException(message))
            "fatal" -> onFailure(message)
        }
    }

    private fun onFailure(message: String) {
        val error = WorkerFailure(message)
        failure = error
        ready.completeExceptionally(error)
        styleReady?.completeExceptionally(error)
        pending?.completeExceptionally(error)
    }

    override suspend fun prepare(request: PreparationRequest): PreparedTile {
        check(!closed)
        failure?.let { throw it }
        try {
            withTimeout(15_000) { ready.await() }
            val revision = request.styleRevision.toString()
            if (styleRevision != revision) {
                styleRevision = revision
                val installed = CompletableDeferred<Unit>()
                styleReady = installed
                installStyle(worker, revision, Json.encodeToString(PreparationStyle.serializer(), request.style))
                withTimeout(15_000) { installed.await() }
            }
            val result = CompletableDeferred<PreparedTile>()
            pending = result
            requestId = request.requestId.toString()
            // Keep the engine's raw cache intact. Transfer a separate buffer and never read it after posting.
            val buffer = newBuffer(request.bytes.size)
            request.bytes.forEachIndexed { index, byte -> setBufferByte(buffer, index, byte.toInt()) }
            postTile(worker, requestId!!, revision, request.canvasGeneration.toString(),
                request.specs.zoom, request.specs.row, request.specs.col, buffer)
            return withTimeout(60_000) { result.await() }
        } catch (error: kotlinx.coroutines.TimeoutCancellationException) {
            throw WorkerFailure("Tile worker readiness or processing timed out", error)
        } finally {
            pending = null
            requestId = null
        }
    }

    override fun close() {
        if (closed) return
        onFailure("Tile worker closed")
        closed = true
        terminateWorker(worker)
    }
}

private fun openWorker(onMessage: (String, String, String, JsAny?, String) -> Unit, onFailure: (String) -> Unit): JsAny = js("""{
    const url = new URL('kmap-worker/tile-worker.mjs', document.baseURI);
    const worker = new Worker(url, {type:'module', name:'KMaP-tile-preparation'});
    worker.onmessage = event => { const m=event.data; onMessage(m.kind || '', m.id || '', m.revision || '', m.payload || null, m.message || ''); };
    worker.onerror = event => { event.preventDefault(); onFailure(event.message || 'Tile worker asset/runtime failed'); };
    worker.onmessageerror = () => onFailure('Tile worker message could not be decoded');
    return worker;
}""")
private fun installStyle(worker: JsAny, revision: String, definition: String): Unit = js("worker.postMessage({kind:'install-style',revision:revision,definition:definition})")
private fun postTile(worker: JsAny, id: String, revision: String, generation: String, zoom: Int, row: Int, col: Int, payload: JsAny): Unit = js("worker.postMessage({kind:'prepare',id:id,revision:revision,generation:generation,zoom:zoom,row:row,col:col,payload:payload.buffer},[payload.buffer])")
private fun terminateWorker(worker: JsAny): Unit = js("worker.terminate()")
private fun newBuffer(size: Int): JsAny = js("new Uint8Array(size)")
private fun bufferView(buffer: JsAny): JsAny = js("new Uint8Array(buffer)")
private fun setBufferByte(buffer: JsAny, index: Int, value: Int): Unit = js("buffer[index]=value")
private fun bufferSize(buffer: JsAny): Int = js("buffer.length")
private fun bufferByte(buffer: JsAny, index: Int): Int = js("buffer[index]")
