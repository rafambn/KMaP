package com.rafambn.kmap.tiles

import com.rafambn.kmap.core.ViewPort
import com.rafambn.kmap.core.bottomLeft
import com.rafambn.kmap.core.bottomRight
import com.rafambn.kmap.core.topLeft
import com.rafambn.kmap.core.topRight
import com.rafambn.kmap.mapProperties.border.OutsideTilesType
import com.rafambn.kmap.mapProperties.coordinates.MapCoordinatesRange
import com.rafambn.kmap.utils.CanvasPosition
import com.rafambn.kmap.utils.toIntFloor
import kotlin.math.pow

class TileFinder {

    fun getVisibleTilesForLevel(
        viewPort: ViewPort,
        zoomLevel: Int,
        outsideTilesType: OutsideTilesType,
        coordinatesRange: MapCoordinatesRange
    ): List<TileSpecs> {
        val topLeftTile = getXYTile(
            viewPort.topLeft().applyInverseOrientation(coordinatesRange),
            zoomLevel,
            coordinatesRange
        )
        val topRightTile = getXYTile(
            viewPort.topRight().applyInverseOrientation(coordinatesRange),
            zoomLevel,
            coordinatesRange
        )
        val bottomRightTile = getXYTile(
            viewPort.bottomRight().applyInverseOrientation(coordinatesRange),
            zoomLevel,
            coordinatesRange
        )
        val bottomLeftTile = getXYTile(
            viewPort.bottomLeft().applyInverseOrientation(coordinatesRange),
            zoomLevel,
            coordinatesRange
        )
        val horizontalTileIntRange =
            IntRange(
                minOf(topLeftTile.first, bottomRightTile.first, topRightTile.first, bottomLeftTile.first),
                maxOf(topLeftTile.first, bottomRightTile.first, topRightTile.first, bottomLeftTile.first)
            )
        val verticalTileIntRange =
            IntRange(
                minOf(topLeftTile.second, bottomRightTile.second, topRightTile.second, bottomLeftTile.second),
                maxOf(topLeftTile.second, bottomRightTile.second, topRightTile.second, bottomLeftTile.second)
            )

        val visibleTileSpecs = mutableListOf<TileSpecs>()
        if (outsideTilesType == OutsideTilesType.NONE) {
            for (x in horizontalTileIntRange)
                for (y in verticalTileIntRange) {
                    var xTile: Int
                    if (x < 0 || x > 2F.pow(zoomLevel) - 1)
                        continue
                    else
                        xTile = x
                    var yTile: Int
                    if (y < 0 || y > 2F.pow(zoomLevel) - 1)
                        continue
                    else
                        yTile = y
                    visibleTileSpecs.add(TileSpecs(zoomLevel, xTile, yTile))
                }
        } else {
            for (x in horizontalTileIntRange)
                for (y in verticalTileIntRange)
                    visibleTileSpecs.add(TileSpecs(zoomLevel, x, y))
        }
        return visibleTileSpecs
    }

    private fun getXYTile(position: CanvasPosition, zoomLevel: Int, mapSize: MapCoordinatesRange): Pair<Int, Int> = Pair(
        ((position.horizontal - mapSize.longitude.getMin()) / mapSize.longitude.span * (1 shl zoomLevel)).toIntFloor(),
        ((position.vertical - mapSize.latitude.getMin()) / mapSize.latitude.span * (1 shl zoomLevel)).toIntFloor()
    )

    private fun CanvasPosition.applyInverseOrientation(mapCoordinatesRange: MapCoordinatesRange): CanvasPosition = CanvasPosition(
        horizontal * mapCoordinatesRange.longitude.getOrientation() * -1,
        vertical * mapCoordinatesRange.latitude.getOrientation() * -1
    )
}