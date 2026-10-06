@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
package com.rafambn.kmap.source.internal

import com.rafambn.kmap.components.parameters.VectorCanvasParameters
import com.rafambn.kmap.mvttile.*
import com.rafambn.kmap.source.*
import com.rafambn.kmap.source.preparation.*
import com.rafambn.kmap.style.StyleResolver
import com.rafambn.kmap.style.compiled.CompiledStyle
import kotlinx.coroutines.*
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.serialization.protobuf.ProtoBuf

internal class CanvasPipelineTestFixture {
    val scheduler = TestCoroutineScheduler()
    val parent = Job()
    val scope = CoroutineScope(parent + StandardTestDispatcher(scheduler))
    val requests = mutableListOf<PreparationRequest>()
    private val replies = mutableMapOf<Long, CompletableDeferred<PreparedTile>>()
    var createdWorkers = 0
    var closedWorkers = 0
    val engine = CanvasEngine(scope, PreparationPool(createWorker = {
        createdWorkers++
        object : PreparationWorker {
            private var reply: CompletableDeferred<PreparedTile>? = null
            private var closed = false
            override suspend fun prepare(request: PreparationRequest): PreparedTile {
                val result = CompletableDeferred<PreparedTile>()
                reply = result
                replies[request.requestId] = result
                requests.add(request)
                return result.await()
            }
            override fun close() {
                if (closed) return
                closed = true
                closedWorkers++
                reply?.completeExceptionally(WorkerFailure("test worker closed"))
            }
        }
    }))

    fun vector(id: Int = 0, style: CompiledStyle = pipelineStyle(),
               source: suspend (Int, Int, Int) -> TileResult<VectorTile> = { z, r, c ->
                   TileResult.Success(EncodedVectorTile(z, r, c, pipelineBytes()))
               }) = VectorCanvasParameters(id, tileSource = source, style = style)

    fun complete(request: PreparationRequest = requests.last(), prepared: PreparedTile = TilePreparation(request.style)
        .prepare(request.bytes, request.specs.zoom, request.specs.row, request.specs.col)) {
        replies.getValue(request.requestId).complete(prepared)
    }
    fun fail(request: PreparationRequest = requests.last(), error: Exception) {
        replies.getValue(request.requestId).completeExceptionally(error)
    }
    suspend fun finish() {
        engine.close()
        scheduler.runCurrent()
        parent.cancel()
        scheduler.runCurrent()
        parent.join()
    }
}

internal fun pipelineStyle(filter: String = "") = StyleResolver().resolve(
    """{"layers":[{"id":"places","type":"symbol","source-layer":"places"$filter}]}"""
).style!!

internal fun pipelineBytes(id: ULong = 1UL): ByteArray = ProtoBuf.encodeToByteArray(
    RawMVTile.serializer(), MVTile(listOf(MVTLayer("places", 4096, listOf(
        MVTFeature(id, RawMVTGeomType.POINT, listOf(listOf(4 to 8)), mapOf("kind" to "park")),
    )))).deparse(),
)

internal suspend fun testPipeline(block: suspend CanvasPipelineTestFixture.() -> Unit) {
    val fixture = CanvasPipelineTestFixture()
    try { fixture.block() } finally { fixture.finish() }
}
