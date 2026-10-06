package com.rafambn.kmap.source.internal

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.rafambn.kmap.components.parameters.CanvasParameters
import com.rafambn.kmap.source.*

internal class CanvasEntry(var parameters: CanvasParameters, val generation: Long, var styleRevision: Long) {
    var activeTiles by mutableStateOf(ActiveTiles())
    var status by mutableStateOf(TilePipelineStatus())
    val cache = linkedMapOf<TileSpecs, Tile>()
    val cacheSizes = mutableMapOf<TileSpecs, Int>()
    val raw = linkedMapOf<TileSpecs, EncodedVectorTile>()
    var demand = emptyList<TileSpecs>()
    val failures = mutableMapOf<TileSpecs, TilePipelineFailure>()
    val attempts = mutableMapOf<TileSpecs, Int>()
    var completedWorkers = 0
    var completedLocal = 0
    var fetchMillis = 0.0
    var preparationMillis = 0.0
    var conversionMillis = 0.0
}
