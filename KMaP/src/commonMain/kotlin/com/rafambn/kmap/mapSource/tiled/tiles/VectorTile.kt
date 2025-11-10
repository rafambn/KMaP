package com.rafambn.kmap.mapSource.tiled.tiles

import com.rafambn.kmap.utils.vectorTile.MVTile

class VectorTile(
    zoom: Int,
    row: Int,
    col: Int,
    val mvtile: MVTile?
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
