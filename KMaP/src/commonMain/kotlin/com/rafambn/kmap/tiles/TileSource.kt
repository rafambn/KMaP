package com.rafambn.kmap.tiles

interface TileSource {
    suspend fun getTile(zoom: Int, row: Int, column: Int): TileRenderResult
}