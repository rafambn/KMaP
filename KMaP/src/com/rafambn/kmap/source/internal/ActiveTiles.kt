package com.rafambn.kmap.source.internal

import com.rafambn.kmap.source.Tile

data class ActiveTiles(val currentZoom: Int = 0, val tiles: List<Tile> = emptyList())
