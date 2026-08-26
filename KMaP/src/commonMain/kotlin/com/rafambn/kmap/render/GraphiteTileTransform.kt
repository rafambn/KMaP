package com.rafambn.kmap.render

import com.rafambn.kmap.mapSource.tiled.tiles.OptimizedVectorTile
import com.rafambn.kmap.utils.toIntFloor
import kotlin.math.pow

internal data class GraphiteTileTransform(
    val scaleAdjustment: Float,
    val left: Float,
    val top: Float,
    val scaleX: Float,
    val scaleY: Float,
    val extent: Float,
)

internal fun resolveGraphiteTileTransform(
    camera: GraphiteMapCamera,
    currentZoom: Int,
    tile: OptimizedVectorTile,
): GraphiteTileTransform? {
    val optimizedTile = tile.optimizedTile ?: return null
    val scaleAdjustment = 2f.pow(currentZoom - tile.zoom)
    val width = scaleAdjustment * camera.tileSizePx.width
    val height = scaleAdjustment * camera.tileSizePx.height
    return GraphiteTileTransform(
        scaleAdjustment = scaleAdjustment,
        left = tile.col * width + camera.positionOffset.x.toIntFloor(),
        top = tile.row * height + camera.positionOffset.y.toIntFloor(),
        scaleX = width / optimizedTile.extent,
        scaleY = height / optimizedTile.extent,
        extent = optimizedTile.extent.toFloat(),
    )
}
