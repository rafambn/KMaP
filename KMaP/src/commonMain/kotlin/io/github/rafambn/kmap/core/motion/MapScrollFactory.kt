package io.github.rafambn.kmap.core.motion

import io.github.rafambn.kmap.config.characteristics.MapSource
import io.github.rafambn.kmap.core.state.MapState
import io.github.rafambn.kmap.utils.Degrees
import io.github.rafambn.kmap.utils.offsets.CanvasPosition
import io.github.rafambn.kmap.utils.offsets.DifferentialScreenOffset
import io.github.rafambn.kmap.utils.offsets.ProjectedCoordinates
import io.github.rafambn.kmap.utils.offsets.ScreenOffset

object MapScrollFactory {

    fun moveBy(coordinates: ProjectedCoordinates): (MapState, MapSource) -> Unit = { mapState, mapSource ->
        with(mapSource) {
            with(mapState) {
                mapState.rawPosition = (mapState.rawPosition + toCanvasPosition(coordinates)).coerceInMap()
            }
        }
    }

    fun moveBy(position: CanvasPosition): (MapState) -> Unit = { mapState ->
        with(mapState) {
            mapState.rawPosition = (mapState.rawPosition + position).coerceInMap()
        }
    }

    fun moveBy(offset: DifferentialScreenOffset): (MapState) -> Unit = { mapState ->
        with(mapState) {
            mapState.rawPosition = (mapState.rawPosition + offset.fromDifferentialScreenOffsetToCanvasPosition()).coerceInMap()
        }
    }

    fun zoomBy(zoom: Float): (MapState) -> Unit = { mapState ->
        with(mapState) {
            mapState.zoom = (zoom + mapState.zoom).coerceZoom()
        }
    }

    fun zoomBy(zoom: Float, position: CanvasPosition): (MapState) -> Unit = { mapState ->
        with(mapState) {
            val previousOffset = position.toScreenOffset()
            mapState.zoom = (zoom + mapState.zoom).coerceZoom()
            centerPositionAtOffset(position, previousOffset) //TODO doesnt(1) work because it doenst update in time
        }
    }

    fun zoomBy(zoom: Float, offset: ScreenOffset): (MapState) -> Unit = { mapState ->
        with(mapState) {
            val position = offset.fromScreenOffsetToCanvasPosition()
            mapState.zoom = (zoom + mapState.zoom).coerceZoom()
            centerPositionAtOffset(position, offset) //TODO doesnt(1) work because it doenst update in time
        }
    }

    fun zoomBy(zoom: Float, coordinates: ProjectedCoordinates): (MapState, MapSource) -> Unit = { mapState, mapSource ->
        with(mapSource) {
            with(mapState) {
                val previousOffset = toCanvasPosition(coordinates).toScreenOffset()
                mapState.zoom = (zoom + mapState.zoom).coerceZoom()
                centerPositionAtOffset(toCanvasPosition(coordinates), previousOffset) //TODO doesnt(1) work because it doenst update in time
            }
        }
    }

    fun rotateBy(angle: Degrees): (MapState) -> Unit = { mapState ->
        mapState.angleDegrees += angle
    }

    fun rotateBy(angle: Degrees, offset: ScreenOffset): (MapState) -> Unit = { mapState ->
        with(mapState) {
            val position = offset.fromScreenOffsetToCanvasPosition()
            mapState.angleDegrees += angle
            centerPositionAtOffset(position, offset) //TODO(1) doesnt work because it doenst update in time
        }
    }

    fun rotateBy(angle: Degrees, position: CanvasPosition): (MapState) -> Unit = { mapState ->
        with(mapState) {
            val previousOffset = position.toScreenOffset()
            mapState.angleDegrees += angle
            centerPositionAtOffset(position, previousOffset) //TODO(1) doesnt work because it doenst update in time
        }
    }

    fun rotateBy(angle: Degrees, coordinates: ProjectedCoordinates): (MapState, MapSource) -> Unit = { mapState, mapSource ->
        with(mapSource) {
            with(mapState) {
                val previousOffset = toCanvasPosition(coordinates).toScreenOffset()
                mapState.angleDegrees += angle
                centerPositionAtOffset(toCanvasPosition(coordinates), previousOffset) //TODO(1) doesnt work because it doenst update in time
            }
        }
    }
}