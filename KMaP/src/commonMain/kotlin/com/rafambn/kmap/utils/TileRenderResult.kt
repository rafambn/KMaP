package com.rafambn.kmap.utils

import com.rafambn.kmap.model.Tile
import com.rafambn.kmap.model.TileSpecs

interface TileRenderResult {
    data class Success(val tile: Tile) : TileRenderResult
    data class Failure(val specs: TileSpecs) : TileRenderResult
}