package com.rafambn.kmap.config.characteristics

import com.rafambn.kmap.model.TileRenderResult

interface TileSource {
    suspend fun getTile(zoom: Int, row: Int, column: Int): TileRenderResult
}