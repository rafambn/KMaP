package com.rafambn.kmap.mapSource.tiled.tiles

import com.rafambn.kmap.utils.vectorTile.OptimizedMVTile

class OptimizedVectorTile(
    zoom: Int,
    row: Int,
    col: Int,
    val optimizedTile: OptimizedMVTile?
) : Tile(zoom, row, col){

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + (optimizedTile?.hashCode() ?: 0)
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OptimizedVectorTile) return false
        if (!super.equals(other)) return false
        if (optimizedTile != other.optimizedTile) return false
        return true
    }

    override fun toString(): String {
        return "Tile(zoom=$zoom, row=$row, col=$col, optimizedTile=$optimizedTile)"
    }
}
