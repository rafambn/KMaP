package io.github.rafambn.kmap.tiles

data class TileToProcess(
    val zoom: Int,
    val row: Int,
    val col: Int,
    val numberOfFails: Int)
