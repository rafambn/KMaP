package io.github.rafambn.kmap.core

import io.github.rafambn.kmap.core.state.MapState
import io.github.rafambn.kmap.utils.Degrees
import io.github.rafambn.kmap.utils.offsets.ProjectedCoordinates
import io.github.rafambn.kmap.utils.offsets.ScreenOffset

class Placer(
    //TODO(4) add grouping
    mapState: MapState,//todo remove mapState
    coordinates: ProjectedCoordinates,
    val tag: String = "",
    val alpha: Float = 1F,
    val drawPosition: DrawPosition = DrawPosition.TOP_LEFT,
    val zIndex: Float = 1F,
    val scaleWithMap: Boolean = false,
    val zoomToFix: Float = 0F,
    val rotateWithMap: Boolean = false,
    val rotation: Degrees = 0.0,
) {
    var coordinates: ScreenOffset = with(mapState) {
        mapState.mapSource.toCanvasPosition(coordinates).toScreenOffset()
    }
    internal val angle: Double = mapState.angleDegrees
    internal val zoom: Float = mapState.zoom
}