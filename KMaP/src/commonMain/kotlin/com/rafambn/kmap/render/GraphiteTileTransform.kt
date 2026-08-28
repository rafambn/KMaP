package com.rafambn.kmap.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
): GraphiteTileTransform? = resolveGraphiteTileTransform(
    tileSizePx = camera.tileSizePx,
    currentZoom = currentZoom,
    tile = tile,
    positionOffset = camera.positionOffset,
)

internal fun resolveGraphiteTileTransform(
    tileSizePx: Size,
    currentZoom: Int,
    tile: OptimizedVectorTile,
    positionOffset: Offset,
): GraphiteTileTransform? {
    val optimizedTile = tile.optimizedTile ?: return null
    val scaleAdjustment = 2f.pow(currentZoom - tile.zoom)
    val width = scaleAdjustment * tileSizePx.width
    val height = scaleAdjustment * tileSizePx.height
    return GraphiteTileTransform(
        scaleAdjustment = scaleAdjustment,
        left = tile.col * width + positionOffset.x.toIntFloor(),
        top = tile.row * height + positionOffset.y.toIntFloor(),
        scaleX = width / optimizedTile.extent,
        scaleY = height / optimizedTile.extent,
        extent = optimizedTile.extent.toFloat(),
    )
}
