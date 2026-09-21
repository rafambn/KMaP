package com.rafambn.kmap.source

import androidx.compose.ui.unit.dp
import com.rafambn.kmap.mapProperties.coordinates.CoordinatesRange
import com.rafambn.kmap.mapProperties.MapProperties
import com.rafambn.kmap.mapProperties.TileDimension
import com.rafambn.kmap.mapProperties.ZoomLevelRange
import com.rafambn.kmap.mapProperties.border.BoundMapBorder
import com.rafambn.kmap.mapProperties.border.MapBorderType
import com.rafambn.kmap.mapProperties.border.OutsideTilesType
import com.rafambn.kmap.geometry.plane.Coordinates
import com.rafambn.kmap.geometry.plane.ProjectedCoordinates
import kotlin.math.*

data class OSMMapProperties(
    override val boundMap: BoundMapBorder = BoundMapBorder(MapBorderType.BOUND, MapBorderType.BOUND),
    override val outsideTiles: OutsideTilesType = OutsideTilesType.NONE,
    override val zoomLevels: ZoomLevelRange = OSMZoomLevelRange(),
    override val coordinatesRange: CoordinatesRange = OSMCoordinatesRange(),
    override val tileSize: TileDimension = TileDimension(512.dp, 512.dp)
) : MapProperties {
    override fun toProjectedCoordinates(coordinates: Coordinates): ProjectedCoordinates = ProjectedCoordinates(
        coordinates.x,
        ln(tan(PI / 4 + (PI * coordinates.y) / 360)) / (PI / 85.051129)
    )

    override fun toCoordinates(projectedCoordinates: ProjectedCoordinates): Coordinates = Coordinates(
        projectedCoordinates.x,
        (atan(E.pow(projectedCoordinates.y * (PI / 85.051129))) - PI / 4) * 360 / PI
    )
}
