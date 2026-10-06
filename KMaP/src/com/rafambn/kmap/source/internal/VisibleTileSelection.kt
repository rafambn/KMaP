package com.rafambn.kmap.source.internal

import com.rafambn.kmap.geometry.plane.TilePoint
import com.rafambn.kmap.mapProperties.TileDimension
import com.rafambn.kmap.mapProperties.border.TileRepeatMode
import com.rafambn.kmap.source.TileSpecs
import kotlin.math.ceil
import kotlin.math.floor

internal fun getVisibleTilesForLevel(
    topLeft: TilePoint,
    bottomRight: TilePoint,
    zoomLevel: Int,
    tileRepeatMode: TileRepeatMode,
    tileDimension: TileDimension,
): List<TileSpecs> {
    require(zoomLevel in 0..30) { "Supported zoom levels are 0..30" }
    if (topLeft.x >= bottomRight.x || topLeft.y >= bottomRight.y) return emptyList()

    val tileCount = 1L shl zoomLevel
    var minX = floor(topLeft.x / tileDimension.width.value * tileCount).toLong()
    // Right and bottom edges are exclusive: a tile that only touches them is not visible.
    var maxX = (ceil(bottomRight.x / tileDimension.width.value * tileCount) - 1).toLong()
    var minY = floor(topLeft.y / tileDimension.height.value * tileCount).toLong()
    var maxY = (ceil(bottomRight.y / tileDimension.height.value * tileCount) - 1).toLong()
    if (tileRepeatMode == TileRepeatMode.NONE) {
        minX = maxOf(minX, 0L)
        maxX = minOf(maxX, tileCount - 1)
        minY = maxOf(minY, 0L)
        maxY = minOf(maxY, tileCount - 1)
    }
    if (minX > maxX || minY > maxY) return emptyList()
    require(minX >= Int.MIN_VALUE && maxX <= Int.MAX_VALUE &&
        minY >= Int.MIN_VALUE && maxY <= Int.MAX_VALUE
    ) { "Visible tile indices exceed the supported Int range" }
    val visibleTiles = mutableListOf<TileSpecs>()
    for (x in minX..maxX)
        for (y in minY..maxY)
            visibleTiles.add(TileSpecs(zoomLevel, y.toInt(), x.toInt()))
    return visibleTiles
}
