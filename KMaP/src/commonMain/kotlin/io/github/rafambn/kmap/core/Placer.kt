package io.github.rafambn.kmap.core

import io.github.rafambn.kmap.core.state.MapState
import io.github.rafambn.kmap.utils.offsets.ProjectedCoordinates
import io.github.rafambn.kmap.utils.offsets.ScreenOffset

class Placer(
    mapState: MapState,
    coordinates: ProjectedCoordinates,
    val drawPosition: DrawPosition = DrawPosition.TOP_LEFT, //Functionality Implemented
    val groupId: Int = -1, //TODO add grouping
    val zIndex: Float = 1F, //Functionality Implemented
    val isGrouping: Boolean = false, //TODO add grouping
    val scaleWithMap: Boolean = false, //Functionality Implemented
    val rotateWithMap: Boolean = false,//Functionality Implemented
    val zoomToFix: Float = 0F, //Functionality Implemented
) {
    var coordinates: ScreenOffset = mapState.mapProperties.mapSource.toCanvasPosition(coordinates).toScreenOffset( //TODO weird placement
        mapState.mapPosition,
        mapState.canvasSize,
        mapState.magnifierScale,
        mapState.zoomLevel,
        mapState.angleDegrees,
        mapState.density,
        mapState.mapProperties.mapSource
    )
    internal val angle: Double = mapState.angleDegrees //Functionality Implemented
    internal val zoom: Float = mapState.zoom
}