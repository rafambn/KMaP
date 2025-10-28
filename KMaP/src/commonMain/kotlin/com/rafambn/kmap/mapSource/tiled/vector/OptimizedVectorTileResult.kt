package com.rafambn.kmap.mapSource.tiled.vector

import com.rafambn.kmap.mapSource.tiled.OptimizedVectorTile
import com.rafambn.kmap.mapSource.tiled.TileSpecs

interface OptimizedVectorTileResult {
    data class Success(val tile: OptimizedVectorTile) : OptimizedVectorTileResult
    data class Failure(val specs: TileSpecs) : OptimizedVectorTileResult
}
