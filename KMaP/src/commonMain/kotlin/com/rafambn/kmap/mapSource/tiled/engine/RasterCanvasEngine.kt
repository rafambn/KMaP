package com.rafambn.kmap.mapSource.tiled.engine

import com.rafambn.kmap.mapSource.tiled.TileResult
import com.rafambn.kmap.mapSource.tiled.tiles.RasterTile
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
