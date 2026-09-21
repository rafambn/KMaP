package com.rafambn.kmap.source

interface TileSource<T : Tile> {
    suspend fun getTile(zoom: Int, row: Int, column: Int): TileResult<T>
}
