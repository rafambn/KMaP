package com.rafambn.kmap.mapSource.tiled

import com.rafambn.kmap.mapSource.tiled.tiles.Tile

data class ActiveTiles(val currentZoom: Int = 0, val tiles: List<Tile> = emptyList())
