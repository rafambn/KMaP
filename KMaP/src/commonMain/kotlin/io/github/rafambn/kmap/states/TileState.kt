package io.github.rafambn.kmap.states

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

class TileState(
    cameraState: CameraState? = null,
    mapProperties: MapProperties? = null
) {



}

@Composable
inline fun rememberTileState(
    crossinline init: TileState.() -> Unit = {}
): TileState = remember {
    TileState().apply(init)
}