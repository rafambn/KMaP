package com.rafambn.kmap.source.internal

import com.rafambn.kmap.source.Tile
import com.rafambn.kmap.source.TileResult
import com.rafambn.kmap.source.TileSpecs
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler

internal class CanvasEngineTestFixture(maxCacheTiles: Int = 20) {
    val scheduler = TestCoroutineScheduler()
    val job = Job()
    private val dispatcher = StandardTestDispatcher(scheduler)
    private val scope = CoroutineScope(job + dispatcher)
    private val requests = mutableListOf<TileSpecs>()
    private val pending = mutableMapOf<TileSpecs, CompletableDeferred<TileResult<Tile>>>()
    val engine = CanvasEngine(
        maxCacheTiles = maxCacheTiles,
        coroutineScope = scope,
        tileRenderer = TileRenderer(
            coroutineScope = scope,
            getTile = { zoom, row, col ->
                val specs = TileSpecs(zoom, row, col)
                val result = CompletableDeferred<TileResult<Tile>>()
                pending[specs] = result
                requests.add(specs)
                try {
                    result.await()
                } finally {
                    pending.remove(specs)
                }
            },
            processTile = { it },
            dispatcher = dispatcher,
        ),
    )

    fun completeTile(tile: Tile) {
        pending.getValue(TileSpecs(tile.zoom, tile.row, tile.col)).complete(TileResult.Success(tile))
    }

    fun failTile(specs: TileSpecs) {
        pending.getValue(specs).complete(TileResult.Failure(specs))
    }

    fun takeRequests(): List<TileSpecs> {
        val result = requests.toList()
        requests.clear()
        return result
    }
}
