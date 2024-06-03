package io.github.rafambn.kmap

import io.github.rafambn.kmap.utils.CanvasPosition
import io.github.rafambn.kmap.utils.ScreenOffset
import io.github.rafambn.kmap.utils.toScreenOffset

class Placer(
    mapState: MapState,
    coordinates: CanvasPosition,
    val drawPosition: DrawPosition, //Functionality Implemented
    val groupId: Int, //TODO add grouping
    val zIndex: Float, //Functionality Implemented
    val isGrouping: Boolean, //TODO add grouping
    val rotateWithMap: Boolean,//Functionality Implemented
    val scaleWithMap: Boolean, //Functionality Implemented
    val zoomToFix: Float, //Functionality Implemented
) {
    var coordinates: ScreenOffset = coordinates.toScreenOffset( //TODO weird placement
        mapState.mapPosition,
        mapState.canvasSize,
        mapState.magnifierScale,
        mapState.zoomLevel,
        mapState.angleDegrees,
        mapState.density,
        mapState.mapProperties.mapSource
    )
    val angle: Double = mapState.angleDegrees //Functionality Implemented
    val zoom: Float = mapState.zoom
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
        val CENTER_BOTTOM = DrawPosition(0.5F, 1F)
        val CENTER_TOP = DrawPosition(0.5F, 0F)
        val LEFT_BOTTOM = DrawPosition(0F, 1F)
        val LEFT_CENTER = DrawPosition(0F, 0.5F)
        val LEFT_TOP = DrawPosition(0F, 0F)
        val RIGHT_BOTTOM = DrawPosition(1F, 1F)
        val RIGHT_CENTER = DrawPosition(1F, 0.5F)
        val RIGHT_TOP = DrawPosition(1F, 0F)
    }
}