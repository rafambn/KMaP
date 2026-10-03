package com.rafambn.kmap.source.internal

import com.rafambn.kmap.source.TileSpecs

internal data class TileSelection(val visibleTiles: List<TileSpecs>, val zoomLevel: Int)
