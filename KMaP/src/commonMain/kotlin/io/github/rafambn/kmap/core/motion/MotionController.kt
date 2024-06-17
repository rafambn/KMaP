package io.github.rafambn.kmap.core.motion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.rafambn.kmap.config.characteristics.MapSource
import io.github.rafambn.kmap.core.state.MapState

@Composable
fun rememberMotionController(): MotionController = remember {
    MotionController()
}

class MotionController {
    private var mapState: MapState? = null
    private var onMapChanged: ((MapState) -> Unit)? by mutableStateOf(null)

    internal fun setMap(mapState: MapState) {
        this.mapState = mapState
        onMapChanged?.let {
            onMapChanged = null
            it(mapState)
        }
    }

    fun set(mapSet: (MapState) -> Unit) {
        val map = mapState
        if (map == null) {
            onMapChanged = {
                it.set(mapSet)
                onMapChanged?.let { callback -> callback(it) }
            }
        } else
            map.set(mapSet)
    }

    fun set(mapSet: (MapState, MapSource) -> Unit) {
        val map = mapState
        if (map == null) {
            onMapChanged = {
                it.set(mapSet)
                onMapChanged?.let { callback -> callback(it) }
            }
        } else
            map.set(mapSet)
    }

    fun scroll(mapScroll: (MapState) -> Unit) {
        val map = mapState
        if (map == null) {
            onMapChanged = {
                it.scroll(mapScroll)
                onMapChanged?.let { callback -> callback(it) }
            }
        } else
            map.scroll(mapScroll)
    }

    fun scroll(mapScroll: (MapState, MapSource) -> Unit) {
        val map = mapState
        if (map == null) {
            onMapChanged = {
                it.scroll(mapScroll)
                onMapChanged?.let { callback -> callback(it) }
            }
        } else
            map.scroll(mapScroll)
    }

    fun animate(mapAnimate: (MapState) -> Unit) {
        val map = mapState
        if (map == null) {
            onMapChanged = {
                it.animate(mapAnimate)
                onMapChanged?.let { callback -> callback(it) }
            }
        } else
            map.animate(mapAnimate)
    }

    fun animate(mapAnimate: (MapState, MapSource) -> Unit) { //TODO(2) implement async
        val map = mapState
        if (map == null) {
            onMapChanged = {
                it.animate(mapAnimate)
                onMapChanged?.let { callback -> callback(it) }
            }
        } else
            map.animate(mapAnimate)
    }
}