package com.rafambn.kmap.mapSource.tiled.raster

import com.rafambn.kmap.mapSource.tiled.RasterTile
import com.rafambn.kmap.mapSource.tiled.TileSpecs

interface RasterTileResult {
    data class Success(val tile: RasterTile) : RasterTileResult
    data class Failure(val specs: TileSpecs) : RasterTileResult
}
