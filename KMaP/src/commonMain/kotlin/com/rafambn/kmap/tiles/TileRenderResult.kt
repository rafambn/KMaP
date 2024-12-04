package com.rafambn.kmap.tiles

interface TileRenderResult {
    data class Success(val tile: Tile) : TileRenderResult
    data class Failure(val specs: TileSpecs) : TileRenderResult
}