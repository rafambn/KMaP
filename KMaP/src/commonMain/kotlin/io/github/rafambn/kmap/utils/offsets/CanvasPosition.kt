package io.github.rafambn.kmap.utils.offsets

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import io.github.rafambn.kmap.config.MapSource
import io.github.rafambn.kmap.config.characteristics.MapCoordinatesRange
import io.github.rafambn.kmap.utils.Degrees
import io.github.rafambn.kmap.utils.rotate
import io.github.rafambn.kmap.utils.toRadians

data class CanvasPosition(override val horizontal: Double, override val vertical: Double) : Position {
    override fun plus(other: Position) = CanvasPosition(horizontal + other.horizontal, vertical + other.vertical)
    override fun unaryMinus() = CanvasPosition(-horizontal, -vertical)
    override fun minus(other: Position) = CanvasPosition(horizontal - other.horizontal, vertical - other.vertical)
    override fun times(operand: Double) = CanvasPosition(horizontal * operand, vertical * operand)
    override fun div(operand: Double) = CanvasPosition(horizontal / operand, vertical / operand)

    fun toCanvasDrawReference(zoomLevel: Int, mapSource: MapSource): CanvasDrawReference {
        val canvasDrawReference = this.applyOrientation(mapSource.mapCoordinatesRange)
            .moveToTrueCoordinates(mapSource.mapCoordinatesRange)
            .scaleToZoom((mapSource.tileSize * (1 shl zoomLevel)).toFloat())
            .scaleToMap(1 / mapSource.mapCoordinatesRange.longitute.span, 1 / mapSource.mapCoordinatesRange.latitude.span)

        return CanvasDrawReference(canvasDrawReference.horizontal, canvasDrawReference.vertical)
    }

    fun toScreenOffset(
        mapPosition: CanvasPosition,
        canvasSize: Offset,
        magnifierScale: Float,
        zoomLevel: Int,
        angle: Degrees,
        density: Density,
        mapSource: MapSource
    ): ScreenOffset = -(this - mapPosition)
        .applyOrientation(mapSource.mapCoordinatesRange)
        .scaleToMap(1 / mapSource.mapCoordinatesRange.longitute.span, 1 / mapSource.mapCoordinatesRange.latitude.span)
        .rotate(angle.toRadians())
        .scaleToZoom(mapSource.tileSize * magnifierScale * (1 shl zoomLevel))
        .times(density.density.toDouble()).toOffset()
        .minus(canvasSize / 2F)


    fun scaleToZoom(zoomScale: Float): CanvasPosition {
        return CanvasPosition(horizontal * zoomScale, vertical * zoomScale)
    }

    fun moveToTrueCoordinates(mapCoordinatesRange: MapCoordinatesRange): CanvasPosition {
        return CanvasPosition(horizontal - mapCoordinatesRange.longitute.span / 2, vertical - mapCoordinatesRange.latitude.span / 2)
    }

    fun scaleToMap(horizontal: Double, vertical: Double): CanvasPosition {
        return CanvasPosition(this.horizontal * horizontal, this.vertical * vertical)
    }

    fun applyOrientation(mapCoordinatesRange: MapCoordinatesRange): CanvasPosition {
        return CanvasPosition(horizontal * mapCoordinatesRange.longitute.orientation, vertical * mapCoordinatesRange.latitude.orientation)
    }

    companion object {
        val Zero = CanvasPosition(0.0, 0.0)
    }
}
