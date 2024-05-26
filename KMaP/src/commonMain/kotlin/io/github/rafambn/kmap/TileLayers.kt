package io.github.rafambn.kmap

import kotlin.concurrent.Volatile

class TileLayers {
    @Volatile
    var backLayer = mutableListOf<Tile>()

    @Volatile
    var frontLayer = mutableListOf<Tile>()

    @Volatile
    var backLayerLevel = 0

    @Volatile
    var frontLayerLevel = 1

    @Volatile
    var backLayerEnable = true

    @Volatile
    var frontLayerEnable = true

    fun changeLayer() {
        backLayer = frontLayer.toMutableList()
        frontLayer.clear()
    }
}