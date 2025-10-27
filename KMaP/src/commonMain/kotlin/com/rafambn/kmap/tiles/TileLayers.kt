package com.rafambn.kmap.tiles

import com.rafambn.kmap.utils.loopInZoom

data class TileLayers(
    var trigger: Int = 1,
    var frontLayer: Layer = Layer(0, listOf()),
    var backLayer: Layer = Layer(-1, listOf())
)

data class Layer(var level: Int, var tiles: List<Tile>)

fun TileLayers.changeLayer(frontLayerLevel: Int) {
    if (frontLayerLevel == backLayer.level) {
        val temp = backLayer
        backLayer = frontLayer
        frontLayer = temp
    } else {
        backLayer = frontLayer
        frontLayer = Layer(frontLayerLevel, listOf())
    }
}

fun TileLayers.insertNewTileBitmap(tile: Tile) {
    when (tile) {
        is RasterTile ->{
            val layer = if (tile.zoom == frontLayer.level) frontLayer else if (tile.zoom == backLayer.level) backLayer else return

            layer.tiles.forEach {
                it as RasterTile
                if (tile == TileSpecs(it.zoom, it.row.loopInZoom(it.zoom), it.col.loopInZoom(it.zoom)))
                    it.imageBitmap = tile.imageBitmap
            }
        }
        is VectorTile ->{
            val layer = if (tile.zoom == frontLayer.level) frontLayer else if (tile.zoom == backLayer.level) backLayer else return

            layer.tiles.forEach {
                it as VectorTile
                if (tile == TileSpecs(it.zoom, it.row.loopInZoom(it.zoom), it.col.loopInZoom(it.zoom)))
                    it.mvtile = tile.mvtile
            }
        }
    }

}

fun TileLayers.updateFrontLayerTiles(tiles: List<Tile>) {
    frontLayer.tiles = tiles
}
