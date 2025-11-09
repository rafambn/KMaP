package com.rafambn.kmap.mapSource.tiled

import com.rafambn.kmap.mapSource.tiled.tiles.Tile

interface TileSource<T : Tile> {
    suspend fun getTile(zoom: Int, row: Int, column: Int): TileResult<T>
}
