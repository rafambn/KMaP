package io.github.rafambn.kmap.states

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import io.github.rafambn.kmap.Tile

class TileCanvasState {
    internal val tileSize = 256F
    val listTiles = mutableStateListOf<Tile>()

    init {
        for (i in 0..8) {
            val row = i % 3
            val column = i / 3 % 3
            listTiles.add(Tile(0, row, column))
        }
    }
}

@Composable
inline fun rememberTileState(
    crossinline init: TileCanvasState.() -> Unit = {}
): TileCanvasState = remember {
    TileCanvasState().apply(init)
}