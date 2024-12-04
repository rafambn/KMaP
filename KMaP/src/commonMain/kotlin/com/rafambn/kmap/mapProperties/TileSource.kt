package com.rafambn.kmap.mapProperties

import com.rafambn.kmap.tiles.TileRenderResult

interface TileSource {
    suspend fun getTile(zoom: Int, row: Int, column: Int): TileRenderResult
}