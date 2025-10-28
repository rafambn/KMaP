package com.rafambn.kmap.mapSource.tiled.vector

interface VectorTileSource {
    suspend fun getTile(zoom: Int, row: Int, column: Int): VectorTileResult
}
