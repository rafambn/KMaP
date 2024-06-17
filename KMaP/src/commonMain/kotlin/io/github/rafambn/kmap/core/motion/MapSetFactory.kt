package io.github.rafambn.kmap.core.motion

import io.github.rafambn.kmap.config.characteristics.MapSource
import io.github.rafambn.kmap.core.state.MapState
import io.github.rafambn.kmap.utils.Degrees
import io.github.rafambn.kmap.utils.offsets.CanvasPosition
import io.github.rafambn.kmap.utils.offsets.ProjectedCoordinates

object MapSetFactory {
    fun setCenter(position: CanvasPosition): (MapState) -> Unit = { mapState ->
        with(mapState) {
            mapState.rawPosition = position.coerceInMap()
        }
    }

    fun setCenter(coordinates: ProjectedCoordinates): (MapState, MapSource) -> Unit = { mapState, mapSource ->
        with(mapSource) {
            with(mapState) {
                mapState.rawPosition = toCanvasPosition(coordinates).coerceInMap()
            }
        }
    }

    fun setZoom(zoom: Float): (MapState) -> Unit = { mapState ->
        with(mapState) {
            mapState.zoom = zoom.coerceZoom()
        }
    }

    fun setRotation(angle: Degrees): (MapState) -> Unit = { mapState ->
        mapState.angleDegrees = angle
    }
}