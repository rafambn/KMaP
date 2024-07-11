package io.github.rafambn.kmap.config.characteristics

import io.github.rafambn.kmap.model.ResultTile

interface TileSource {
    suspend fun getTile(zoom: Int, row: Int, column: Int): ResultTile
}