package com.rafambn.kmapdemo.core

import com.rafambn.kmapdemo.model.Tile
import kotlin.concurrent.Volatile

data class TileLayers(
    @Volatile var backLayer: List<Tile> = listOf(),
    @Volatile var frontLayer: List<Tile> = listOf(),
    @Volatile var backLayerLevel: Int = -1,
    @Volatile var frontLayerLevel: Int = 0
) {
    fun changeLayer(frontLayerLevel: Int) {
        backLayer = frontLayer.toList()
        frontLayer = listOf()
        backLayerLevel = this.frontLayerLevel
        this.frontLayerLevel = frontLayerLevel
    }
}