package com.rafambn.kmap.mapSource.tiled.tiles

import androidx.compose.ui.graphics.ImageBitmap

class RasterTile(
    zoom: Int,
    row: Int,
    col: Int,
    val imageBitmap: ImageBitmap?
) : Tile(zoom, row, col) {

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + (imageBitmap?.hashCode() ?: 0)
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (!super.equals(other)) return false
        if (other is RasterTile && imageBitmap != other.imageBitmap) return false
        return true
    }

    override fun toString(): String {
        return "Tile(zoom=$zoom, row=$row, col=$col, imageBitmap=$imageBitmap)"
    }
}
