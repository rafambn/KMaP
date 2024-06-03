package io.github.rafambn.kmap.config.sources.openStreetMaps

import io.github.rafambn.kmap.config.MapSource
import io.github.rafambn.kmap.utils.offsets.CanvasPosition
import io.github.rafambn.kmap.utils.offsets.ProjectedCoordinates
import kotlin.math.PI
import kotlin.math.ln
import kotlin.math.tan

object OSMMapSource : MapSource {
    override val zoomLevels = OSMZoomLevelsRange
    override val mapCoordinatesRange = OSMCoordinatesRange
    override val tileSize = 256

    override fun toCanvasPosition(projectedCoordinates: ProjectedCoordinates): CanvasPosition = CanvasPosition(
        projectedCoordinates.horizontal,
        ln(tan(PI / 4 + (PI * projectedCoordinates.vertical) / 360)) / (PI / 85.051129)
    )
}