package com.rafambn.kmap.mapSource.tiled.vector

import com.rafambn.kmap.mapSource.tiled.TileSpecs
import com.rafambn.kmap.mapSource.tiled.VectorTile

interface VectorTileResult {
    data class Success(val tile: VectorTile) : VectorTileResult
    data class Failure(val specs: TileSpecs) : VectorTileResult
}
