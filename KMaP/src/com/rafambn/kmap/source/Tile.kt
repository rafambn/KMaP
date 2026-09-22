package com.rafambn.kmap.source

abstract class Tile(zoom: Int, row: Int, col: Int): TileSpecs(zoom, row, col){
    fun isParentOf(childCandidate: TileSpecs): Boolean {
        if (this.zoom >= childCandidate.zoom) return false

        val zoomDiff = childCandidate.zoom - this.zoom
        val scaleFactor = 1 shl zoomDiff
        val parentRow = childCandidate.row.floorDiv(scaleFactor)
        val parentCol = childCandidate.col.floorDiv(scaleFactor)

        return this.row == parentRow && this.col == parentCol
    }

    fun isChildOf(parentCandidate: TileSpecs): Boolean {
        if (this.zoom <= parentCandidate.zoom) return false

        val zoomDiff = this.zoom - parentCandidate.zoom
        val scaleFactor = 1 shl zoomDiff
        val parentRow = this.row.floorDiv(scaleFactor)
        val parentCol = this.col.floorDiv(scaleFactor)

        return parentCandidate.row == parentRow && parentCandidate.col == parentCol
    }

    abstract fun withSpecs(newSpecs: TileSpecs): Tile
}
