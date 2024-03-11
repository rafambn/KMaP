package io.github.rafambn.kmap.model

data class TileSpecs(
    val zoom: Int,
    val row: Int,
    val col: Int,
    var numberOfTries: Int = 0
)