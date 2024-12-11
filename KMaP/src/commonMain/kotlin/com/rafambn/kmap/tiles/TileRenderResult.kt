package com.rafambn.kmap.tiles

sealed interface TileRenderResult {
    data class Success(val tile: Tile) : TileRenderResult
    data class Failure(val specs: TileSpecs) : TileRenderResult
}