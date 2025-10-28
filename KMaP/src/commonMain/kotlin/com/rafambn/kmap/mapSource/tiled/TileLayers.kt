package com.rafambn.kmap.mapSource.tiled

data class TileLayers(
    val frontLayer: Layer = Layer(0, listOf()),
    val backLayer: Layer = Layer(-1, listOf())
)

data class Layer(val level: Int, val tiles: List<Tile>)
