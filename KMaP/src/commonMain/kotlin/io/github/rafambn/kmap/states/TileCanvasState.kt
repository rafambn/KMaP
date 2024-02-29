package io.github.rafambn.kmap.states

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import io.github.rafambn.kmap.Tile

class TileCanvasState {
    internal val tileSize = 256F
    var listTiles = mutableStateListOf<Tile>()

    fun addTile(tile: Tile) {
        listTiles = mutableStateListOf(tile)
    }
}

@Composable
inline fun rememberTileState(
    crossinline init: TileCanvasState.() -> Unit = {}
): TileCanvasState = remember {
    TileCanvasState().apply(init)
}