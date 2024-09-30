package com.rafambn.kmapdemo.config.characteristics

import com.rafambn.kmapdemo.model.ResultTile

interface TileSource {
    suspend fun getTile(zoom: Int, row: Int, column: Int): ResultTile
}