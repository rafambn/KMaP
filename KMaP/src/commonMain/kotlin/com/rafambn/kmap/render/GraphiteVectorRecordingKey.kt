package com.rafambn.kmap.render

import com.rafambn.kmap.mapSource.tiled.tiles.OptimizedVectorTile
import com.rafambn.kmap.utils.style.OptimizedStyleLayer

internal class GraphiteVectorRecordingKey(
    val canvasId: Int,
    val styleLayerIndex: Int,
    val styleLayer: OptimizedStyleLayer,
    val tile: OptimizedVectorTile,
    val currentZoom: Int,
    val styleZoom: Double,
    val tileWidth: Float,
    val tileHeight: Float,
) {
    override fun equals(other: Any?): Boolean =
        other is GraphiteVectorRecordingKey &&
            canvasId == other.canvasId &&
            styleLayerIndex == other.styleLayerIndex &&
            styleLayer === other.styleLayer &&
            tile === other.tile &&
            currentZoom == other.currentZoom &&
            styleZoom == other.styleZoom &&
            tileWidth == other.tileWidth &&
            tileHeight == other.tileHeight

    override fun hashCode(): Int {
        var result = canvasId
        result = 31 * result + styleLayerIndex
        result = 31 * result + tile.zoom
        result = 31 * result + tile.row
        result = 31 * result + tile.col
        result = 31 * result + currentZoom
        result = 31 * result + styleZoom.hashCode()
        result = 31 * result + tileWidth.hashCode()
        result = 31 * result + tileHeight.hashCode()
        return result
    }
}
