package com.rafambn.kmap.source.internal

import com.rafambn.kmap.source.RasterTile
import com.rafambn.kmap.source.TileResult
import kotlinx.coroutines.CoroutineScope

class RasterCanvasEngine(
    maxCacheTiles: Int,
    getTile: suspend (zoom: Int, row: Int, column: Int) -> TileResult<RasterTile>,
    coroutineScope: CoroutineScope,
) : CanvasEngine<RasterTile>(
    maxCacheTiles,
    coroutineScope,
    TileRenderer(
        coroutineScope = coroutineScope,
        getTile = getTile,
        processTile = { it }
    )
)
