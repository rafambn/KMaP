package com.rafambn.kmap.components.canvas.tiled

interface TileSource {
    suspend fun getTile(zoom: Int, row: Int, column: Int): TileRenderResult
}
