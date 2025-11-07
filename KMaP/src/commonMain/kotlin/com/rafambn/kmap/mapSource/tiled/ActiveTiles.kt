package com.rafambn.kmap.mapSource.tiled

data class ActiveTiles(val currentZoom: Int = 0, val tiles: List<Tile> = emptyList())
