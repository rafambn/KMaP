package io.github.rafambn.kmap

import io.github.rafambn.kmap.OSMMapSource.toCanvasPosition
import io.github.rafambn.kmap.utils.CanvasPosition
import io.github.rafambn.kmap.utils.ProjectedCoordinates
import io.github.rafambn.kmap.utils.ScreenOffset
import io.github.rafambn.kmap.utils.toScreenOffset

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

class DrawPosition(x: Float, y: Float) {
    val x = x.coerceIn(0.0f, 1.0f).also {
        if (it != x) println("Warning: x was coerced to the range [0, 1]")
    }
    val y = y.coerceIn(0.0f, 1.0f).also {
        if (it != y) println("Warning: y was coerced to the range [0, 1]")
    }

    companion object {
        val CENTER = DrawPosition(0.5F, 0.5F)
        val CENTER_LEFT = DrawPosition(0F, 0.5F)
        val CENTER_RIGHT = DrawPosition(1F, 0.5F)
        val BOTTOM_CENTER = DrawPosition(0.5F, 1F)
        val BOTTOM_LEFT = DrawPosition(0F, 1F)
        val BOTTOM_RIGHT = DrawPosition(1F, 1F)
        val TOP_CENTER = DrawPosition(0.5F, 0F)
        val TOP_LEFT = DrawPosition(0F, 0F)
        val TOP_RIGHT = DrawPosition(1F, 0F)
    }
}