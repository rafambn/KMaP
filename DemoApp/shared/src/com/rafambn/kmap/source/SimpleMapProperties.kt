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

data class SimpleMapProperties(
    override val boundMap: BoundMapBorder = BoundMapBorder(MapBorderType.BOUND, MapBorderType.BOUND),
    override val outsideTiles: OutsideTilesType = OutsideTilesType.NONE,
    override val zoomLevels: ZoomLevelRange = SimpleZoomLevelRange(),
    override val coordinatesRange: CoordinatesRange = SimpleCoordinatesRange(),
    override val tileSize: TileDimension = TileDimension(512.dp, 512.dp)
) : MapProperties {
    override fun toProjectedCoordinates(coordinates: Coordinates): ProjectedCoordinates = ProjectedCoordinates(
        coordinates.x,
        coordinates.y
    )

    override fun toCoordinates(projectedCoordinates: ProjectedCoordinates): Coordinates = Coordinates(
        projectedCoordinates.x,
        projectedCoordinates.y
    )
}
