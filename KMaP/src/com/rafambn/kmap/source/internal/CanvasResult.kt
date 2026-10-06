package com.rafambn.kmap.source.internal

import com.rafambn.kmap.source.Tile
import com.rafambn.kmap.source.TileResult
import com.rafambn.kmap.source.preparation.PreparedTile

internal sealed interface CanvasResult {
    data class Fetched(val request: CanvasRequest, val result: TileResult<Tile>?, val error: Exception?, val millis: Double) : CanvasResult
    data class Prepared(val request: CanvasRequest, val tile: PreparedTile?, val error: Exception?, val millis: Double) : CanvasResult
}
