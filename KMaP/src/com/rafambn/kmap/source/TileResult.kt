package com.rafambn.kmap.source

sealed interface TileResult<out T : Tile> {
    data class Success<T : Tile>(val tile: T) : TileResult<T>
    data class Failure(val specs: TileSpecs) : TileResult<Nothing>
}
