package com.rafambn.kmap.tiles

sealed interface TileResult {
    data class Success(val tile: Tile) : TileResult
    data class Failure(val specs: TileSpecs) : TileResult
}
