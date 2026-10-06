package com.rafambn.kmap.source.internal

import com.rafambn.kmap.components.parameters.RasterCanvasParameters
import com.rafambn.kmap.source.RasterTile
import com.rafambn.kmap.source.TileResult
import com.rafambn.kmap.source.TileSpecs
import kotlinx.coroutines.*
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler

internal class CanvasEngineTestFixture(maxCacheTiles: Int = 20) {
    val scheduler = TestCoroutineScheduler()
    val job = Job()
    private val dispatcher = StandardTestDispatcher(scheduler)
    val scope = CoroutineScope(job + dispatcher)
    private val requests = mutableListOf<TileSpecs>()
    private val pending = mutableMapOf<TileSpecs, CompletableDeferred<TileResult<RasterTile>>>()
    val engine = CanvasEngine(scope)

    init {
        engine.refreshCanvas(listOf(RasterCanvasParameters(
            id = 0, maxCacheTiles = maxCacheTiles,
            tileSource = { zoom, row, col ->
                val specs = TileSpecs(zoom, row, col)
                val result = CompletableDeferred<TileResult<RasterTile>>()
                pending[specs] = result
                requests.add(specs)
                try { result.await() } finally { pending.remove(specs) }
            },
        )))
    }

    fun completeTile(tile: RasterTile) {
        pending.getValue(TileSpecs(tile.zoom, tile.row, tile.col)).complete(TileResult.Success(tile))
    }
    fun failTile(specs: TileSpecs) { pending.getValue(specs).complete(TileResult.Failure(specs)) }
    fun takeRequests(): List<TileSpecs> = requests.toList().also { requests.clear() }
}
