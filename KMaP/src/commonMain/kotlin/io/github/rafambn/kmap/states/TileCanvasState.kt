package io.github.rafambn.kmap.states

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import io.github.rafambn.kmap.tiles.Tile
import kotlin.math.floor
import kotlin.math.pow

class TileCanvasState {
    var visibleTilesList = mutableStateListOf<Tile>()

    fun onZoomChange() {
        visibleTilesList.clear()
    }

    fun onPositionChange(position: Offset, zoomLevel: Int, magnifierScale: Float, tileMapSize: Offset) {
        getVisibleTiles(position, zoomLevel, magnifierScale, tileMapSize).forEach { tile ->
            visibleTilesList.find { it == tile } ?: run { visibleTilesList.add(tile) }
        }
    }

    private fun getVisibleTiles(position: Offset, zoomLevel: Int, magnifierScale: Float, tileMapSize: Offset): List<Tile> {
        val centerTile = getXYTile(
            position.x.toDouble() / (2F.pow(zoomLevel - 1) * magnifierScale),
            position.y.toDouble() / (2F.pow(zoomLevel - 1) * magnifierScale),
            zoomLevel,
            tileMapSize.x.toDouble(),
            tileMapSize.y.toDouble()
        )
        val tileSpecsList = mutableListOf<Tile>()
        for (i in -2..2) for (j in -2..2) tileSpecsList.add(
            Tile(
                zoomLevel, centerTile.second + i, centerTile.first + j
            )
        )
        return tileSpecsList
    }

    private fun getXYTile(x: Double, y: Double, zoom: Int, width: Double, height: Double): Pair<Int, Int> {
        var xtile = floor(-x / width * (1 shl zoom)).toInt()
        var ytile = floor(-y / height * (1 shl zoom)).toInt()

        if (xtile < 0) {
            xtile = 0
        }
        if (xtile >= (1 shl zoom)) {
            xtile = (1 shl zoom) - 1
        }
        if (ytile < 0) {
            ytile = 0
        }
        if (ytile >= (1 shl zoom)) {
            ytile = (1 shl zoom) - 1
        }
        return Pair(xtile, ytile)
    }

    companion object {
        internal val tileSize = 256F
    }
}

@Composable
inline fun rememberTileCanvasState(
    crossinline init: TileCanvasState.() -> Unit = {}
): TileCanvasState = remember {
    TileCanvasState().apply(init)
}