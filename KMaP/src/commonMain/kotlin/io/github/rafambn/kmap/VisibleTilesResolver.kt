package io.github.rafambn.kmap

import androidx.compose.runtime.Composable
import io.github.rafambn.kmap.states.MapState

@Composable
fun VisibleTilesResolver(mapState: MapState) {
    mapState.rawPosition
    mapState.angleDegrees
    mapState.mapViewSize
    val tileSize = 256F

}