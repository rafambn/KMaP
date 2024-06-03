package io.github.rafambn.kmap.core

import io.github.rafambn.kmap.model.Tile
import kotlin.concurrent.Volatile

class TileLayers(startZoom: Int) {
    @Volatile
    var backLayer = listOf<Tile>()

    @Volatile
    var frontLayer = listOf<Tile>()

    @Volatile
    var backLayerLevel = startZoom - 1

    @Volatile
    var frontLayerLevel = startZoom

    fun changeLayer(frontLayerLevel: Int) {
        backLayer = frontLayer.toList()
        frontLayer = listOf()
        backLayerLevel = this.frontLayerLevel
        this.frontLayerLevel = frontLayerLevel
    }
}