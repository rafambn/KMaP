package com.rafambn.kmap.mapSource.tiled

data class ActiveTiles(val currentZoom: Int = 0,  val tiles: List<TileWithVisibility> = emptyList())

data class TileWithVisibility(val tile: Tile, val isVisible: Boolean = true)
