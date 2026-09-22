package com.rafambn.kmap.source

open class TileSpecs(
    val zoom: Int,
    val row: Int,
    val col: Int
){
    init {
        require(zoom in 0..30) { "Supported zoom levels are 0..30" }
    }

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
