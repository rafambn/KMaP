package com.rafambn.kmap.tiles

import androidx.compose.ui.geometry.Offset
import com.rafambn.kmap.core.ViewPort
import com.rafambn.kmap.mapProperties.border.OutsideTilesType
import com.rafambn.kmap.utils.toIntFloor
import kotlin.math.pow

object TileFinder {

    fun getVisibleTilesForLevel(
        viewPort: ViewPort,
        zoomLevel: Int,
        outsideTilesType: OutsideTilesType,
        tileDimension: TileDimension
    ): List<TileSpecs> {
        val topLeftTile = getXYTile(
            viewPort.topLeft,
            zoomLevel,
            tileDimension
        )
        val topRightTile = getXYTile(
            viewPort.topRight,
            zoomLevel,
            tileDimension
        )
        val bottomRightTile = getXYTile(
            viewPort.bottomRight,
            zoomLevel,
            tileDimension
        )
        val bottomLeftTile = getXYTile(
            viewPort.bottomLeft,
            zoomLevel,
            tileDimension
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

    private fun getXYTile(position: Offset, zoomLevel: Int, tileDimension: TileDimension): Pair<Int, Int> = Pair(
        (position.x / tileDimension.width * (1 shl zoomLevel)).toIntFloor(),
        (position.y / tileDimension.height * (1 shl zoomLevel)).toIntFloor()
    )
}
