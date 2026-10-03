package com.rafambn.kmap.source.internal

import com.rafambn.kmap.source.TileSpecs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

internal suspend fun CanvasKernel.awaitActiveTiles(id: Int, zoom: Int, expected: List<TileSpecs>): ActiveTiles =
    withContext(Dispatchers.Default) {
        withTimeout(5_000) {
            val expectedSpecs = expected.toSet()
            var active = getActiveTiles(id)
            while (active.currentZoom != zoom || active.tiles.map { TileSpecs(it.zoom, it.row, it.col) }.toSet() != expectedSpecs) {
                delay(1)
                active = getActiveTiles(id)
            }
            active
        }
    }
