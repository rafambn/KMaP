package io.github.rafambn.kmap

import androidx.compose.runtime.Composable
import io.github.rafambn.kmap.states.MapState
import io.github.rafambn.kmap.states.TileCanvasState
import kotlin.math.floor

@Composable
fun VisibleTilesResolver(mapState: MapState, tileCanvasState: TileCanvasState) {



}

fun getXYTile(x: Double, y: Double, zoom: Int, width: Double, height: Double): Pair<Int, Int> {
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
