package com.rafambn.kmap.tiles

import androidx.compose.ui.graphics.ImageBitmap
import com.rafambn.kmap.utils.vectorTile.MVTile

open class TileSpecs(
    val zoom: Int,
    val row: Int,
    val col: Int
){
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TileSpecs) return false
        if (zoom != other.zoom) return false
        if (row != other.row) return false
        if (col != other.col) return false
        return true
    }

    override fun hashCode(): Int {
        var result = zoom
        result = 31 * result + row
        result = 31 * result + col
        return result
    }

    override fun toString(): String {
        return "TileSpecs(zoom=$zoom, row=$row, col=$col)"
    }
}

open class Tile(zoom: Int, row: Int, col: Int): TileSpecs(zoom, row, col)

class RasterTile(
    zoom: Int,
    row: Int,
    col: Int,
    var imageBitmap: ImageBitmap?
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

class VectorTile(
    zoom: Int,
    row: Int,
    col: Int,
    var mvtile: MVTile?
) : Tile(zoom, row, col){

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + (mvtile?.hashCode() ?: 0)
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (!super.equals(other)) return false
        if (other is VectorTile && mvtile != other.mvtile) return false
        return true
    }

    override fun toString(): String {
        return "Tile(zoom=$zoom, row=$row, col=$col, mvtile=$mvtile)"
    }
}
