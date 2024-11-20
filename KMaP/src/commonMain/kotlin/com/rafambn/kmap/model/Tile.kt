package com.rafambn.kmap.model

import androidx.compose.ui.graphics.ImageBitmap

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

class Tile(
    zoom: Int,
    row: Int,
    col: Int,
    var imageBitmap: ImageBitmap?
) : TileSpecs(zoom, row, col) {

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + (imageBitmap?.hashCode() ?: 0)
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (!super.equals(other)) return false
        if (other is Tile && imageBitmap != other.imageBitmap) return false
        return true
    }

    override fun toString(): String {
        return "Tile(zoom=$zoom, row=$row, col=$col, imageBitmap=$imageBitmap)"
    }
}

interface TileRenderResult {
    data class Success(val tile: Tile) : TileRenderResult
    data class Failure(val specs: TileSpecs) : TileRenderResult
}