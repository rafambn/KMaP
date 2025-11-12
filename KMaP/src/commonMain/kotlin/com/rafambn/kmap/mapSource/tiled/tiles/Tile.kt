package com.rafambn.kmap.mapSource.tiled.tiles

abstract class Tile(zoom: Int, row: Int, col: Int): TileSpecs(zoom, row, col){
    fun isParentOf(childCandidate: TileSpecs): Boolean {
        if (this.zoom >= childCandidate.zoom) return false

        val zoomDiff = childCandidate.zoom - this.zoom
        val scaleFactor = 1 shl zoomDiff
        val parentRow = childCandidate.row / scaleFactor
        val parentCol = childCandidate.col / scaleFactor

        return this.row == parentRow && this.col == parentCol
    }

    fun isChildOf(parentCandidate: TileSpecs): Boolean {
        if (this.zoom <= parentCandidate.zoom) return false

        val zoomDiff = this.zoom - parentCandidate.zoom
        val scaleFactor = 1 shl zoomDiff
        val parentRow = this.row / scaleFactor
        val parentCol = this.col / scaleFactor

        return parentCandidate.row == parentRow && parentCandidate.col == parentCol
    }

    abstract fun withSpecs(newSpecs: TileSpecs): Tile
}
