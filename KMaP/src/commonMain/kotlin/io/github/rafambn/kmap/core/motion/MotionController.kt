package io.github.rafambn.kmap.core.motion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.rafambn.kmap.config.MapSource
import io.github.rafambn.kmap.core.state.MapState

@Composable
fun rememberMotionController(): MotionController = remember {
    MotionController()
}

class MotionController {
    private var mapState: MapState? = null

    internal fun setMap(mapState: MapState) {
        this.mapState = mapState
    }

    fun set(mapSet: (MapState, MapSource) -> Unit) {
        mapState?.set(mapSet)
    }

    fun set(mapSet: (MapState) -> Unit) {
        mapState?.set(mapSet)
    }

    fun scroll(mapScroll: (MapState, MapSource) -> Unit) {
        mapState?.scroll(mapScroll)
    }

    fun scroll(mapScroll: (MapState) -> Unit) {
        mapState?.scroll(mapScroll)
    }

    fun animate(mapScroll: (MapState, MapSource) -> Unit) {
        mapState?.animate(mapScroll)
    }

    fun animate(mapScroll: (MapState) -> Unit) {
        mapState?.animate(mapScroll)
    }
}