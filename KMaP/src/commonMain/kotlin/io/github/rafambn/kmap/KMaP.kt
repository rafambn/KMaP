package io.github.rafambn.kmap

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layoutId
import io.github.rafambn.kmap.enums.MapComponentType
import io.github.rafambn.kmap.states.CameraState
import io.github.rafambn.kmap.states.MapProperties

@Composable
fun KMaP(
    modifier: Modifier = Modifier,
    cameraState: CameraState,
    mapProperties: MapProperties
) {
    MotionManager(modifier, cameraState) {
        TileCanvas(
            Modifier.layoutId(MapComponentType.CANVAS),
            cameraState.angleDegres.value,
            cameraState.rawPosition.value,
            cameraState.zoom.value,
            cameraState.mapSize.value
        )
    }
}