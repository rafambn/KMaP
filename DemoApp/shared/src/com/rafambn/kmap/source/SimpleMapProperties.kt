package com.rafambn.kmap.source

import androidx.compose.ui.unit.dp
import com.rafambn.kmap.map.CoordinatesRange
import com.rafambn.kmap.map.MapProperties
import com.rafambn.kmap.map.TileDimension
import com.rafambn.kmap.map.ZoomLevelRange
import com.rafambn.kmap.map.border.BoundMapBorder
import com.rafambn.kmap.map.border.MapBorderType
import com.rafambn.kmap.map.border.OutsideTilesType
import com.rafambn.kmap.geometry.Coordinates
import com.rafambn.kmap.geometry.ProjectedCoordinates

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
