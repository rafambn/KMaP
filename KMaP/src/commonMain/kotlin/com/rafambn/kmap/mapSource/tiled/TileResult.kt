package com.rafambn.kmap.mapSource.tiled

import com.rafambn.kmap.mapSource.tiled.tiles.Tile
import com.rafambn.kmap.mapSource.tiled.tiles.TileSpecs

sealed interface TileResult<out T : Tile> {
    data class Success<T : Tile>(val tile: T) : TileResult<T>
    data class Failure(val specs: TileSpecs) : TileResult<Nothing>
}
