package io.github.rafambn.kmap

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layoutId
import io.github.rafambn.kmap.composables.MotionManager
import io.github.rafambn.kmap.composables.TileCanvas
import io.github.rafambn.kmap.enums.MapComponentType
import io.github.rafambn.kmap.states.MapState

@Composable
fun KMaP(
    modifier: Modifier = Modifier,
    mapState: MapState,
) {
    MotionManager(modifier, mapState) {
        TileCanvas(
            Modifier.layoutId(MapComponentType.CANVAS),
            mapState
        )
    }
}