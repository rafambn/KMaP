package com.rafambn.kmap.mapSource.tiled.raster

interface RasterTileSource {
    suspend fun getTile(zoom: Int, row: Int, column: Int): RasterTileResult
}
