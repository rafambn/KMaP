package io.github.rafambn.kmap.model

import androidx.compose.ui.graphics.ImageBitmap

interface TileCore {
    val zoom: Int
    val row: Int
    val col: Int

    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
    override fun toString(): String
}

class TileSpecs(
    override val zoom: Int,
    override val row: Int,
    override val col: Int,
    var numberOfTries: Int = 0,
) : TileCore {
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
        result = 31 * result + numberOfTries
        return result
    }

    override fun toString(): String {
        return "TileSpecs(zoom=$zoom, row=$row, col=$col, numberOfTries=$numberOfTries)"
    }
}

class Tile(
    override val zoom: Int,
    override val row: Int,
    override val col: Int,
    val imageBitmap: ImageBitmap
) : TileCore {
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
        result = 31 * result + imageBitmap.hashCode()
        return result
    }

    override fun toString(): String {
        return "Tile(zoom=$zoom, row=$row, col=$col, imageBitmap=$imageBitmap)"
    }
}