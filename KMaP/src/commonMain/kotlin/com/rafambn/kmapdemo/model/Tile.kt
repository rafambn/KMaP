package com.rafambn.kmapdemo.model

import androidx.compose.ui.graphics.ImageBitmap

open class TileCore(
    val zoom: Int,
    val row: Int,
    val col: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TileCore) return false
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
}

class TileSpecs(
    zoom: Int,
    row: Int,
    col: Int
) : TileCore(zoom, row, col) {
    override fun toString(): String {
        return "TileSpecs(zoom=$zoom, row=$row, col=$col)"
    }

    fun toTile(): Tile = Tile(zoom, row, col, null)
}

class Tile(
    zoom: Int,
    row: Int,
    col: Int,
    var imageBitmap: ImageBitmap?
) : TileCore(zoom, row, col) {
    override fun hashCode(): Int {
        var result = zoom
        result = 31 * result + row
        result = 31 * result + col
        result = 31 * result + imageBitmap.hashCode()
        return result
    }

    override fun toString(): String {
        return "Tile(zoom=$zoom, row=$row, col=$col, imageBitmap=$imageBitmap)"
    }

    fun toTileSpecs(): TileSpecs = TileSpecs(zoom, row, col)
}

data class ResultTile(val tile: Tile?, val result: TileResult)

enum class TileResult {
    SUCCESS,
    FAILURE
}